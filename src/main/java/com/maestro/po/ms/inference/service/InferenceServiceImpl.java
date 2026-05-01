package com.maestro.po.ms.inference.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.maestro.po.ms.inference.constants.ApplicationConstants;
import com.maestro.po.ms.inference.exception.BadDataException;
import com.maestro.po.ms.inference.exception.InferenceException;
import com.maestro.po.ms.inference.exception.InferenceGuardrailViolationException;
import com.maestro.po.ms.inference.model.annotation.InferenceAnswer;
import com.maestro.po.ms.inference.model.annotation.InferenceResponse;
import com.maestro.po.ms.inference.model.annotation.PdfTextExtractJson;
import com.maestro.po.ms.inference.model.rest.GenericInfo;
import com.maestro.po.ms.inference.model.rest.InferenceQuery;
import com.maestro.po.ms.inference.model.rest.InferenceRestAnswer;
import com.maestro.po.ms.inference.model.rest.InferenceRestResponse;
import com.maestro.po.ms.inference.model.rest.InferenceSchema;
import com.maestro.po.ms.inference.model.rest.PdfInferenceRequest;
import com.maestro.po.ms.inference.repository.InferenceAnswerRepository;
import com.maestro.po.ms.inference.repository.InferenceResponseRepository;
import com.maestro.po.ms.inference.util.ResponseFormatValidator;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.DocumentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.DocumentSource;
import software.amazon.awssdk.services.bedrockruntime.model.GuardrailConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.GuardrailTrace;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;

/**
 * Core implementation of the inference pipeline that orchestrates AWS Bedrock
 * and Textract calls for each query in a PDF inference request.
 * Handles guardrail configuration, token throttling, response validation,
 * JSON merging, and persistence of inference answers.
 *
 * @author Ganesh Punde
 */
@Component
@Slf4j
public class InferenceServiceImpl implements InferenceService
{
    @Autowired
    InferenceAnswerRepository answerRepository;

    @Autowired
    InferenceResponseRepository inferenceResponseRepository;

    @Autowired
    InferenceRetryService inferenceRetryService;

    @Autowired
    BedrockRuntimeAsyncClient asyncBedrockRuntimeClient;
    
    @Autowired
    TokenThrottleService tokenThrottleService;
    
    @Autowired
    TextExtractionService textExtractionService;

    @Value("${maestro.inference.config.temp}")
    private Float defaultTemp;

    @Value("${maestro.inference.config.topp}")
    private Float defaultTopP;
    
    @Value("${maestro.inference.config.maxWait:3600000}")
    private long maxWaitTime;
    
    @Value("${maestro.inference.guardrail.id}")
    private String guardrailId;

    @Value("${maestro.inference.guardrail.version}")
    private String guardrailVersion;
    
	@Value("${maestro.inference.guardrail.enabled}")
	private boolean guardrailEnabled;

    /**
     * Entry point for async inference execution. Validates inputs then delegates
     * to {@link #processInferencePipeline}.
     *
     * @param pr        the PDF inference request
     * @param requestId the inference tracking record
     */
    @Override
    @Async
    public void executeInferencePipeline(PdfInferenceRequest pr, InferenceResponse requestId)
    {
        if (pr == null || requestId == null || requestId.getRequestId() == null || requestId.getRequestId().isBlank())
        {
            log.error("{}: {}", ApplicationConstants.NULL_REQUEST_ERROR_CODE, ApplicationConstants.NULL_REQUEST_ERROR_MSG);
            return;
        }
        
        processInferencePipeline(pr, requestId);
    }
    
