package com.example.demo.service;

import com.example.demo.domain.Conversation;
import com.example.demo.domain.Message;

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
}
