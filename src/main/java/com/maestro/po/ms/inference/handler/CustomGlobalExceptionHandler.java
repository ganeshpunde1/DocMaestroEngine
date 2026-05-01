package com.maestro.po.ms.inference.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.maestro.po.ms.inference.exception.BadDataException;
import com.maestro.po.ms.inference.exception.DataNotFoundException;
import com.maestro.po.ms.inference.exception.InferenceException;
import com.maestro.po.ms.inference.exception.InferenceGuardrailViolationException;
import com.maestro.po.ms.inference.model.rest.AcceptedApiResponse;

import lombok.extern.slf4j.Slf4j;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
@Slf4j
public class CustomGlobalExceptionHandler extends ResponseEntityExceptionHandler
{

    public static final String DEFAULT_MDC_UUID_TOKEN_KEY = "Slf4jMDCFilter.UUID";

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleConflict(Exception ex, WebRequest request)
    {
        log.error("@ExceptionHandler(Exception.class): ", ex);
        return new ResponseEntity<Object>(new AcceptedApiResponse(getPrefix() + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), null, "SYS-500"), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(BadDataException.class)
    protected ResponseEntity<Object> handleConflictBadDataException(BadDataException ex, WebRequest request)
    {
        log.error("@ExceptionHandler(BadDataException.class): ", ex);
        return new ResponseEntity<Object>(new AcceptedApiResponse(getPrefix() + ex.getMessage(), HttpStatus.BAD_REQUEST.value(), null, ex.getErrorCode()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataNotFoundException.class)
    protected ResponseEntity<Object> handleConflictDataNotFoundException(DataNotFoundException ex, WebRequest request)
    {
        log.error("@ExceptionHandler(DataNotFoundException.class): ", ex);
        return new ResponseEntity<Object>(new AcceptedApiResponse(getPrefix() + ex.getMessage(), HttpStatus.NOT_FOUND.value(), null, ex.getErrorCode()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InferenceException.class)
    protected ResponseEntity<Object> handleInferenceException(InferenceException ex, WebRequest request)
    {
        log.error("@ExceptionHandler(InferenceException.class): ", ex);
        return new ResponseEntity<Object>(new AcceptedApiResponse(getPrefix() + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), null, ex.getErrorCode()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(InferenceGuardrailViolationException.class)
    protected ResponseEntity<Object> handleGuardrailViolationException(InferenceGuardrailViolationException ex, WebRequest request)
    {
        log.error("@ExceptionHandler(InferenceGuardrailViolationException.class): ", ex);
        return new ResponseEntity<Object>(new AcceptedApiResponse(getPrefix() + ex.getMessage(), HttpStatus.FORBIDDEN.value(), null, ex.getErrorCode()), HttpStatus.FORBIDDEN);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request)
    {
        log.error("handleHttpRequestMethodNotSupported: ", ex);
        return new ResponseEntity<Object>(new AcceptedApiResponse(getPrefix() + ex.getMessage(), HttpStatus.METHOD_NOT_ALLOWED.value(), null, "HTTP-405"), HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request)
    {
        log.error("handleHttpMediaTypeNotSupported: ", ex);
        return new ResponseEntity<Object>(new AcceptedApiResponse(getPrefix() + ex.getMessage(), HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), null, "HTTP-415"), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request)
    {
        log.error("handleHttpMediaTypeNotAcceptable: ", ex);
        return new ResponseEntity<Object>(new AcceptedApiResponse(getPrefix() + ex.getMessage(), HttpStatus.NOT_ACCEPTABLE.value(), null, "HTTP-406"), HttpStatus.NOT_ACCEPTABLE);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request)
    {
        log.error("handleHttpMessageNotReadable: ", ex);
        return new ResponseEntity<Object>(new AcceptedApiResponse(getPrefix() + ex.getMessage(), HttpStatus.BAD_REQUEST.value(), null, "HTTP-400"), HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request)
    {
        log.error("handleMethodArgumentNotValid: ", ex);
        final List<String> errors = new ArrayList<String>();
        for (final FieldError error : ex.getBindingResult().getFieldErrors())
            errors.add(error.getDefaultMessage());
        for (final ObjectError error : ex.getBindingResult().getGlobalErrors())
            errors.add(error.getDefaultMessage());
        return new ResponseEntity<Object>(new AcceptedApiResponse(getPrefix() + errors.stream().collect(Collectors.joining(". ")), HttpStatus.BAD_REQUEST.value(), null, "VAL-400"), HttpStatus.BAD_REQUEST);
    }

    protected ResponseEntity<Object> handleMissingPathVariable(MissingPathVariableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request)
    {
        log.error("handleMissingPathVariable: ", ex);
        return new ResponseEntity<Object>(new AcceptedApiResponse(getPrefix() + " Path param missed. " + ex.getMessage(), HttpStatus.BAD_REQUEST.value(), null, "VAL-PATH-001"), HttpStatus.BAD_REQUEST);
    }

    private String getPrefix()
    {
        return "UUID : " + MDC.get(DEFAULT_MDC_UUID_TOKEN_KEY) + " : ";
    }
}
