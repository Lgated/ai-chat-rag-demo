package com.example.demo.service;

import com.example.demo.domain.Conversation;
import com.example.demo.domain.Message;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ChatService {

    //展示所有会话
    List<Conversation> listConversations();

    //创建会话
    Conversation creatConversation(String title);

    //获取会话
    Conversation getConversation(Long conversationId);

    //根据会话id展示信息
    List<Message> listMessages(Long conversationId);

    //添加信息
    Message addMessage(Long conversationId, String role, String content);

    //流式ai回复
    Flux<String> streamAiResponse(Long conversationId, String userMessage);

    //获取指定会话最新的用户消息
    Message getLatestUserMessage(Long conversationId, String content);

    //获取指定会话最新的AI助手消息
    Message getLatestAssistantMessage(Long conversationId, String content);

    //只保存用户消息，不触发AI
    Message addUserMessageOnly(Long conversationId, String content);
}
