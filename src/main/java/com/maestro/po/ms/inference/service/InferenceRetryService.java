package com.maestro.po.ms.inference.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.maestro.po.ms.inference.constants.ApplicationConstants;
import com.maestro.po.ms.inference.exception.InferenceException;
import com.maestro.po.ms.inference.util.ResponseFormatValidator;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;

/**
 * Handles retrying failed AWS Bedrock Converse API calls with configurable
 * backoff and retry limits. Validates the response content type after each attempt.
 *
 * @author Ganesh Punde
 */
@Service
@Slf4j
public class InferenceRetryService
{
    @Autowired
    BedrockRuntimeAsyncClient asyncBedrockRuntimeClient;

    @Retryable(maxAttemptsExpression = "${maestro.inference.config.retry:2}", value =
    { InferenceException.class }, backoff = @Backoff(delayExpression = "${maestro.inference.config.retryDelay:2000}", multiplierExpression = "${maestro.inference.config.retryMultiply}"))
    /**
     * Retries a failed Bedrock ConverseRequest using Spring Retry.
     * Validates the response content type and throws {@link InferenceException}
     * if the response does not match the expected format.
     *
     * @param cr          the original ConverseRequest to retry
     * @param requestId   the request ID for logging
     * @param contentType the expected MIME type of the response
     * @return the successful ConverseResponse
     * @throws InferenceException if the retry fails or the content type does not match
     */
    public ConverseResponse retryConverseRequest(ConverseRequest cr, String requestId, String contentType)
    {
        log.debug("Request failed. Retrying requestId: " + requestId + " ...");
        try
        {
            ConverseResponse crd = this.asyncBedrockRuntimeClient.converse(cr).get();
            if (validateContentType(contentType, crd))
            {
                return crd;
            }
            else
            {
                throw new InferenceException(ApplicationConstants.CONTENT_TYPE_MISMATCH_ERROR_CODE,
                    "Bedrock could not coerce the input into the desired format for requestId: " + requestId);
            }
        }
        catch (Exception e)
        {
            throw new InferenceException(ApplicationConstants.BEDROCK_INVOCATION_ERROR_CODE, e.getMessage());
        }
    }

    private boolean validateContentType(String contentType, ConverseResponse t)
    {
        if (StringUtils.isBlank(contentType) || "text/plain".equalsIgnoreCase(contentType))
        {
            return true;
        }

        if (t == null || t.output() == null || t.output().message() == null || CollectionUtils.isEmpty(t.output().message().content()))
        {
            return false;
        }

        String contentLiteral = t.output().message().content().get(0).text();

        if ("application/json".equalsIgnoreCase(contentType))
        {
            return ResponseFormatValidator.isValidJson(contentLiteral);
        }

        if ("application/xml".equalsIgnoreCase(contentType))
        {
            return ResponseFormatValidator.isValidXML(contentLiteral);
        }

        return true;
    }
}