    /**
     * Orchestrates the full inference pipeline for all queries in the request.
     * For each query, optionally runs Textract extraction, builds a Bedrock
     * ConverseRequest, applies guardrail configuration, and submits asynchronously.
     *
     * @param pr        the PDF inference request
     * @param requestId the inference tracking record
     */
    protected void processInferencePipeline(PdfInferenceRequest pr, InferenceResponse requestId)
    {
        List<CompletableFuture<Void>> responses = new ArrayList<>();
        FailureCount failures = new FailureCount(0);
        for (InferenceQuery query : pr.getQueries())
        {
        	String json =null;
        	if (query.getExtractionInfo() != null && !CollectionUtils.isEmpty(query.getExtractionInfo().getExtractionFields()))
            {
            	json = textExtractionService.extractJson(pr, query.getExtractionInfo(), requestId);
                if (StringUtils.isBlank(json))
                {
                    //If Textract failed to read the required fields, continue to next request
                    log.error("{}: Textract returned blank JSON for requestId: {}",
                        ApplicationConstants.TEXTRACT_EMPTY_RESULT_ERROR_CODE, requestId.getRequestId());
                    failures.increment();
                    CompletableFuture<Void> ft = new CompletableFuture<Void>();
                    ft.complete(null);
                    responses.add(ft);
                    continue;
                }
            
				if (Boolean.TRUE.equals(query.isTextractOnly())) {
					final String finalJson =json; 
					log.debug("Textract-only – skipping Bedrock for request {}", requestId.getRequestId());
					CompletableFuture.runAsync(
							() -> new PostTextractFunction(query.getQuestion(), requestId.getInferenceResponseId(),
									query.getMimeType(), query.getQuestionKey()).accept(finalJson));

					responses.add(CompletableFuture.completedFuture(null));
					continue;
				}
			}

            ContentBlock doc = createPdfDocumentBlock(pr.getPdfData());
            ContentBlock contentQuery = ContentBlock.fromText(query.getQuestion());
            Message bedrockMessage = Message.builder().role(ConversationRole.USER).content(doc, contentQuery).build();
            InferenceSchema schema = buildInferenceSchema(query.getQuestionKey(), pr.getGenericInfo());
            schema.setContentType(query.getMimeType());
            InferenceConfiguration inferenceConfig = buildInferenceConfig(query);
            ConverseRequest converseRequest = null;
         
			log.info("Preparing ConverseRequest... GuardrailEnabled={}, ID={}, Version={}, ModelID={}",	guardrailEnabled, guardrailId, guardrailVersion, pr != null ? pr.getModelId() : "pr is null");
            if(guardrailEnabled)
            {
				if (guardrailId == null || guardrailVersion == null) {
					log.error(ApplicationConstants.AWS_GUARDRAIL_CONFIG_ERROR_CODE+" : "+ApplicationConstants.AWS_GUARDRAIL_CONFIG_ERROR_MSG);
					throw new InferenceGuardrailViolationException(ApplicationConstants.AWS_GUARDRAIL_CONFIG_ERROR_CODE,ApplicationConstants.AWS_GUARDRAIL_CONFIG_ERROR_MSG);
				}
            	log.debug("Building ConverseRequest with GuardrailConfiguration...");
				converseRequest = ConverseRequest.builder().messages(bedrockMessage).modelId(pr.getModelId()) 
						.inferenceConfig(inferenceConfig)
						.guardrailConfig(GuardrailConfiguration.builder().guardrailIdentifier(guardrailId).trace(GuardrailTrace.ENABLED) 
								.guardrailVersion(guardrailVersion) 
								.build())
						.build();
				log.info("ConverseRequest built with guardrailrail.");
			} else {
				log.debug("Building ConverseRequest without GuardrailConfiguration...");
				converseRequest = ConverseRequest.builder().messages(bedrockMessage).modelId(pr.getModelId())
						.inferenceConfig(inferenceConfig).build();
				log.info("ConverseRequest built without guardrailrail.");
			}
            
            this.tokenThrottleService.throttle();
            log.debug("Invoking bedrock for requestId: " + requestId.getRequestId());

            CompletableFuture<Void> cr = this.asyncBedrockRuntimeClient.converse(converseRequest)
                    .handle(new InspectConverseResponse(failures, requestId.getRequestId(), converseRequest, schema))
                    .thenAccept(new PostConverseFunction(query.getQuestion(), requestId.getInferenceResponseId(), query.getMimeType(), query.getQuestionKey(), json))
                    .exceptionally(ex -> {
                        log.error("{}: Async Bedrock invocation failed for requestId: {} - {}",
                            ApplicationConstants.BEDROCK_INVOCATION_ERROR_CODE, requestId.getRequestId(), ex.getMessage());
                        failures.increment();
                        return null;
                    });

            responses.add(cr);
        }

        CompletableFuture.runAsync(new InferenceResponseCompletionTask(responses, requestId.getInferenceResponseId(), failures));

    }

    /**
     * Builds an {@link InferenceSchema} by looking up a schema key in the generic info list.
     *
     * @param questionKey the question key used to derive the schema key
     * @param genericInfo list of generic key-value metadata
     * @return the populated InferenceSchema, or an empty one if no match found
     */
    protected InferenceSchema buildInferenceSchema(String questionKey, List<GenericInfo> genericInfo)
    {
        InferenceSchema schema = new InferenceSchema();
        if (!CollectionUtils.isEmpty(genericInfo) && StringUtils.isNotBlank(questionKey))
        {
            
            String schemaKey = questionKey +"_schema";
            Optional<GenericInfo> info = genericInfo.stream().filter(gi -> schemaKey.equalsIgnoreCase(gi.getKey())).findFirst();
            info.ifPresent(q -> {
                schema.setSchema((String) q.getValue());
            });
        }
        return schema;
    }

