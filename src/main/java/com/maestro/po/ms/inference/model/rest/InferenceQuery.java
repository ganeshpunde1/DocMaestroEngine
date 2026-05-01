package com.maestro.po.ms.inference.model.rest;

import java.math.BigDecimal;

import org.hibernate.validator.constraints.Length;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.maestro.po.ms.inference.annotations.EnumValueCheck;
import com.maestro.po.ms.inference.repository.EnumInferenceAnswerMimeTypeRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class InferenceQuery
{
    @JsonProperty("max_tokens")
    @PositiveOrZero(message="max_tokens cannot be negative")
    private Integer maxTokens;
    
    @JsonProperty("temperature")
    @PositiveOrZero(message="temperature cannot be negative")
    @Max(value= 1l, message="temperature must be between 0 and 1.")
    private BigDecimal temperature;
    
    @JsonProperty("top_p")
    @NotNull(message="top_p cannot be null")
    @PositiveOrZero(message="top_p cannot be negative")
    @Max(value= 1l, message="top_p must be between 0 and 1.")
    private BigDecimal topP;
    
    @JsonProperty("question")
    @NotBlank(message="question cannot be blank")
    private String question;
    
    @JsonProperty("question_key")
    @Length(max=256, message="question_key cannot be longer than 256 characters")
    private String questionKey;
    
    @JsonProperty("mime_type")
    @EnumValueCheck(enumRepository=EnumInferenceAnswerMimeTypeRepository.class)
    private String mimeType;
    
    @JsonProperty("extract_info")
    @Valid
    private ExtractionInfo extractionInfo;
    
    @JsonProperty("textract_only")
    @Valid
    private boolean isTextractOnly;


}
