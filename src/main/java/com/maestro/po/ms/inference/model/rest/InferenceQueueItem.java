package com.maestro.po.ms.inference.model.rest;

import com.maestro.po.ms.inference.model.annotation.InferenceResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InferenceQueueItem
{
    private PdfInferenceRequest inferenceRequest;
    private InferenceResponse inferenceResponse;
}
