package com.example.demo.service;

import com.example.demo.domain.Document;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

public interface DocumentService {

    /**
     * 上传文档（响应式）
     * @param file 文件
     * @param description 文档描述（可选）
     * @return 保存后的 Document 对象
     */
    Mono<Document> uploadDocument(FilePart filePart, String description);

    /**
     * 获取所有文档列表
     * @return 文档列表
     */
    java.util.List<Document> getAllDocuments();

    /**
     * 根据 ID 获取文档
     * @param docId 文档ID
     * @return Document 对象
     */
    Document getDocumentById(Long docId);

    /**
     * 删除文档（同时删除相关的 chunks）
     * @param docId 文档ID
     */
    void deleteDocument(Long docId);
}
