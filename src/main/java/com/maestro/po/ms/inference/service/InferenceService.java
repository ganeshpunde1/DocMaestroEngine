package com.maestro.po.ms.inference.service;

import com.maestro.po.ms.inference.model.annotation.InferenceResponse;
import com.maestro.po.ms.inference.model.rest.InferenceRestResponse;
import com.maestro.po.ms.inference.model.rest.PdfInferenceRequest;

/**
 * Service contract for managing the PDF inference lifecycle.
 * Defines operations for submitting inference requests, creating tracking records,
 * and retrieving inference and OCR results.
 *
 * @author Ganesh Punde
 */
public interface InferenceService
{
    /**
     * Submits a PDF inference request for asynchronous processing through
     * the Bedrock and Textract pipeline.
     *
     * @param pr        the PDF inference request containing document data and queries
     * @param requestId the inference response tracking record
     */
    public void executeInferencePipeline(PdfInferenceRequest pr, InferenceResponse requestId);
    
    /**
     * Creates and persists a new inference tracking record with IN_PROGRESS status.
     *
     * @return the newly created {@link InferenceResponse} with a generated request ID
     */
    public InferenceResponse createInference();

    /**
     * Retrieves the inference result including all Bedrock answers for a given request ID.
     *
     * @param requestId the unique request ID
     * @return the inference response with status and answer list
     */
    public InferenceRestResponse getInferenceResult(String requestId);
    
    /**
     * Retrieves the raw OCR extraction result produced by AWS Textract for a given request ID.
     *
     * @param requestId the unique request ID
     * @return the inference response containing the raw Textract JSON
     */
    public InferenceRestResponse getOcrInferenceResult(String requestId);
   
}