    @Getter
    @AllArgsConstructor
    protected class FailureCount
    {
        private Integer failures;

        public void increment()
        {
            failures++;
        }
    }

    @AllArgsConstructor
    protected class InspectConverseResponse implements BiFunction<ConverseResponse, Throwable, ConverseResponse>
    {
        private FailureCount failure;
        private String requestId;
        private ConverseRequest request;
        private InferenceSchema schema;

        @Override
        public ConverseResponse apply(ConverseResponse t, Throwable thr)
        {
            boolean contentTypeMatches = validateResponseContentType(schema.getContentType(), t);
            if (t != null && t.sdkHttpResponse() != null && t.sdkHttpResponse().isSuccessful() && contentTypeMatches)
            {
                log.debug("Request successful for requestId: " + requestId);
                return t;
            }
            else if (t != null && t.sdkHttpResponse() != null && t.sdkHttpResponse().isSuccessful() && !contentTypeMatches)
            {
                ContentBlock bedrockResponse = ContentBlock.fromText(t.output().message().content().get(0).text());
                Message bedrockMessage = Message.builder().role(ConversationRole.ASSISTANT).content(bedrockResponse).build();
                List<Message> newMessages = new ArrayList<>();
                newMessages.addAll(request.messages());
                newMessages.add(bedrockMessage);

                ContentBlock reprompt = ContentBlock.fromText(
                        "The content you provided does not match the requested content type of " + schema.getContentType() + " Make sure the response matches the content of " + schema.getContentType());
                Message systemReprompt = Message.builder().role(ConversationRole.USER).content(reprompt).build();
                newMessages.add(systemReprompt);
                request = request.copy(bldr -> {
                    bldr.messages(newMessages);
                });
            }
            log.debug("Error recieved for requestId: " + requestId + " " + thr);

            ConverseResponse tx = null;
            try
            {
                tx = inferenceRetryService.retryConverseRequest(request, requestId, schema.getContentType());
            }
            catch (Exception e)
            {
                log.error("{}: {} for requestId: {}",
                    ApplicationConstants.BEDROCK_RETRY_EXHAUSTED_ERROR_CODE,
                    ApplicationConstants.BEDROCK_RETRY_EXHAUSTED_ERROR_MSG, requestId);
            }

            if (tx == null)
            {
                failure.increment();
            }
            log.debug("Request Id " + requestId + " The ConverseResponse " + t + " came with an error: " + thr);
            return tx;
        }

        private boolean validateResponseContentType(String contentType, ConverseResponse t)
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
                String strippedLiteral = contentLiteral.replace("```json", "").replace("```", "");
                return ResponseFormatValidator.isValidJson(strippedLiteral);
            }

            if ("application/xml".equalsIgnoreCase(contentType))
            {
                return ResponseFormatValidator.isValidXML(contentLiteral);
            }

