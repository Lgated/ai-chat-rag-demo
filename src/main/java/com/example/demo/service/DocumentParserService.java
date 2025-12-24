package com.example.demo.service;

import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

public interface DocumentParserService {

    /**
     * 解析文档，提取纯文本内容
     */
    String parseDocument(MultipartFile file);

    /**
     * 解析文档（响应式方式）
     */
    Mono<String> parseDocumentReactive(FilePart filePart);

    /**
     *  检查文本类型是否支持
     */
    boolean isSupportedFileType(String filename);

    /**
     * 获取文件类型
     */
    String getFileType(String filename);
}
