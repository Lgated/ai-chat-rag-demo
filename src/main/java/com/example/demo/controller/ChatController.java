package com.example.demo.controller;

import com.example.demo.common.Dto.Result;
import com.example.demo.domain.Conversation;
import com.example.demo.domain.Message;
import com.example.demo.service.AgentService;
import com.example.demo.service.ChatService;
import com.example.demo.service.RagService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final RagService ragService;
    private final AgentService agentService;


    public ChatController(ChatService chatService,
                          RagService ragService,
                          AgentService agentService){
        this.chatService = chatService;
        this.ragService = ragService;
        this.agentService = agentService;
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
     * 添加一条消息 - 非流式子
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

    /**
     * produces = MediaType.TEXT_EVENT_STREAM_VALUE ： 告诉前端「这是一个持续的流式响应，不是一次性返回的 JSON」
     * 通过 SSE 协议向前端实时推送 AI 的流式回答，最后推送流结束标记
     */

    @GetMapping(value = "/conversations/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@PathVariable Long id,@RequestParam String message){

        // 调用 AI 流式生成（需要实现流式版本的 AiService）
        return chatService.streamAiResponse(id, message)
                //将 AI 逐段返回的纯文本内容，封装为 SSE 事件对象（前端能识别的格式
                .map(content -> ServerSentEvent.<String>builder()
                        .data(content)
                        .build())
                //追加一个「流结束标记」的 SSE 事件，告知前端「AI 回答已全部返回」；
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .data("[DONE]")  // 流结束标记
                        .build()));
    }

    //获取最新用户消息
    @GetMapping("conversations/{id}/latestUserMessage")
    public Result<Message> getLatestUserMessage(@PathVariable Long id,@RequestParam String message){
        Message latestUserMessage = chatService.getLatestUserMessage(id, message);
        if (latestUserMessage == null) {
            return Result.error("未找到对应的用户消息");
        }
        return Result.success(latestUserMessage);
    }

    //获取最新ai返回信息
    @GetMapping("conversations/{id}/latestAssistantMessage")
    public Result<Message> getLatestAssistantMessage(@PathVariable Long id,@RequestParam String content){
        Message latestAssistantMessage = chatService.getLatestAssistantMessage(id, content);
        if (latestAssistantMessage == null) {
            return Result.error("未找到对应的AI助手消息");
        }
        return Result.success(latestAssistantMessage);
    }

    /**
     * RAG 对话接口（带知识库检索） - 非流式
     */
    @PostMapping("conversations/{id}/rag")
    public Result<Message> ragChat( @PathVariable Long id,
                                    @RequestBody Map<String, String> request){
        String content = request.getOrDefault("content","");
        Message assistantMessage = ragService.ragChat(id, content);
        return Result.success(assistantMessage);
    }


    /**
     * RAG 流式对话接口
     */
    @GetMapping(value = "/conversations/{id}/rag-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> ragStreamChat(
            @PathVariable Long id,
            @RequestParam String message) {
        return ragService.streamRagResponse(id,message,5)
                .map(content -> ServerSentEvent.<String>builder()
                        .data(content)
                        .build())
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .data("[DONE]")
                        .build())
                );
    }

    @PostMapping(value="/conversations/{id}/rag-stream", produces=MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> ragStreamChatPost(@PathVariable Long id,
                                                           @RequestBody Map<String,String> body) {
        String message = body.getOrDefault("message", "");
        return ragService.streamRagResponse(id, message, 5)
                .map(c -> ServerSentEvent.<String>builder().data(c).build())
                .concatWith(Flux.just(ServerSentEvent.<String>builder().data("[DONE]").build()));
    }

    /**
     * Agent 流式对话接口
     * GET /api/chat/conversations/{id}/agent-stream?message=xxx
     */
    @GetMapping(value = "/conversations/{id}/agent-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> agentStreamChat(
            @PathVariable Long id,
            @RequestParam String message) {

        return agentService.streamProcessWithTools(id, message)
                .map(content -> ServerSentEvent.<String>builder()
                        .data(content)
                        .build())
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .data("[DONE]")
                        .build()));
    }

    /**
     * 文档入库接口
     */
    @PostMapping("rag/ingest")
    public Result<String> ingestDocument(@RequestBody Map<String, Object> request){
        String content = (String) request.get("content");
        Long docId = request.containsKey("docId") ?
                Long.valueOf(request.get("docId").toString()) : null;

        if (content == null || content.isEmpty()) {
            return Result.error("文档内容不能为空");
        }

        ragService.ingestDocument(content, docId);
        return Result.success("文档入库成功");
    }


}
