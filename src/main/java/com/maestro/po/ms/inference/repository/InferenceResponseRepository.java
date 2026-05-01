package com.maestro.po.ms.inference.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.maestro.po.ms.inference.model.annotation.InferenceResponse;

public interface InferenceResponseRepository extends JpaRepository<InferenceResponse, Long>
{
    @Query(value = "SELECT SEQ_INFERENCE_RESPONSE_ID.nextval from DUAL", nativeQuery = true)
    public Long getNextInferenceResponseId();
    
    public InferenceResponse findByRequestId(String requestId);
}
