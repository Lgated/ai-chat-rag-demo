package com.example.demo.service;

import com.example.demo.domain.Message;
import reactor.core.publisher.Flux;

import java.util.List;

public interface RagService {

    /**
     * 文档入库（切分 + embedding + 存储）
     */
    void ingestDocument(String documentContent, Long docId);

    /**
     * RAG 检索：根据问题找到相关文档片段
     */
    List<String> retrieveRelevantChunks(String query, int topK);

    /**
     * 使用 RAG 生成回答 : 非流式
     */
    String generateRagResponse(String query, List<String> relevantChunks);

    /**
     * RAG 流式对话：检索文档 + 流式生成回答
     * @param query 用户问题
     * @param conversationId 会话ID（用于获取历史消息）
     * @param topK 检索的文档片段数量
     * @return 流式回答
     */
    Flux<String> streamRagResponse(Long conversationId, String query, int topK);

    /**
     * RAG 对话（完整流程）：检索 + 生成 + 保存消息
     * @param conversationId 会话ID
     * @param query 用户问题
     * @return AI回答的Messagee
     */
    Message ragChat(Long conversationId, String query);
}
