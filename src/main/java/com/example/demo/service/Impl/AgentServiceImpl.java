package com.example.demo.service.Impl;

import com.example.demo.domain.Conversation;
import com.example.demo.domain.Message;
import com.example.demo.repository.ConversationRepository;
import com.example.demo.repository.MessageRepository;
import com.example.demo.service.AgentService;
import com.example.demo.service.AiService;
import com.example.demo.service.Tool;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AgentServiceImpl implements AgentService {

    private final AiService aiService;
    private final List<Tool> tools;  // 注入所有工具
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;

    public AgentServiceImpl(AiService aiService,
                            List<Tool> tools,
                            MessageRepository messageRepository,
                            ConversationRepository conversationRepository) {
        this.conversationRepository =conversationRepository;
        this.aiService = aiService;
        this.tools = tools;
        this.messageRepository = messageRepository;
    }


    @Override
    public String processWithTools(String userMessage) {
        //1、先让AI判断是否需要调用工具
        String toolPrompt = buildToolPrompt(userMessage);
        String aiDecision = aiService.generateResponse(toolPrompt, null);

        //2、解析AI决策
        Tool tool = findToolToUse(aiDecision, userMessage);

        if(tool != null){
             //3、调用工具
            String toolResult = tool.execute(extractToolInput(userMessage));
            // 4. 把工具结果传给 AI，生成最终回答
            String finalPrompt = String.format(
                    "用户问题：%s\n工具执行结果：%s\n请基于工具结果回答用户问题。",
                    userMessage, toolResult
            );
            return aiService.generateResponse(finalPrompt, null);
        } else {
            // 不需要工具，直接回答
            return aiService.generateResponse(userMessage, null);
        }
    }

    @Override
    public Flux<String> streamProcessWithTools(Long conversationId, String userMessage) {
        // 1. 获取历史消息
        List<Message> historyMessages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);

        String conversationHistory = historyMessages.stream().limit(10)
                .map(msg -> msg.getRole() + ": " + msg.getContent())
                .collect(Collectors.joining("\n"));

        // 2. 判断是否需要调用工具
        String toolPrompt = buildToolPrompt(userMessage);
        String aiDecision = aiService.generateResponse(toolPrompt, null);
        Tool tool = findToolToUse(aiDecision,userMessage);

        if(tool != null){
            // 3. 调用工具
            String toolResult = tool.execute(extractToolInput(userMessage));

            // 4. 构建包含工具结果的 Prompt
            String finalPrompt = String.format(
                    "用户问题：%s\n工具执行结果：%s\n请基于工具结果回答用户问题。",
                    userMessage, toolResult
            );
            // 5. 流式生成回答
            StringBuilder fullResponse = new StringBuilder();
            return aiService.streamResponse(finalPrompt, conversationHistory)
                    .doOnNext(fullResponse::append)
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
        } else {
            // 不需要工具，直接流式回答
            StringBuilder fullResponse = new StringBuilder();
            return aiService.streamResponse(userMessage, conversationHistory)
                    .doOnNext(fullResponse::append)
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
    }

    //检查是否有工具调用
    private Tool findToolToUse(String aiDecision, String userMessage) {
        // 简单匹配：检查用户消息中是否包含关键词
        if (userMessage.contains("天气")) {
            return tools.stream()
                    .filter(t -> t.getName().equals("get_weather"))
                    .findFirst()//拿到第一个匹配的工具就停止
                    .orElse(null);
        }
        return null;
    }

    //构建工具prompt
    private String buildToolPrompt(String userMessage) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("用户问题：").append(userMessage).append("\n\n");
        prompt.append("可用工具：\n");
        for (Tool tool : tools) {
            prompt.append("- ").append(tool.getName())
                    .append(": ").append(tool.getDescription()).append("\n");
        }
        prompt.append("\n请判断是否需要调用工具。如果需要，回复工具名称；如果不需要，回复 'none'。");
        return prompt.toString();
    }

    private String extractToolInput(String userMessage) {
        // 简单提取：提取城市名称
        Pattern pattern = Pattern.compile("(.+?)的天气");
        Matcher matcher = pattern.matcher(userMessage);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return userMessage;
    }
}
