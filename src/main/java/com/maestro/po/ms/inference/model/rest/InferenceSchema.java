package com.maestro.po.ms.inference.model.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InferenceSchema
{
    private String contentType;
    
    private String schema;

}
