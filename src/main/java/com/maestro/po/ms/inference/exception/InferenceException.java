package com.maestro.po.ms.inference.exception;

public class InferenceException extends RuntimeException
{
    private static final long serialVersionUID = -2898994084626952363L;
    private final String errorCode;

    public InferenceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
