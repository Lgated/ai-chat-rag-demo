package com.example.demo.service.Impl;

import com.example.demo.domain.Conversation;
import com.example.demo.domain.Message;
import com.example.demo.repository.ConversationRepository;
import com.example.demo.repository.MessageRepository;
import com.example.demo.service.AiService;
import com.example.demo.service.ChatService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    //根据会话id添加一条消息
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
}
