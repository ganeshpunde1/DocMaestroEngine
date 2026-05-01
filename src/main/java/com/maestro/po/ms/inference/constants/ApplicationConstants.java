package com.maestro.po.ms.inference.constants;

/**
 * Application level constants.
 *
 * @author Ganesh Punde
 */
public final class ApplicationConstants
{
    private ApplicationConstants() {    }

    // ── Guardrail ────────────────────────────────────────────────────────────
    public static final String AWS_GUARDRAIL_CONFIG_ERROR_CODE    = "AWS-BR-GR-CONFIG-001";
    public static final String AWS_GUARDRAIL_CONFIG_ERROR_MSG     = "Guardrail ID and Version must be provided when guardrail is enabled.";

    // ── Request validation ───────────────────────────────────────────────────
    public static final String NULL_REQUEST_ERROR_CODE            = "VAL-REQ-001";
    public static final String NULL_REQUEST_ERROR_MSG             = "Request or request ID cannot be null or blank.";

    public static final String BLANK_REQUEST_ID_ERROR_CODE        = "VAL-REQ-002";
    public static final String BLANK_REQUEST_ID_ERROR_MSG         = "requestId cannot be null or empty.";

    // ── Inference pipeline ───────────────────────────────────────────────────
    public static final String INFERENCE_PIPELINE_ERROR_CODE      = "INF-PIPE-001";
    public static final String INFERENCE_PIPELINE_ERROR_MSG       = "An error occurred during the inference pipeline execution.";

    public static final String BEDROCK_INVOCATION_ERROR_CODE      = "INF-BR-001";
    public static final String BEDROCK_INVOCATION_ERROR_MSG       = "Failed to invoke AWS Bedrock Converse API.";

    public static final String BEDROCK_RETRY_EXHAUSTED_ERROR_CODE = "INF-BR-002";
    public static final String BEDROCK_RETRY_EXHAUSTED_ERROR_MSG  = "Bedrock retries exhausted. Could not obtain a valid response.";

    public static final String CONTENT_TYPE_MISMATCH_ERROR_CODE   = "INF-BR-003";
    public static final String CONTENT_TYPE_MISMATCH_ERROR_MSG    = "Bedrock response does not match the requested content type.";

    // ── Textract ─────────────────────────────────────────────────────────────
    public static final String TEXTRACT_S3_UPLOAD_ERROR_CODE      = "TXT-S3-001";
    public static final String TEXTRACT_S3_UPLOAD_ERROR_MSG       = "Failed to upload document to S3 for Textract processing.";

    public static final String TEXTRACT_EMPTY_RESULT_ERROR_CODE   = "TXT-JOB-001";
    public static final String TEXTRACT_EMPTY_RESULT_ERROR_MSG    = "Textract returned no blocks for the submitted document.";

    public static final String TEXTRACT_JOB_ERROR_CODE            = "TXT-JOB-002";
    public static final String TEXTRACT_JOB_ERROR_MSG             = "Textract job failed to start or complete.";

    public static final String TEXTRACT_JSON_SERIALIZE_ERROR_CODE = "TXT-SER-001";
    public static final String TEXTRACT_JSON_SERIALIZE_ERROR_MSG  = "Failed to serialize Textract output to JSON.";

    // ── Spring context ───────────────────────────────────────────────────────
    public static final String SPRING_CONTEXT_NULL_ERROR_CODE     = "CTX-001";
    public static final String SPRING_CONTEXT_NULL_ERROR_MSG      = "Spring ApplicationContext has not been initialized.";

    // ── Property / config ────────────────────────────────────────────────────
    public static final String PROPERTY_NOT_FOUND_ERROR_CODE      = "CFG-001";
    public static final String PROPERTY_NOT_FOUND_ERROR_MSG       = "Required application property is missing or blank.";

    public static final String PROPERTY_PARSE_ERROR_CODE          = "CFG-002";
    public static final String PROPERTY_PARSE_ERROR_MSG           = "Application property could not be parsed to the required type.";

    // ── Data not found ───────────────────────────────────────────────────────
    public static final String INFERENCE_NOT_FOUND_ERROR_CODE     = "DATA-001";
    public static final String INFERENCE_NOT_FOUND_ERROR_MSG      = "No inference record found for the given request ID.";

    public static final String OCR_NOT_FOUND_ERROR_CODE           = "DATA-002";
    public static final String OCR_NOT_FOUND_ERROR_MSG            = "No OCR extraction record found for the given request ID.";
}
