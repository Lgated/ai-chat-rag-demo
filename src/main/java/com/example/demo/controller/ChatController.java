package com.example.demo.controller;

import com.example.demo.common.Dto.Result;
import com.example.demo.domain.Conversation;
import com.example.demo.domain.Message;
import com.example.demo.service.ChatService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService){
        this.chatService = chatService;
    }

    /**
     * 列出所有会话
     */
    @GetMapping("/conversations")
    public Result<List<Conversation>> listConversations(){
        List<Conversation> conversations = chatService.listConversations();
        return Result.success(conversations);
    }


    /**
     * 创建新会话
     * POST /api/chat/conversations
     * Body: { "title": "我的第一个会话" }
     */
    @PostMapping("/conversations")
    public Result<Conversation> createConversations(@RequestBody Map<String, String> request){
        String title = request.getOrDefault("title", "新会话");
        Conversation conversation = chatService.creatConversation(title);
        return Result.success("创建会话成功", conversation);
    }

    /**
     * 获取单个会话
     */
    @GetMapping("/conversations/{id}")
    public Result<Conversation> getConversation(@PathVariable Long id) {
        Conversation conversation = chatService.getConversation(id);
        return Result.success(conversation);
    }

    /**
     * 查看某个会话的消息
     */
    @GetMapping("/conversations/{id}/messages")
    public Result<List<Message>> listMessages(@PathVariable Long id) {
        List<Message> messages = chatService.listMessages(id);
        return Result.success(messages);
    }

    /**
     * 添加一条消息
     */
    @PostMapping("/conversations/{id}/messages")
    public Result<Message> addMessage(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String role = request.getOrDefault("role", "user");
        String content = request.getOrDefault("content","");

        // 参数验证
        if (content == null || content.trim().isEmpty()) {
            return Result.error("消息内容不能为空");
        }

        if (!"user".equals(role) && !"assistant".equals(role)) {
            return Result.error("角色不匹配");
        }

        Message message = chatService.addMessage(id, role, content);
        return Result.success("添加消息成功",message);
    }
}
