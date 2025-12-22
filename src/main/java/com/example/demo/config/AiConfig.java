package com.example.demo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring AI 配置类
 * 手动配置 OpenAiChatModel 以确保 base-url 正确设置
 */
@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double temperature;

    /**
     * 手动创建 OpenAiApi，确保 base-url 配置正确
     * Spring AI 1.0.0-M4 的 OpenAiApi 构造函数：OpenAiApi(String baseUrl, String apiKey)
     */
    @Bean
    @ConditionalOnMissingBean
    public OpenAiApi openAiApi() {
        log.info("创建 OpenAiApi，baseUrl: {}, model: {}, apiKey: {}...", baseUrl, model, maskApiKey(apiKey));
        // 确保 baseUrl 不以 /v1 结尾，Spring AI 会自动添加
        String normalizedBaseUrl = baseUrl.endsWith("/v1") ? baseUrl.substring(0, baseUrl.length() - 3) : baseUrl;
        log.info("规范化后的 baseUrl: {}", normalizedBaseUrl);
        return new OpenAiApi(normalizedBaseUrl, apiKey);
    }

    private String maskApiKey(String key) {
        if (key == null || key.length() <= 8) {
            return "***";
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    /**
     * 手动创建 OpenAiChatModel，确保使用正确的 base-url
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean
    public ChatModel chatModel(OpenAiApi openAiApi) {
        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                .withModel(model)
                .withTemperature(temperature)
                .build();

        return new OpenAiChatModel(openAiApi, chatOptions);
    }

    /**
     * 创建 ChatClient bean
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .build();
    }
}

