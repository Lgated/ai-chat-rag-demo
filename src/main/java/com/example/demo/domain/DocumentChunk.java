package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "document_chunk")
public class DocumentChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doc_id")
    private Long docId;  // 文档 ID（可以用来关联多个 chunk 属于同一个文档）

    @Column(columnDefinition = "text", nullable = false)
    private String content;  // 文档内容

    @Column(columnDefinition = "vector(1536)", nullable = false)  // 1536 是 OpenAI embedding 的维度
    private String embedding;  // 向量（PostgreSQL 的 vector 类型）

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
