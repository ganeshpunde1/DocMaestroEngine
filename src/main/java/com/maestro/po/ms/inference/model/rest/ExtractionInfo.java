package com.maestro.po.ms.inference.model.rest;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExtractionInfo
{
    @JsonProperty("extract_fields")
    @NotEmpty
    @NotNull
    @Valid
    private List<ExtractionField> extractionFields;

}
