package com.maestro.po.ms.inference.model.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AcceptedApiResponse
{
    @JsonProperty("message")
    private String message;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("request_id")
    private String requestId;

    @JsonInclude(Include.NON_NULL)
    @JsonProperty("error_code")
    private String errorCode;

    public AcceptedApiResponse(String message, Integer status, String requestId)
    {
        this.message = message;
        this.status = status;
        this.requestId = requestId;
    }
}
