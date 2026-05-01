package com.maestro.po.ms.inference.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InferenceRestAnswer
{
    @JsonProperty("query")
    private String query;
    
    @JsonProperty("answer")
    private String answer;
    
    @JsonProperty("question_key")
    private String questionKey;
    
    @JsonProperty("mime_type")
    private String mimeType;
}
