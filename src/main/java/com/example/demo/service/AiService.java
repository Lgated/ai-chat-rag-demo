package com.example.demo.service;

import reactor.core.publisher.Flux;

public interface AiService {
    /**
     * 调用大模型生成回答（非流式）
     * @param userMessage 用户消息
     * @param conversationHistory 历史消息（用于上下文）
     * @return AI 生成的回答
     */
    String generateResponse(String userMessage, String conversationHistory);

    /**
     * ai流式回答
     */
    Flux<String> streamResponse(String userMessage, String conversationHistory);
}
