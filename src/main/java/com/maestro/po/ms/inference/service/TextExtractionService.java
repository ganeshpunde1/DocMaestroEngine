package com.maestro.po.ms.inference.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.maestro.po.ms.inference.model.annotation.InferenceResponse;
import com.maestro.po.ms.inference.model.annotation.PdfTextExtractJson;
import com.maestro.po.ms.inference.model.annotation.TextractJobContext;
import com.maestro.po.ms.inference.model.interfaces.S3InferenceRequest;
import com.maestro.po.ms.inference.model.rest.BoundingBox;
import com.maestro.po.ms.inference.model.rest.FieldMatchingRule;
import com.maestro.po.ms.inference.model.rest.ExtractionField;
import com.maestro.po.ms.inference.model.rest.ExtractionInfo;
import com.maestro.po.ms.inference.repository.PdfTextExtractRepository;
import com.maestro.po.ms.inference.util.TextractJsonSerializer;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.Block;
import software.amazon.awssdk.services.textract.model.DocumentLocation;
import software.amazon.awssdk.services.textract.model.GetDocumentTextDetectionRequest;
import software.amazon.awssdk.services.textract.model.GetDocumentTextDetectionResponse;
import software.amazon.awssdk.services.textract.model.S3Object;
import software.amazon.awssdk.services.textract.model.StartDocumentTextDetectionRequest;
import software.amazon.awssdk.services.textract.model.StartDocumentTextDetectionResponse;

/**
 * Orchestrates the full AWS Textract document extraction workflow.
 * Uploads the PDF to S3, starts a Textract text detection job, polls for completion,
 * maps extracted blocks to configured output fields, and persists the result.
 *
 * @author Ganesh Punde
 */
@Slf4j
@Component
@Configuration
public class TextExtractionService
{
    @Autowired
    TextractClient textractClient;

    @Autowired
    S3Client s3Client;

    @Autowired
    PdfTextExtractRepository pdfTextExtractRepository;

    @Autowired
    TextractThrottleService textractThrottleService;

    @Value(value = "${maestro.inference.config.idpS3Bucket}")
    private String idpS3Bucket;

    @Value(value = "${maestro.inference.config.textractCompletedJobStatus}")
    private List<String> completedJobStatuses;

    @Value(value = "${maestro.inference.config.textractSleepInterval:20000}")
    private Integer jobStatusSleepInterval;
    
    @Value(value = "${maestro.inference.config.textractShortSleepInterval:5000}")
    private Integer jobStatusShortSleepInterval;

    /**
     * Runs the full Textract extraction pipeline for the given request.
     * Uploads the document to S3, runs the Textract job, maps fields, and returns the result JSON.
     *
     * @param s3ir the S3 inference request containing the document content
     * @param info the extraction configuration specifying which fields to extract
     * @param ir   the inference tracking record
     * @return the extracted fields as a JSON string, or empty string on failure
     */
    public String extractJson(S3InferenceRequest s3ir, ExtractionInfo info, InferenceResponse ir)
    {
        if (!uploadDocumentToS3(s3ir, ir))
        {
            log.error("Error in uploading document to S3, aborting textract call for requestId: " + ir.getRequestId());
            return "";
        }

        TextractJobContext txc = runTextractJob(ir);

        if (CollectionUtils.isEmpty(txc.getSortedBlocks()))
        {
            log.error("Nothing extracted from Textract for requestId: " + ir.getRequestId());
            return "";
        }

        log.debug("Building json paramters for requestId: " + ir.getRequestId());
        mapTextractBlocksToOutputFields(info, txc);

        serializeOutputMapToJson(txc);

        saveTextractResult(txc);

        return txc.getResultJson();
    }

    private void saveTextractResult(TextractJobContext txc)
    {
        if (txc == null || StringUtils.isBlank(txc.getResultJson()) || StringUtils.isBlank(txc.getRequestId()))
            return;

        PdfTextExtractJson extractJson = new PdfTextExtractJson();
        extractJson.setCreateTs(Date.from(Instant.now()));
        extractJson.setCreatedBy("INFERENCE_API");
        extractJson.setJson(txc.getResultJson());
        extractJson.setRequestId(txc.getRequestId());
        extractJson.setRawJson(TextractJsonSerializer.convertTextractOutputToJson(txc.getAllBlocks()));
        pdfTextExtractRepository.save(extractJson);
    }

