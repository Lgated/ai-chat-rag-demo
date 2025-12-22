package com.example.demo.service.Impl;

import com.example.demo.service.EmbeddingService;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingServiceImpl(EmbeddingModel embeddingModel){
        this.embeddingModel = embeddingModel;
    }

    //对文本生成embedding向量
    @Override
    public float[] embedText(String text) {
        // trim() : 把字符串首位的所有空白字符（空格、Tab、换行、全角空格等）去掉，返回一个新的字符串。
        if(text == null | text.trim().isEmpty()){
            throw new IllegalArgumentException("文本不能为空");
        }

        return embeddingModel.embed(text);
    }

    @Override
    public List<float[]> embedTexts(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        return embeddingModel.embed(texts);
    }

    @Override
    public String toVectorString(float[] embedding){
        if(embedding == null || embedding.length == 0){
            return "[]";
        }
        //转换为 PostgreSQL vector 格式
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if(i > 0){
                sb.append(",");
            }
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
