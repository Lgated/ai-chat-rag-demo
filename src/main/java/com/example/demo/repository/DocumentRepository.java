package com.example.demo.repository;

import com.example.demo.domain.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface DocumentRepository extends JpaRepository<DocumentChunk,Long> {
    // 使用 pgvector 的相似度查询
    // 按向量距离升序取出与给定向量最相似的前 N 条记录
    @Query(value = "SELECT * FROM document_chunk " +
            // pgvector 的“欧氏距离（L2）”运算符；把前端传过来的字符串参数 :queryEmbedding 转成 vector 类型，
            // 然后与表里每行的 embedding 列计算距离，并按距离 从小到大 排序（距离越小越相似）
            "ORDER BY embedding <-> CAST(:queryEmbedding AS vector) " +
            //只取前 N 条，N 由参数 :limit 动态传入。
            "LIMIT :limit", nativeQuery = true)
    List<DocumentChunk> findSimilarChunks(@Param("queryEmbedding") String queryEmbedding,
                                          @Param("limit") int limit);



    /**
     * 使用原生 SQL 插入 DocumentChunk，正确处理 vector 类型转换
     * 注意：@Modifying 方法不能使用 RETURNING 子句，因为 executeUpdate() 期望返回 int 而不是结果集
     */
    //Modifying:告诉 Spring Data：这不是 SELECT，而是 INSERT/UPDATE/DELETE 这类会改变数据的语句，需要走 JDBC 的 executeUpdate 逻辑。
    @Modifying
    @Query(value = "INSERT INTO document_chunk (content, doc_id, embedding, created_at) " +
            "VALUES (:content, :docId, CAST(:embedding AS vector), :createdAt)", 
            nativeQuery = true)
    @Transactional
    void insertWithVector(@Param("content") String content,
                          @Param("docId") Long docId,
                          @Param("embedding") String embedding,
                          @Param("createdAt") LocalDateTime createdAt);

}