            return true;
        }

    }

    /**
     * Builds an {@link InferenceConfiguration} from the query's temperature and max token settings.
     *
     * @param query the inference query containing model parameters
     * @return the configured InferenceConfiguration
     */
    protected InferenceConfiguration buildInferenceConfig(InferenceQuery query)
    {
        Float temp = Optional.ofNullable(query.getTemperature()).orElse(BigDecimal.valueOf(defaultTemp.doubleValue())).floatValue();
        //Float topp = Optional.ofNullable(query.getTopP()).orElse(BigDecimal.valueOf(defaultTopP.doubleValue())).floatValue();

        return InferenceConfiguration.builder().maxTokens(query.getMaxTokens()).temperature(temp).build();
    }

    /**
     * Decodes a base64-encoded PDF and wraps it as a Bedrock {@link ContentBlock} document.
     *
     * @param base64 the base64-encoded PDF content
     * @return a ContentBlock containing the PDF as a document source
     */
    protected ContentBlock createPdfDocumentBlock(String base64)
    {
        String dummyFileName = UUID.randomUUID().toString();
        byte[] bytes = Base64.getDecoder().decode(base64);
        SdkBytes sdkBytes = SdkBytes.fromByteArray(bytes);
        DocumentSource ds = DocumentSource.fromBytes(sdkBytes);
        DocumentBlock db = DocumentBlock.builder().source(ds).format("pdf").name(dummyFileName).build();
        ContentBlock document = ContentBlock.fromDocument(db);
        return document;
    }

    /**
     * Creates and persists a new {@link InferenceResponse} record with IN_PROGRESS status
     * and a generated UUID request ID.
     *
     * @return the saved InferenceResponse entity
     */
    @Override
    public InferenceResponse createInference()
    {
        Long id = inferenceResponseRepository.getNextInferenceResponseId();
        if (id == null)
            throw new InferenceException(ApplicationConstants.INFERENCE_PIPELINE_ERROR_CODE,
                "Failed to generate inference response ID.");

        InferenceResponse infr = new InferenceResponse();
        infr.setCreateTs(getCurrentEasternTime());
        infr.setStatusCd("IN_PROGRESS");
        infr.setCreatedBy("INFERENCE_API");
        String rqId = UUID.randomUUID().toString();
        infr.setRequestId(rqId);
        infr.setInferenceResponseId(id);
        return inferenceResponseRepository.save(infr);
    }

    private Date getCurrentEasternTime()
    {
        Instant instant = Instant.now().atZone(ZoneId.of("America/New_York")).toInstant();
        return Date.from(instant);
    }

    @AllArgsConstructor
    public class InferenceResponseCompletionTask implements Runnable
    {
        List<CompletableFuture<Void>> subtasks;
        Long inferenceResponseId;
        FailureCount failures;

        @Override
        public void run()
        {
            int size = subtasks.size();
            long completed = subtasks.stream().filter(st -> st.isDone()).count();
            Long startTime = System.currentTimeMillis();

            while (completed < size && (System.currentTimeMillis() - startTime < maxWaitTime))
            {
                try
                {
                    Thread.sleep(5000);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    log.error("{}: Completion task interrupted for inferenceResponseId: {}",
                        ApplicationConstants.INFERENCE_PIPELINE_ERROR_CODE, inferenceResponseId);
                }

                completed = subtasks.stream().filter(st -> st.isDone()).count();
            }
            long errors = subtasks.stream().filter(st -> st.isCompletedExceptionally()).count();
            InferenceResponse ir = inferenceResponseRepository.findById(inferenceResponseId).orElse(null);
            if (ir == null)
                return;

            String status = (errors >= completed || failures.getFailures() >= completed) ? "FAILED" : "SUCCESS";
            ir.setUpdateTs(getCurrentEasternTime());
            ir.setUpdatedBy("INFERENCE_API");
            ir.setStatusCd(status);
            inferenceResponseRepository.save(ir);
        }
    }

    @AllArgsConstructor
    public class PostConverseFunction implements Consumer<ConverseResponse>
    {
        private String query;
        private Long inferenceResponseId;
        private String mimeType;
        private String questionKey;
        private String textractJson;

        @Override
        public void accept(ConverseResponse c)
        {
            InferenceAnswer ans = new InferenceAnswer();
            if (c != null)
            {
                String converseResponseJson = "";
                if ("application/json".equalsIgnoreCase(mimeType))
                {
                	converseResponseJson = c.output().message().content().get(0).text().replace("```json", "").replace("```", "");
                }
                else
                {
                	converseResponseJson = c.output().message().content().get(0).text();
                }
                ans.setAnswer(mergeresponses(converseResponseJson, textractJson));
                ans.setInputTokenCount(c.usage().inputTokens());
                ans.setOutputTokenCount(c.usage().outputTokens());
            }
            ans.setInferenceResponseId(inferenceResponseId);
            ans.setCreateTs(getCurrentEasternTime());
            ans.setMimeType(mimeType);
            ans.setQuery(query);
            ans.setQuestionKey(questionKey);
            ans.setCreatedBy("INFERENCE_API");
            answerRepository.save(ans);
        }

        private String mergeresponses(String converseResponseJson, String textractJsonStr) {
        	if (StringUtils.isBlank(textractJsonStr)) {
        		log.debug("Textract JSON Response is empty. Nothing to merge.");
        		return converseResponseJson;
        	}
        	
        	ObjectMapper objectMapper = new ObjectMapper();

        	String mergedJsonString = converseResponseJson;
            // Parse both JSON strings into JsonNode objects
			try {
				JsonNode firstJsonNode = objectMapper.readTree(converseResponseJson);
				JsonNode secondJsonNode = objectMapper.readTree(textractJsonStr);
				
				// Merge the second JSON into the first
				JsonNode mergedNode = merge(firstJsonNode, secondJsonNode);
				
				// Print the result to the console
				mergedJsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mergedNode);
			} catch (Exception e) {
				log.debug("Error merging responses. Returning response from Bedrock.");
			} 
        	return mergedJsonString;
        }

        public static JsonNode merge(JsonNode mainNode, JsonNode sourceNode) {
            Iterator<String> fieldNames = sourceNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode jsonNode = mainNode.get(fieldName);
                JsonNode sourceValue = sourceNode.get(fieldName);

                // If the field exists in both and is an embedded object, merge recursively.
                if (jsonNode != null && jsonNode.isObject() && sourceValue.isObject()) {
                    merge(jsonNode, sourceValue);
                } else if (mainNode instanceof ObjectNode) {
                    // For other types (primitives, arrays, etc.), overwrite the field.
                    ObjectNode objectNode = (ObjectNode) mainNode;
                    objectNode.replace(fieldName, sourceValue);
                }
            }
            return mainNode;
        }

    }
    
	@AllArgsConstructor
	public class PostTextractFunction implements Consumer<String> {

		private final String query;
		private final Long inferenceResponseId;
		private final String mimeType;
		private final String questionKey;

		@Override
		public void accept(String content) {

			InferenceAnswer ans = new InferenceAnswer();

			if (content != null) {

				String cleaned = content.trim();

				if ("application/json".equalsIgnoreCase(mimeType)) {
					cleaned = cleaned.replace("```json", "").replace("```", "").trim();
				}

				ans.setAnswer(cleaned);

				// Only set token counts if available
				ans.setInputTokenCount(0);
				ans.setOutputTokenCount(0);
			}

			ans.setInferenceResponseId(inferenceResponseId);
			ans.setCreateTs(getCurrentEasternTime());
			ans.setMimeType(mimeType);
			ans.setQuery(query);
			ans.setQuestionKey(questionKey);
			ans.setCreatedBy("INFERENCE_API");
			answerRepository.save(ans);
		}
	}
	

    /**
     * Retrieves the raw Textract OCR result for the given request ID.
     *
     * @param requestId the unique request ID
     * @return the inference response containing raw Textract JSON, or UNKNOWN status if not found
     * @throws BadDataException if requestId is blank
     */
    @Override
    public InferenceRestResponse getOcrInferenceResult(String requestId)
    {
    	  if (StringUtils.isBlank(requestId))
          {
              throw new BadDataException(ApplicationConstants.BLANK_REQUEST_ID_ERROR_CODE, ApplicationConstants.BLANK_REQUEST_ID_ERROR_MSG);
          }

    	  PdfTextExtractJson ir = this.textExtractionService.pdfTextExtractRepository.findByRequestId(requestId);
    	  
    	  if (ir == null)
          {
              return new InferenceRestResponse("UNKNOWN", requestId, new ArrayList<>(),null);
          }
    	  
          InferenceRestResponse irr = new InferenceRestResponse();
          irr.setRequestId(requestId);
          irr.setRawJson(ir.getRawJson());

          return irr;
    }


    /**
     * Retrieves the full inference result including all Bedrock answers for the given request ID.
     *
     * @param requestId the unique request ID
     * @return the inference response with status and list of answers, or UNKNOWN status if not found
     * @throws BadDataException if requestId is blank
     */
    @Override
    public InferenceRestResponse getInferenceResult(String requestId)
    {
        if (StringUtils.isBlank(requestId))
        {
            throw new BadDataException(ApplicationConstants.BLANK_REQUEST_ID_ERROR_CODE, ApplicationConstants.BLANK_REQUEST_ID_ERROR_MSG);
        }

        InferenceResponse ir = this.inferenceResponseRepository.findByRequestId(requestId);

        if (ir == null)
        {
            return new InferenceRestResponse("UNKNOWN", requestId, new ArrayList<>(),null);
        }

        InferenceRestResponse irr = new InferenceRestResponse();
        irr.setRequestId(requestId);
        irr.setStatus(ir.getStatusCd());
        irr.setInferenceAnswers(new ArrayList<>());

        List<InferenceAnswer> ans = this.answerRepository.findAllByInferenceResponseId(ir.getInferenceResponseId());

        if (ans == null)
            return irr;

        for (InferenceAnswer ia : ans)
        {
            irr.getInferenceAnswers().add(new InferenceRestAnswer(ia.getQuery(), ia.getAnswer(), ia.getQuestionKey(), ia.getMimeType()));
        }

        return irr;
    }
}