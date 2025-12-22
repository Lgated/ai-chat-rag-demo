package com.example.demo.common.exception;

import com.example.demo.common.Dto.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 统一处理所有异常，返回统一格式的响应
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理 AI 服务异常
     */
    @ExceptionHandler(AiServiceException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleAiServiceException(AiServiceException e) {
        log.error("AI 服务异常: [{}] {}", e.getErrorCode(), e.getErrorMessage(), e);
        
        // 根据错误码返回不同的状态码
        Integer code = switch (e.getErrorCode()) {
            case "AI_API_KEY_ERROR" -> 4001;  // API Key 错误
            case "AI_NETWORK_ERROR" -> 4002;  // 网络错误
            case "AI_API_ERROR" -> 4003;      // API 调用错误
            case "AI_SERVICE_UNAVAILABLE" -> 4004;  // 服务不可用
            case "AI_INVALID_INPUT" -> 4005;  // 输入无效
            case "AI_EMPTY_RESPONSE" -> 4006; // 返回为空
            default -> 5000;  // 未知错误
        };
        
        return Result.error(code, e.getErrorMessage());
    }

    /**
     * 处理参数错误（IllegalArgumentException）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数错误: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return Result.error(500, "服务器内部错误，请稍后重试");
    }
}


