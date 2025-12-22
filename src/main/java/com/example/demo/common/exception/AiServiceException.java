package com.example.demo.common.exception;

/**
 * AI 服务异常
 */
public class AiServiceException extends RuntimeException {

    private final String errorCode;
    private final String errorMessage;

    public AiServiceException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public AiServiceException(String errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }


    public static AiServiceException apiKeyError(String message) {
        return new AiServiceException("AI_API_KEY_ERROR", "API Key 配置错误: " + message);
    }

    public static AiServiceException networkError(String message, Throwable cause) {
        return new AiServiceException("AI_NETWORK_ERROR", "网络连接失败: " + message, cause);
    }

    public static AiServiceException apiError(String message, Throwable cause) {
        return new AiServiceException("AI_API_ERROR", "API 调用失败: " + message, cause);
    }

    public static AiServiceException serviceUnavailable(String message) {
        return new AiServiceException("AI_SERVICE_UNAVAILABLE", "AI 服务暂不可用: " + message);
    }
}


