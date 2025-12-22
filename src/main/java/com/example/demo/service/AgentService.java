package com.example.demo.service;

import reactor.core.publisher.Flux;

public interface AgentService {
    /**
     * 非流式处理（带工具调用）
     */
    String processWithTools(String userMessage);


    /**
     * 流式处理（带工具调用）
     * @param conversationId 会话ID（用于获取历史消息）
     * @param userMessage 用户消息
     * @return 流式回答
     */
    Flux<String> streamProcessWithTools(Long conversationId, String userMessage);
}
