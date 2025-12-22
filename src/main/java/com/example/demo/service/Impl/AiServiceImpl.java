package com.example.demo.service.Impl;


import com.example.demo.common.exception.AiServiceException;
import com.example.demo.service.AiService;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;


@Service
public class AiServiceImpl implements AiService {


    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);
    //结合openai
    private final ChatClient chatClient;

    public AiServiceImpl(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /*
    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    @Value("${spring.ai.dashscope.model:qwen1.5-110b-chat}")
    private String model;

    @PostConstruct
    public void init() {
        // 避免打印完整 key，只看前后 4 位
        if (apiKey == null) {
            log.error("DashScope apiKey is NULL!");
            return;
        }
        String shown = apiKey.length() <= 8
                ? apiKey
                : apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
        log.info("DashScope apiKey loaded (masked): {}", shown);
    }*/

    @Override
    public String generateResponse(String userMessage, String conversationHistory) {
        // 参数校验
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new AiServiceException("AI_INVALID_INPUT", "用户消息不能为空");
        }

        try {

            String response;
            if(conversationHistory != null && !conversationHistory.isEmpty()){
                //有历史记录
                response = chatClient.prompt()
                        .system("以下是对话历史 \n" + conversationHistory)
                        .user(userMessage)
                        .call()
                        .content();
            }else {
                //无历史记录
                response = chatClient.prompt()
                        .user(userMessage)
                        .call()// 非流式调用，返回 ChatResponse
                        .content();// 提取内容字符串
            }
            if (response == null || response.trim().isEmpty()) {
                throw new AiServiceException("AI_EMPTY_RESPONSE", "AI 返回内容为空");
            }
            return response;
        }catch (Exception e){
            log.error("AI 调用失败: {}", e.getMessage(), e);
            throw AiServiceException.apiError("AI 调用失败: " + e.getMessage(), e);
        }

        /*
        //使用千问模型调用api
        try {
            Generation gen = new Generation();
            // 组装 messages
            List<Message> messages = new ArrayList<>();
            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                messages.add(Message.builder()
                        .role(Role.SYSTEM.getValue())
                        .content("以下是对话历史：\n" + conversationHistory)
                        .build());
            }
            messages.add(Message.builder()
                    .role(Role.USER.getValue())
                    .content(userMessage)
                    .build());
            // 构建参数
            QwenParam param = QwenParam.builder()
                    .apiKey(apiKey)
                    .model(model)
                    .messages(messages)
                    .resultFormat(QwenParam.ResultFormat.MESSAGE)
                    .temperature(0.7f)
                    .build();
            // 调用 API
            var result = gen.call(param);

            // 提取回答
            if (result.getOutput() != null
                    && result.getOutput().getChoices() != null
                    && !result.getOutput().getChoices().isEmpty()) {
                String content = result.getOutput().getChoices()
                        .get(0).getMessage().getContent();
                if (content != null && !content.trim().isEmpty()) {
                    return content;
                }
            }

            // 返回为空
            throw new AiServiceException("AI_EMPTY_RESPONSE", "AI 返回内容为空");
        } catch (Exception e) {
            // 统一兜底
            log.error("AI 调用失败: {}", e.getMessage(), e);
            throw AiServiceException.apiError("AI 调用失败: " + e.getMessage(), e);
        }*/
    }

    @Override
    public Flux<String> streamResponse(String userMessage, String conversationHistory) {
        // 参数校验
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return Flux.error(new AiServiceException("AI_INVALID_INPUT", "用户消息不能为空"));
        }

        try {
            if(conversationHistory != null && !conversationHistory.isEmpty()){
                //有历史记录
                return chatClient.prompt()
                        .system("以下是对话历史 \n" + conversationHistory)
                        .user(userMessage)
                        .stream()
                        .content();
            }else {
                return chatClient.prompt()
                        .user(userMessage)
                        .stream()// 流式调用，返回 Flux<ChatResponse>
                        .content();  // 提取内容 Flux<String>
            }
        }catch (Exception e){
            log.error("流式调用失败: {}", e.getMessage(), e);
            return Flux.error(AiServiceException.apiError("流式调用失败: " + e.getMessage(), e));
        }
    }
}
