package com.studyroom.service;

/** 业务异常：携带错误码，前端捕获后弹窗提示。 */
public class BusinessException extends RuntimeException {
    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
