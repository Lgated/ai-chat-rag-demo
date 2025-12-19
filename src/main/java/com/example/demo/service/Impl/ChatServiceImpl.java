package com.example.demo.service.Impl;

import com.example.demo.domain.Conversation;
import com.example.demo.domain.Message;
import com.example.demo.repository.ConversationRepository;
import com.example.demo.repository.MessageRepository;
import com.example.demo.service.AiService;
import com.example.demo.service.ChatService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    private final AiService aiService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ChatServiceImpl(ConversationRepository conversationRepository,
                           MessageRepository messageRepository,
                           AiService aiService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.aiService = aiService;
    }

    @Override
    public List<Conversation> listConversations() {
        return conversationRepository.findAll();
    }

    @Override
    @Transactional
    public Conversation creatConversation(String title) {
        Conversation conversation = new Conversation();
        conversation.setTitle(title != null && !title.isEmpty() ? title : "新会话");
        return conversationRepository.save(conversation);
    }

    @Override
    public Conversation getConversation(Long conversationId) {
        return conversationRepository.findById(conversationId).orElseThrow(() -> new IllegalArgumentException("会话不存在" + conversationId));
    }

    @Override
    public List<Message> listMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    //非流式问答
    @Override
    public Message addMessage(Long conversationId, String role, String content) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(()->new IllegalArgumentException("会话不存在" + conversationId));

        //保存用户信息
        Message message = new Message();
        message.setConversation(conversation);
        message.setContent(content);
        message.setRole(role);
        messageRepository.save(message);

        //如果是用户信息，调用ai回答
        if(role.equals("user")){
            //获取历史消息（用于上下文）
            List<Message> historyMessages = messageRepository
                    .findByConversationIdOrderByCreatedAtAsc(conversationId);

            // 构建历史消息文本 -- 只取最近10条，避免token消耗过多
            String historyText = historyMessages.stream().limit(10)
                    .map(msg -> msg.getRole() + ": " + msg.getContent())
                    .collect(Collectors.joining("\n"));

            String aiResponse = aiService.generateResponse(content, historyText);

            //保存AI的回答
            Message assistantMessage = new Message();
            assistantMessage.setConversation(conversation);
            assistantMessage.setRole("assistant");
            assistantMessage.setContent(aiResponse);
            messageRepository.save(assistantMessage);

            return assistantMessage;
        }
        return message;
    }

    @Override
    public Flux<String> streamAiResponse(Long conversationId, String userMessage) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(()->new IllegalArgumentException("会话不存在" + conversationId));

        Message message = new Message();
        message.setConversation(conversation);
        message.setContent(userMessage);
        message.setRole("user");
        messageRepository.save(message);

        //获取历史消息
        List<Message> historyMessages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        String historyText = historyMessages.stream().limit(10)
                .map(msg -> msg.getRole() + ": " + msg.getContent())
                .collect(Collectors.joining("\n"));

        // 调用流式 AI
        StringBuilder fullResponse = new StringBuilder();

        //优点：非阻塞、异步、适配 WebFlux 前端流式返回
        return aiService.streamResponse(userMessage,historyText)
                //用于在流的每个元素处理时执行额外逻辑 -  每收到 AI 返回的一段内容chunk，就拼接到 fullResponse 中
                .doOnNext(chunk -> fullResponse.append(chunk))
                .doOnComplete(() -> {
            // 流结束后保存完整的 AI 回答
            Message assistantMsg = new Message();
            assistantMsg.setConversation(conversation);
            assistantMsg.setRole("assistant");
            assistantMsg.setContent(fullResponse.toString());
            messageRepository.save(assistantMsg);
        });
    }

    // 获取最新的一条用户数据
    @Override
    public Message getLatestUserMessage(Long conversationId, String content) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(()->new IllegalArgumentException("会话不存在" + conversationId));

        List<Message> userMessages  = messageRepository.findByConversationIdAndRoleOrderByCreatedAtAsc(conversationId, "user");
        // 过滤出内容匹配、按创建时间倒序排序，取第一条
        Optional<Message> latestMessage = userMessages.stream()
                .filter(msg -> StringUtils.hasText(msg.getContent())
                        && msg.getContent().equals(content))
                .max(Comparator.comparing(Message::getCreatedAt)); // 按创建时间最新排序

        return latestMessage.orElse(null);

    }


    @Override
    public Message getLatestAssistantMessage(Long conversationId, String content) {
        // 查询该会话下所有AI助手消息
        List<Message> assistantMessages = messageRepository.findByConversationIdAndRoleOrderByCreatedAtAsc(
                conversationId, "assistant");

        // 过滤出内容匹配、按创建时间倒序排序，取第一条
        Optional<Message> latestMessage = assistantMessages.stream()
                .filter(msg -> StringUtils.hasText(msg.getContent())
                        && msg.getContent().equals(content))
                .max(Comparator.comparing(Message::getCreatedAt));

        return latestMessage.orElse(null);
    }

}
