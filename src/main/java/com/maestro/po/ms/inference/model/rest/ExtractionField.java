package com.maestro.po.ms.inference.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class ExtractionField
{
    @JsonProperty("input_field_name")
    @NotBlank(message="input_field_name cannot be blank")
    @NotNull(message="input_field_name cannot be null")
    private String inputFieldName;
    
    @JsonProperty("output_field_name")
    @NotBlank(message="output_field_name cannot be blank")
    @NotNull(message="output_field_name cannot be null")
    private String outputFieldName;

    @JsonProperty("lower_page")
    @PositiveOrZero(message="lower_page cannot be negative")
    private Integer lowerPage;
    
    @JsonProperty("upper_page")
    @PositiveOrZero(message="upper_page cannot be negative")
    private Integer upperPage;
    
    @JsonProperty("preceding_field_label")
    private String precedingFieldName;
    
    @JsonProperty("subsequent_field_label")
    private String subsequentFieldName;
    
    @JsonProperty("bounding_box")
    private BoundingBox boundingBox;
    
    @JsonProperty("enum_traits")
    @Valid
    private FieldMatchingRule enumTrait;

}
