package com.maestro.po.ms.inference.model.annotation;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * The persistent class for the INFERENCE_ANSWER database table.
 * 
 */
@Entity
@Table(name="INFERENCE_ANSWER")
@NamedQuery(name="InferenceAnswer.findAll", query="SELECT i FROM InferenceAnswer i")
@Data
@NoArgsConstructor
public class InferenceAnswer implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(generator = "inference_answer_seqg")
    @SequenceGenerator(name = "inference_answer_seqg", sequenceName = "SEQ_INFERENCE_ANSWER_ID", allocationSize = 1)
	@Column(name="INFERENCE_ANSWER_ID")
	private long inferenceAnswerId;

	@Lob
	@Column(name="ANSWER")
	private String answer;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="CREATE_TS")
	private Date createTs;

	@Column(name="CREATED_BY")
	private String createdBy;

	@Column(name="MIME_TYPE")
	private String mimeType;
	
	@Column(name="QUESTION_KEY")
    private String questionKey;

	@Lob
	@Column(name="QUERY")
	private String query;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="UPDATE_TS")
	private Date updateTs;

	@Column(name="UPDATED_BY")
	private String updatedBy;

	@Column(name="INFERENCE_RESPONSE_ID")
	private Long inferenceResponseId;
	
	@Column(name="INPUT_TOKEN_COUNT")
	private Integer inputTokenCount;
	
	@Column(name="OUTPUT_TOKEN_COUNT")
	private Integer outputTokenCount;

}