    private void serializeOutputMapToJson(TextractJobContext txc)
    {
        String json = "";

        if (txc == null || CollectionUtils.isEmpty(txc.getTextractOutput()))
            return;

        try
        {
            txc.getTextractOutput().put("requestId", txc.getRequestId());
            json = TextractJsonSerializer.generateJsonFromMap(txc.getTextractOutput());
        }
        catch (Exception e)
        {
            log.error("Could not transform output to json string for requestId : " + txc.getRequestId());
        }

        txc.setResultJson(json);
    }

    private TextractJobContext runTextractJob(InferenceResponse ir)
    {
        log.debug("Starting Textract call for requestId: " + ir.getRequestId());
        S3Object s3Object = S3Object.builder().name(ir.getRequestId()).bucket(idpS3Bucket).build();

        DocumentLocation dl = DocumentLocation.builder().s3Object(s3Object).build();

        StartDocumentTextDetectionRequest sdtd = StartDocumentTextDetectionRequest.builder().documentLocation(dl).build();

        StartDocumentTextDetectionResponse rx = startTextractTextDetection(sdtd);

        String jobId = rx.jobId();
        boolean finished = false;
        GetDocumentTextDetectionResponse response = null;

        GetDocumentTextDetectionRequest textDetectionRequest = GetDocumentTextDetectionRequest.builder().jobId(jobId).maxResults(1000).build();
        List<Block> blockStore = new ArrayList<Block>();
        this.textractThrottleService.safeSleep(jobStatusSleepInterval);

        while (!finished)
        {
            response = getTextractTextDetection(textDetectionRequest);
            String status = response.jobStatus().toString();

            if (jobCompleted(status))
            {
                finished = true;
                if (response.hasBlocks())
                    blockStore.addAll(response.blocks());
                while (StringUtils.isNotBlank(response.nextToken()))
                {
                    textDetectionRequest = textDetectionRequest.toBuilder().nextToken(response.nextToken()).build();
                    response = getTextractTextDetection(textDetectionRequest);
                    if (response.hasBlocks())
                        blockStore.addAll(response.blocks());
                }
                log.info("Textact jobId: " + jobId + " - COMPLETE");
            }
            else
            {
                log.info("Waiting on Textract jobId: " + jobId + " for requestId: " + ir.getRequestId() + " waiting an additional " + jobStatusShortSleepInterval); 
                this.textractThrottleService.safeSleep(jobStatusShortSleepInterval);
            }
        }

        TextractJobContext txec = new TextractJobContext();
        txec.setRequestId(ir.getRequestId());
        Map<String, List<Block>> mapList = new HashMap<String, List<Block>>();
        for (Block bx : blockStore)
        {
            if (bx == null)
                continue;

            List<Block> listing = mapList.get(bx.blockTypeAsString());
            if (listing == null)
            {
                listing = new ArrayList<Block>();
                mapList.put(bx.blockTypeAsString(), listing);
            }
            listing.add(bx);
        }
        txec.setSortedBlocks(mapList);
        txec.setJobId(jobId);
        txec.setAllBlocks(blockStore);

        return txec;
    }

    private GetDocumentTextDetectionResponse getTextractTextDetection(GetDocumentTextDetectionRequest textDetectionRequest)
    {
        this.textractThrottleService.throttleGetDocumentTextDetection();;
        GetDocumentTextDetectionResponse response = textractClient.getDocumentTextDetection(textDetectionRequest);
        return response;
    }

    private StartDocumentTextDetectionResponse startTextractTextDetection(StartDocumentTextDetectionRequest sdtd)
    {
        this.textractThrottleService.throttleStartDocumentTextDetection();
        StartDocumentTextDetectionResponse rx = textractClient.startDocumentTextDetection(sdtd);
        return rx;
    }

    private boolean jobCompleted(String status)
    {
        return this.completedJobStatuses.contains(status);
    }

    private void mapTextractBlocksToOutputFields(ExtractionInfo info, TextractJobContext txc)
    {
        if (txc == null || CollectionUtils.isEmpty(txc.getSortedBlocks()) || info == null || CollectionUtils.isEmpty(info.getExtractionFields()))
        {
            return;
        }

        for (ExtractionField field : info.getExtractionFields())
        {
            List<Block> narrowedList = txc.getSortedBlocks().get("LINE");

            if (field.getLowerPage() != null && field.getUpperPage() != null)
            {
                narrowedList = filterBlocksByPageRange(field.getLowerPage(), field.getUpperPage(), narrowedList);
            }
            else if (field.getLowerPage() != null)
            {
                narrowedList = filterBlocksByPageRange(field.getLowerPage(), Integer.MAX_VALUE, narrowedList);
            }
            else if (field.getUpperPage() != null)
            {
                narrowedList = filterBlocksByPageRange(0, field.getUpperPage(), narrowedList);
            }
            
            narrowedList = filterBlocksByBoundingBox(narrowedList, field.getBoundingBox());

            narrowedList = truncateBySubsequentFieldName(field, narrowedList);

            String value = resolveFieldValueFromBlocks(narrowedList, field.getEnumTrait());
            txc.getTextractOutput().put(field.getOutputFieldName(), value);
        }
    }
    
