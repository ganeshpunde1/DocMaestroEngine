package com.maestro.po.ms.inference.model.rest;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@JsonPropertyOrder({ "key", "value", "description" })
@JsonInclude(Include.NON_EMPTY)
public class GenericInfo
{
    @JsonProperty("key")
    private String key;

    @JsonProperty("value")
    private Object value;

    @JsonProperty("description")
    private String description;
}