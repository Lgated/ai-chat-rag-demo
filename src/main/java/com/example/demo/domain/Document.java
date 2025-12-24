package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "document")
@Data
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;  // 原始文件名

    @Column(nullable = false) // 非空
    private String fileType;   // 文件类型：pdf, docx, xlsx 等

    @Column
    private Long fileSize;     // 文件大小（字节）

    @Column(columnDefinition = "text")
    private String description; // 文档描述

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy;   // 上传用户（后续扩展）

}
