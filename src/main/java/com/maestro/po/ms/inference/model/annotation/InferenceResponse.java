package com.maestro.po.ms.inference.model.annotation;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * The persistent class for the INFERENCE_RESPONSE database table.
 * 
 */
@Entity
@Table(name="INFERENCE_RESPONSE")
@NamedQuery(name="InferenceResponse.findAll", query="SELECT i FROM InferenceResponse i")
@Data
@NoArgsConstructor
public class InferenceResponse implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name="INFERENCE_RESPONSE_ID")
	private long inferenceResponseId;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="CREATE_TS")
	private Date createTs;

	@Column(name="CREATED_BY")
	private String createdBy;

	@Column(name="REQUEST_ID")
	private String requestId;

	@Column(name="STATUS_CD")
	private String statusCd;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="UPDATE_TS")
	private Date updateTs;

	@Column(name="UPDATED_BY")
	private String updatedBy;

}