	private List<Block> truncateBySubsequentFieldName(ExtractionField field, List<Block> blocks) {
		if (field == null || field.getSubsequentFieldName() == null || CollectionUtils.isEmpty(blocks)) {
			return blocks;
		}

		List<Block> rtrnVal = new ArrayList<>();

		boolean blockMatchedForField = false;
		boolean subsequentFieldNameBlock = false;
		for (Block bx : blocks) {
			if (blockMatchedForField) {
				if (bx.text() != null && bx.text().indexOf(field.getSubsequentFieldName()) != -1) {
					subsequentFieldNameBlock = true;
					break;
				} else {
					rtrnVal.add(bx);
				}
			} else if (StringUtils.isNotBlank(field.getInputFieldName()) && StringUtils.isNotBlank(bx.text())
					&& (bx.text().indexOf(field.getInputFieldName()) != -1
							|| isMatch(field.getInputFieldName(), bx.text()))) {
				blockMatchedForField = true;
				continue;
			}
		}
		if (!subsequentFieldNameBlock && rtrnVal != null && rtrnVal.size() > 0) {
			Block block = rtrnVal.get(0);
			rtrnVal = new ArrayList<>();
			rtrnVal.add(block);
		}
		return rtrnVal;
	}

	/**
	 * Method to match two strings (Fuzzy match)
	 *
	 * Example:
	 *
	 * String s1 = "Physical therapy : # of days:"; String s2 = "Physical therapy #
	 * of days:";
	 * 
	 * ASSUMPTION: The above two strings are considered same.
	 * 
	 * S1 --> String to be matched --> InputFieldName S2 --> Value extracted by AWS
	 * Textract
	 * 
	 * Even though the document has : (colon) in the UAS CHA Document label, AWS
	 * Textract is not extracting the : (colon) sometimes
	 * 
	 * As a result of this method, we were able to successfully match S1 and S2 to
	 * be the same.
	 *
	 * @param s1 - Inputfield string
	 * @param s2 - AWS Textract extracted string
	 * @return
	 */
	private boolean isMatch(String s1, String s2) {
	    if (s1 == null || s2 == null) return false;
	    return normalize(s1).equals(normalize(s2));
	}

	private String normalize(String s) {
	    return s.replaceAll("[^a-zA-Z0-9#]", "").toLowerCase(Locale.ROOT);
	}
    
    private List<Block> filterBlocksByBoundingBox(List<Block> lines, BoundingBox boundingBox)
    {
        if (CollectionUtils.isEmpty(lines) || boundingBox == null)
        {
            return lines;
        }

        List<Block> rtrnVal = new ArrayList<>();

        for (Block bx : lines)
        {
            if (bx == null || bx.geometry() == null || bx.geometry().boundingBox() == null)
                continue;

            Float boxTop = bx.geometry().boundingBox().top();
            Float boxLeft = bx.geometry().boundingBox().left();

            Float bbHeader = boundingBox.getHeaderThreshold();
            Float bbFooter = boundingBox.getFooterThreshold();
            Float bbColmun = boundingBox.getColumnThreshold();

            if (bbHeader != null && bbHeader >= boxTop)
                continue;
            if (bbFooter != null && bbFooter <= boxTop)
                continue;
            if (bbColmun != null && bbColmun >= boxLeft)
                continue;

            rtrnVal.add(bx);
        }

        return rtrnVal;
    }

