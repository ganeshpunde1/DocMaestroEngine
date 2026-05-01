package com.maestro.po.ms.inference.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class BoundingBox
{
    @JsonProperty("column_threshold")
    @PositiveOrZero(message="column_threshold cannot be negative")
    @DecimalMax(value = "1.0", message="column_threshold cannot be greater than 1.0")
    private Float columnThreshold;

    @JsonProperty("header_threshold")
    @PositiveOrZero(message="header_threshold cannot be negative")
    @DecimalMax(value = "1.0",message="header_threshold cannot be greater than 1.0")
    private Float headerThreshold;

    @JsonProperty("footer_threshold")
    @PositiveOrZero(message="footer_threshold cannot be negative")
    @DecimalMax(value = "1.0",message="footer_threshold cannot be greater than 1.0")
    private Float footerThreshold;

    @JsonProperty("indent_threshold")
    @PositiveOrZero(message="indent_threshold cannot be negative")
    @DecimalMax(value = "1.0",message="indent_threshold cannot be greater than 1.0")
    private Float indentThreshold;

    @JsonProperty("height_tolerance")
    @PositiveOrZero(message="height_tolerance cannot be negative")
    @DecimalMax(value = "1.0",message="height_tolerance cannot be greater than 1.0")
    private Float heightTolerance;
}
