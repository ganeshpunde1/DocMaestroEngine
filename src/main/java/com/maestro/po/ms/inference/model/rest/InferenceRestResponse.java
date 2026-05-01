package com.maestro.po.ms.inference.model.rest;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InferenceRestResponse
{
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("request_id")
    private String requestId;
    
    @JsonProperty("inferences")
    List<InferenceRestAnswer> inferenceAnswers;
    
    @JsonProperty("rawJson")
    private String rawJson;

}