    private String resolveFieldValueFromBlocks(List<Block> boundBoxCheck, FieldMatchingRule enumTrait)
    {
        if (CollectionUtils.isEmpty(boundBoxCheck))
        {
            return "";
        }

        if (enumTrait != null && !CollectionUtils.isEmpty(enumTrait.getAnswers()))
        {
            String rtVal = "";
            String completeText = boundBoxCheck.stream().filter(b -> !StringUtils.isBlank(b.text())).map(v -> v.text()).collect(Collectors.joining(" "));
            double currentScore = 0.0;

            for (String potentialAnswer : enumTrait.getAnswers())
            {
                if (StringUtils.equalsIgnoreCase(completeText, potentialAnswer))
                {
                    return potentialAnswer;
                }

                Double score = computeLevenshteinSimilarity(completeText, potentialAnswer);

                if (score > currentScore || (score == currentScore && (potentialAnswer.length() > rtVal.length())))
                {
                    currentScore = score;
                    rtVal = potentialAnswer;
                }
            }

            return rtVal;
        }

        if (enumTrait != null && StringUtils.isNotBlank(enumTrait.getRegex()))
        {
            String completeText = boundBoxCheck.stream().filter(b -> !StringUtils.isBlank(b.text())).map(v -> v.text()).collect(Collectors.joining(" "));
            String regex = enumTrait.getRegex();

            String rtVal = findLongestMatchingSubstring(completeText, regex);
            return rtVal;

        }

        return boundBoxCheck.stream().filter(b -> !StringUtils.isBlank(b.text())).map(v -> v.text()).collect(Collectors.joining(" "));
    }

    private String findLongestMatchingSubstring(String text, String regex)
    {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        String longestMatch = null;
        int maxLength = 0;

        while (matcher.find())
        {
            String currentMatch = matcher.group();
            if (currentMatch.length() > maxLength)
            {
                maxLength = currentMatch.length();
                longestMatch = currentMatch;
            }
        }
        return longestMatch;
    }

    private List<Block> filterBlocksByPageRange(int firstPage, int lastPage, List<Block> blocks)
    {
        if (CollectionUtils.isEmpty(blocks) || firstPage > lastPage || firstPage < 0 || lastPage < 0)
        {
            return blocks;
        }

        List<Block> rtrnVal = new ArrayList<>();

        for (Block b : blocks)
        {
            if (b.page() == null)
                continue;

            if (b.page() >= firstPage && b.page() <= lastPage)
            {
                rtrnVal.add(b);
            }
        }

        return rtrnVal;

    }

    private double computeLevenshteinSimilarity(String searchText, String searchString)
    {
        double score = 0;

        if (StringUtils.isBlank(searchText) || StringUtils.isBlank(searchString))
        {
            return score;
        }

        Integer distance = LevenshteinDistance.getDefaultInstance().apply(searchText, searchString);

        if (distance < 0)
            return score;

        int maxDistance = Math.max(searchText.length(), searchString.length());

        score = 1.0 - (distance * 1.0 / maxDistance);

        return score;
    }

    private boolean uploadDocumentToS3(S3InferenceRequest pr, InferenceResponse ir)
    {
        if (pr == null || StringUtils.isBlank(pr.s3Content()))
        {
            return false;
        }

        try
        {

            log.debug("Initiating document upload for requestId: " + ir.getRequestId());
            Long startTime = System.currentTimeMillis();
            Map<String, String> s3Metadata = getMetadata(pr, ir);
            PutObjectRequest s3Request = PutObjectRequest.builder().bucket(idpS3Bucket).key(ir.getRequestId()).metadata(s3Metadata).build();

            byte[] bytes = Base64.getDecoder().decode(pr.s3Content());
            RequestBody rb = RequestBody.fromBytes(bytes);

            s3Client.putObject(s3Request, rb).responseMetadata();

            Tagging tags = Tagging.builder().tagSet(getTags(pr)).build();
            PutObjectTaggingRequest potr = PutObjectTaggingRequest.builder().bucket(s3Request.bucket()).key(s3Request.key()).tagging(tags).build();

            s3Client.putObjectTagging(potr);

            Long endTime = System.currentTimeMillis();
            log.debug("uploadDocumentToS3 Operation took " + (endTime - startTime) + " milliseconds for requestId: " + ir.getRequestId());
        }
        catch (Exception e)
        {
            log.error("Upload for requestId " + ir.getRequestId() + " failed due to error: " + e);
            return false;
        }

        return true;

    }

    private Map<String, String> getMetadata(S3InferenceRequest pr, InferenceResponse ir)
    {
        log.debug("Building S3 metadata for requestId: " + ir.getRequestId());
        Map<String, String> metaData = new HashMap<>();
        if (pr != null && !CollectionUtils.isEmpty(pr.metadata()))
        {
            metaData.putAll(pr.metadata());
        }
        metaData.put("requestId", ir.getRequestId());
        return metaData;
    }

    private List<Tag> getTags(S3InferenceRequest pr)
    {
        List<Tag> meta = new ArrayList<>();
        if (pr != null && !CollectionUtils.isEmpty(pr.tags().keySet()))
        {
            for (String entry : pr.tags().keySet())
                meta.add(Tag.builder().key(entry).value(pr.tags().get(entry)).build());
        }
        return meta;
    }

}