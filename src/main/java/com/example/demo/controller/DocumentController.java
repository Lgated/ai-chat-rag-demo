package com.example.demo.controller;

import com.example.demo.common.Dto.Result;
import com.example.demo.domain.Document;

import com.example.demo.service.DocumentService;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.List;

@RestController
@RequestMapping("/api/document")
@Slf4j
public class DocumentController {


    private static final String UPLOAD_DIR = "uploads";


    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }


    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<Result<Document>> uploadDocument(
            @RequestPart("file") FilePart filePart,
            @RequestPart(value = "description", required = false) FormFieldPart descriptionPart) {

        String description = descriptionPart != null ? descriptionPart.value() : null;

        return documentService.uploadDocument(filePart, description)
                .map(document -> Result.success("文档上传成功", document))
                .onErrorResume(e -> {
                    log.error("上传文档失败", e);
                    String message = e instanceof IllegalArgumentException
                            ? e.getMessage()
                            : "上传文档失败: " + e.getMessage();
                    return Mono.just(Result.error(message));
                });
    }

    @GetMapping("/list")
    public Mono<Result<List<Document>>> getAllDocuments() {
        return Mono.fromCallable(() -> {
            try {
                List<Document> documents = documentService.getAllDocuments();
                return Result.success(documents);
            } catch (Exception e) {
                log.error("获取文档列表失败", e);
                return Result.error("获取文档列表失败: " + e.getMessage());
            }
        });
    }

    @GetMapping("/{id}")
    public Mono<Result<Document>> getDocumentById(@PathVariable Long id) {
        return Mono.fromCallable(() -> {
            try {
                Document document = documentService.getDocumentById(id);
                return Result.success(document);
            } catch (IllegalArgumentException e) {
                return Result.error(e.getMessage());
            } catch (Exception e) {
                log.error("获取文档失败: ID={}", id, e);
                return Result.error("获取文档失败: " + e.getMessage());
            }
        });
    }

    @DeleteMapping("/{id}")
    public Mono<Result<String>> deleteDocument(@PathVariable Long id) {
        return Mono.fromCallable(() -> {
            try {
                documentService.deleteDocument(id);
                return Result.success("文档删除成功");
            } catch (IllegalArgumentException e) {
                return Result.error(e.getMessage());
            } catch (Exception e) {
                log.error("删除文档失败: ID={}", id, e);
                return Result.error("删除文档失败: " + e.getMessage());
            }
        });
    }
}
