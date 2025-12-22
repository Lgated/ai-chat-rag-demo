package com.example.demo.service;

import java.util.List;

public interface EmbeddingService {

    /**
     * 对文本生成 embedding 向量
     */
    float[] embedText(String text);

    /**
     * 批量生成 embedding
     */
    List<float[]> embedTexts(List<String> texts);

    /**
     * 将 float[] 转换为 PostgreSQL vector 格式的字符串
     */
    String toVectorString(float[] embedding);
}
