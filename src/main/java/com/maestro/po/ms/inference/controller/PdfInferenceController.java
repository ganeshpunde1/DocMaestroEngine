package com.maestro.po.ms.inference.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.maestro.po.ms.inference.constants.ApplicationConstants;
import com.maestro.po.ms.inference.exception.BadDataException;
import com.maestro.po.ms.inference.model.annotation.InferenceResponse;
import com.maestro.po.ms.inference.model.rest.AcceptedApiResponse;
import com.maestro.po.ms.inference.model.rest.InferenceRestResponse;
import com.maestro.po.ms.inference.model.rest.PdfInferenceRequest;
import com.maestro.po.ms.inference.service.InferenceService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller that exposes endpoints for PDF-based AI inference and OCR extraction.
 * Accepts base64-encoded PDF documents, submits them asynchronously for processing
 * via AWS Bedrock and Textract, and provides endpoints to retrieve results.
 *
 * @author Ganesh Punde
 */
@Controller
@RequestMapping("/pdf")
@Slf4j
public class PdfInferenceController
{
    @Autowired
    @Qualifier("queuedInferenceService")
    InferenceService inferenceService;

    /**
     * Accepts a PDF inference request, creates a tracking record, and submits
     * the request asynchronously to the inference pipeline.
     *
     * @param pir the validated PDF inference request containing document data and queries
     * @return HTTP 202 Accepted with a request ID for polling results
     */
    @PostMapping(value =
    { "/inference/summarize", "/inference/summarize/" }, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AcceptedApiResponse> submitPdfSummarizationRequest(@Valid @RequestBody PdfInferenceRequest pir)
    {
        log.debug("Beginning inference for request");
        InferenceResponse ifr = inferenceService.createInference();
        inferenceService.executeInferencePipeline(pir, ifr);
        AcceptedApiResponse response = new AcceptedApiResponse("Accepted", 202, ifr.getRequestId());
        return ResponseEntity.accepted().body(response);
    }

    /**
     * Retrieves the inference result for a previously submitted request.
     *
     * @param requestId the unique request ID returned at submission time
     * @return the inference response containing status and answer payloads
     */
    @GetMapping(value =
    { "/inference/outcome/{request_id}", "/inference/outcome/{request_id}/" })
    public ResponseEntity<InferenceRestResponse> retrieveSummarizationResult(@PathVariable("request_id") String requestId)
    {
        if (requestId == null || requestId.isBlank())
            throw new BadDataException(ApplicationConstants.BLANK_REQUEST_ID_ERROR_CODE, "request_id cannot be null or blank.");

        InferenceRestResponse result = inferenceService.getInferenceResult(requestId);
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves the raw OCR extraction result produced by AWS Textract
     * for a previously submitted request.
     *
     * @param requestId the unique request ID returned at submission time
     * @return the inference response containing the raw Textract JSON output
     */
    @GetMapping(value =
    { "/inference/ocr/{request_id}", "/inference/ocr/{request_id}/" })
    public ResponseEntity<InferenceRestResponse> retrieveOcrExtractionResult(@PathVariable("request_id") String requestId)
    {
        if (requestId == null || requestId.isBlank())
            throw new BadDataException(ApplicationConstants.BLANK_REQUEST_ID_ERROR_CODE, "request_id cannot be null or blank.");

        InferenceRestResponse result = inferenceService.getOcrInferenceResult(requestId);
        return ResponseEntity.ok(result);
    }
}
