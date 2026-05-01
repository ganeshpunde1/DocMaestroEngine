package com.maestro.po.ms.inference.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.maestro.po.ms.inference.model.annotation.InferenceAnswer;

public interface InferenceAnswerRepository extends JpaRepository<InferenceAnswer, Long>
{
    List<InferenceAnswer> findAllByInferenceResponseId(Long inferenceResponseId);
    
    @Query(value= "SELECT SUM(INPUT_TOKEN_COUNT) + SUM(OUTPUT_TOKEN_COUNT) TPM FROM INFERENCE_ANSWER WHERE CREATE_TS >= (SYSDATE - (1 / (24 * 60)))", nativeQuery = true)
    Long calculateCurrentTPM();
}
