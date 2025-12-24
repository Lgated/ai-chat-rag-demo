package com.example.demo.service.Impl;

import com.example.demo.domain.Document;
import com.example.demo.repository.DocumentMetaRepository;
import com.example.demo.repository.DocumentRepository;
import com.example.demo.service.DocumentParserService;
import com.example.demo.service.DocumentService;
import com.example.demo.service.RagService;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Service
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    // 用于删除 chunks
    private final DocumentRepository documentRepository;
    private final DocumentMetaRepository documentMetaRepository;
    private final DocumentParserService documentParserService;
    private final RagService ragService;

    @Value("${app.upload.dir:uploads}")  // 可以从 application.yml 配置
    private String uploadDir;

    public DocumentServiceImpl(
            DocumentMetaRepository documentMetaRepository,
            DocumentRepository documentRepository,
            DocumentParserService documentParserService,
            RagService ragService) {
        this.documentMetaRepository = documentMetaRepository;
        this.documentRepository = documentRepository;
        this.documentParserService = documentParserService;
        this.ragService = ragService;
    }

    @PostConstruct
    public void init() {
        // 创建上传目录（在字段注入完成后执行）
        try {
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);
            log.info("文档上传目录已创建: {}", uploadPath.toAbsolutePath());
        } catch (Exception e) {
            log.error("创建上传目录失败: {}", uploadDir, e);
        }
    }


    @Override
    @Transactional
    public Mono<Document> uploadDocument(FilePart filePart, String description) {
        String filename = filePart.filename();
        if (filename.isEmpty()) {
            return Mono.error(new IllegalArgumentException("文件名不能为空"));
        }

        // 验证文件类型
        if (!documentParserService.isSupportedFileType(filename)) {
            return Mono.error(new IllegalArgumentException("不支持的文件类型，支持：PDF、Word、Excel、PPT、TXT、Markdown"));
        }

        // 保存文件到本地
        String savedFilename = System.currentTimeMillis() + "_" + filename;
        Path filePath = Paths.get(uploadDir, savedFilename);

        // 1. 先保存文件，然后解析内容
        return DataBufferUtils.write(filePart.content(), filePath,   StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING)
                .then(documentParserService.parseDocumentReactive(filePart))
                .flatMap(content -> {
                    // 2. 保存文档元信息
                    Document document = new Document();
                    document.setFilename(filename);
                    document.setFileType(documentParserService.getFileType(filename));
                    try {
                        document.setFileSize(Files.size(filePath));
                    } catch (Exception e) {
                        log.warn("获取文件大小失败", e);
                        document.setFileSize(0L);
                    }
                    document.setDescription(description);
                    document.setCreatedBy("system");

                    Document savedDocument = documentMetaRepository.save(document);
                    log.info("文档元信息已保存: ID={}, filename={}", savedDocument.getId(), filename);

                    // 3. 文档入库（切分 + embedding + 存储）
                    try {
                        ragService.ingestDocument(content, savedDocument.getId());
                        log.info("文档已入库: ID={}, filename={}", savedDocument.getId(), filename);
                    } catch (Exception e) {
                        log.error("文档入库失败: ID={}, filename={}", savedDocument.getId(), filename, e);
                        documentMetaRepository.delete(savedDocument);
                        try {
                            Files.deleteIfExists(filePath);
                        } catch (Exception ex) {
                            log.warn("删除文件失败", ex);
                        }
                        return Mono.<Document>error(new RuntimeException("文档入库失败: " + e.getMessage(), e));
                    }

                    return Mono.just(savedDocument);
                })
                .doOnError(error -> {
                    // 出错时删除文件
                    try {
                        Files.deleteIfExists(filePath);
                    } catch (Exception e) {
                        log.warn("删除文件失败", e);
                    }
                });
    }

    @Override
    public List<Document> getAllDocuments() {
        return documentMetaRepository.findAll();
    }

    @Override
    public Document getDocumentById(Long docId) {
        return documentMetaRepository.findById(docId).orElseThrow(() -> new IllegalArgumentException("文档不存在：" + docId));
    }

    @Override
    public void deleteDocument(Long docId) {
        // 1. 删除文档元信息
        Document document = documentMetaRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在: " + docId));
        // 删除相关的 chunks
        documentRepository.deleteByDocId(docId);

        documentMetaRepository.delete(document);
        log.info("文档已删除: ID={}, filename={}", docId, document.getFilename());
    }
}
