package com.maestro.po.ms.inference.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.maestro.po.ms.inference.model.annotation.InferenceResponse;
import com.maestro.po.ms.inference.model.annotation.PdfTextExtractJson;

public interface PdfTextExtractRepository extends JpaRepository<PdfTextExtractJson, Long>
{
    @Query(value = "SELECT SEQ_PDF_TEXT_EXTRACT_JSON_ID.nextval from DUAL", nativeQuery = true)
    public Long getNextPdfExtractId();
    
    public PdfTextExtractJson findByRequestId(String requestId);
}
