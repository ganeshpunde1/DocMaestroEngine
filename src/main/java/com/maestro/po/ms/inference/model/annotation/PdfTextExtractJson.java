package com.maestro.po.ms.inference.model.annotation;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;

/**
 * The persistent class for the PDF_TEXT_EXTRACT_JSON database table.
 * 
 */
@Entity
@Table(name="PDF_TEXT_EXTRACT_JSON")
@NamedQuery(name="PdfTextExtractJson.findAll", query="SELECT p FROM PdfTextExtractJson p")
@Data
public class PdfTextExtractJson implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(generator = "pdf_text_json_seq")
    @SequenceGenerator(name = "pdf_text_json_seq", sequenceName = "SEQ_PDF_TEXT_EXTRACT_JSON_ID", allocationSize = 1)
	@Column(name="PDF_TEXT_EXTRACT_JSON_ID")
	private long pdfTextExtractJsonId;

	@Temporal(TemporalType.DATE)
	@Column(name="CREATE_TS")
	private Date createTs;

	@Column(name="CREATED_BY")
	private String createdBy;

	@Lob
	@Column(name="JSON")
	private String json;
	
	@Lob
	@Column(name="RAW_JSON")
	private String rawJson;

	@Column(name="REQUEST_ID")
	private String requestId;

	@Temporal(TemporalType.DATE)
	@Column(name="UPDATE_TS")
	private Date updateTs;

	@Column(name="UPDATED_BY")
	private String updatedBy;
}