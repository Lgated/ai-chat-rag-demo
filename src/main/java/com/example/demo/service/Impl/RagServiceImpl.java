package com.example.demo.service.Impl;

import com.example.demo.domain.Conversation;
import com.example.demo.domain.DocumentChunk;
import com.example.demo.domain.Message;
import com.example.demo.repository.ConversationRepository;
import com.example.demo.repository.DocumentRepository;
import com.example.demo.repository.MessageRepository;
import com.example.demo.service.AiService;
import com.example.demo.service.ChatService;
import com.example.demo.service.EmbeddingService;
import com.example.demo.service.RagService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class RagServiceImpl implements RagService {

    private final EmbeddingService embeddingService;
    private final AiService aiService;
    private final DocumentRepository documentRepository;
    private final ChatService chatService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public RagServiceImpl(
            EmbeddingService embeddingService
            ,AiService aiService
            ,DocumentRepository documentRepository
            ,ChatService chatService
            ,ConversationRepository conversationRepository
            ,MessageRepository messageRepository){
        this.embeddingService = embeddingService;
        this.aiService = aiService;
        this.documentRepository = documentRepository;
        this.chatService = chatService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    //文档入库
    @Override
    public void ingestDocument(String documentContent, Long docId) {
        //1、文档切分 - 按500字切
        List<String> chunks = splitText(documentContent,500);
        //2、批量生成embedding
        List<float[]> embeddings = embeddingService.embedTexts(chunks);
        //3、存入数据库
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocId(docId);
            chunk.setContent(chunks.get(i));
            chunk.setEmbedding(embeddingService.toVectorString(embeddings.get(i)));
            documentRepository.save(chunk);
        }
    }


    // rag检索
    @Override
    public List<String> retrieveRelevantChunks(String query, int topK) {
        //1、 对查询问题生成embedding
        float[] queryEmbedding = embeddingService.embedText(query);
        String queryVector = embeddingService.toVectorString(queryEmbedding);
        //2、 向量相似度检查
        List<DocumentChunk> similarChunks = documentRepository.findSimilarChunks(queryVector, topK);
        //3、返回内容
        return similarChunks.stream()
                .map(DocumentChunk::getContent)
                .collect(Collectors.toList());
    }

    //使用 RAG 生成回答
    @Override
    public String generateRagResponse(String query, List<String> relevantChunks) {
        // 构建prompt，包含检索到的文档片段
        StringBuilder context = new StringBuilder();
        context.append("以下是与问题相关的文档片段：\n\n");
        for (int i = 0; i < relevantChunks.size(); i++) {
            context.append("片段 ").append(i + 1).append(":\n");
            context.append(relevantChunks.get(i)).append("\n\n");
        }
        context.append("请基于以上文档内容回答问题：").append(query);
        // 调用 AI 生成回答
        return aiService.generateResponse(query, context.toString());
    }


    @Override
    public Flux<String> streamRagResponse(Long conversationId, String query, int topK) {
        // 1. RAG 检索：找到相关文档片段
        List<String> relevantChunks = retrieveRelevantChunks(query, topK);

        // 2. 获取历史消息（用于上下文）
        List<Message> historyMessages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);
        String historyText = historyMessages.stream().limit(10)
                .map(msg -> msg.getRole() + ": " + msg.getContent())
                .collect(Collectors.joining("\n"));

        // 3. 构建包含文档片段的上下文
        StringBuilder context = new StringBuilder();
        if (!historyText.isEmpty()) {
            context.append("以下是对话历史：\n").append(historyText).append("\n\n");
        }
        context.append("以下是与问题相关的文档片段：\n\n");
        for (int i = 0; i < relevantChunks.size(); i++) {
            context.append("片段 ").append(i + 1).append(":\n");
            context.append(relevantChunks.get(i)).append("\n\n");
        }
        context.append("请基于以上文档内容回答问题：").append(query);

        // 4. 流式生成回答
        StringBuilder fullResponse = new StringBuilder();
        return aiService.streamResponse(query,context.toString())
                .doOnNext(chunk -> fullResponse.append(chunk))
                .doOnComplete(() -> {
                    // 流结束后保存完整的 AI 回答
                    Conversation conversation = conversationRepository.findById(conversationId)
                            .orElseThrow(() -> new IllegalArgumentException("会话不存在"));
                    Message assistantMsg = new Message();
                    assistantMsg.setConversation(conversation);
                    assistantMsg.setRole("assistant");
                    assistantMsg.setContent(fullResponse.toString());
                    messageRepository.save(assistantMsg);
                });
    }

    @Override
    public Message ragChat(Long conversationId, String query) {
        // 1. 保存用户消息
        Message userMessage = chatService.addMessage(conversationId, "user", query);
        // 2. RAG 检索
        List<String> relevantChunks = retrieveRelevantChunks(query, 5);
        // 3. 生成回答
        String aiResponse = generateRagResponse(query, relevantChunks);
        // 4. 保存AI回答
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在"));
        Message assistantMsg = new Message();
        assistantMsg.setConversation(conversation);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(aiResponse);

        return messageRepository.save(assistantMsg);
    }

    // 按字数切分文档
    private List<String> splitText(String documentContent, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        int length = documentContent.length();
        for (int i = 0; i < length; i+=chunkSize) {
            int end = Math.min(i + chunkSize, length);
            chunks.add(documentContent.substring(i,end));
        }
        return chunks;
    }
}
