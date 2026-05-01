package com.maestro.po.ms.inference.exception;

public class DataNotFoundException extends RuntimeException
{
    private static final long serialVersionUID = 1L;
    private final String errorCode;

    public DataNotFoundException(String errorCode, String message)
    {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}