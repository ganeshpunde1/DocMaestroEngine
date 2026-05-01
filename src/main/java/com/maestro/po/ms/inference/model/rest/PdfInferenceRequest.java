package com.maestro.po.ms.inference.model.rest;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.maestro.po.ms.inference.annotations.Base64Check;
import com.maestro.po.ms.inference.annotations.Base64SizeCheck;
import com.maestro.po.ms.inference.annotations.PropertyValueCheck;
import com.maestro.po.ms.inference.model.interfaces.S3InferenceRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PdfInferenceRequest implements S3InferenceRequest
{
    @JsonProperty("pdf_data")
    @NotBlank(message="pdf_data cannot be blank")
    @Base64Check(message="pdf_data is not properly base64 encoded")
    @Base64SizeCheck(message="pdf_data is too large", maxSizeProperty ="maestro.gp.po.size_limit")
    private String pdfData;
    
    @JsonProperty("model_id")
    @PropertyValueCheck(property="maestro.gp.po.models", message="No corresponding model is enabled")
    @NotBlank
    @NotNull
    private String modelId;
    
    @JsonProperty("queries")
    @NotNull(message="queries cannot be null")
    @Valid
    List<InferenceQuery> queries;
    
    @JsonProperty("generic_info")
    List<GenericInfo> genericInfo;
    
    @Override
    public Map<String, String> tags()
    {
        return Map.of("AI_MODEL", modelId);
    }

    @Override
    public String s3Content()
    {
        return this.pdfData;
    }
}
