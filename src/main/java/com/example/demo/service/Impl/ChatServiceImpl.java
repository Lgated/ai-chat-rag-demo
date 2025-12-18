package com.example.demo.service.Impl;

import com.example.demo.domain.Conversation;
import com.example.demo.domain.Message;
import com.example.demo.repository.ConversationRepository;
import com.example.demo.repository.MessageRepository;
import com.example.demo.service.ChatService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {


    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ChatServiceImpl(ConversationRepository conversationRepository,
                           MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
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

    //根据会话id添加一条消息
    @Override
    public Message addMessage(Long conversationId, String role, String content) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(()->new IllegalArgumentException("会话不存在" + conversationId));

        Message message = new Message();
        message.setConversation(conversation);
        message.setContent(content);
        message.setRole(role);
        return messageRepository.save(message);
    }
}
