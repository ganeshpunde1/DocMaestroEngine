package com.maestro.po.ms.inference.model.annotation;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


/**
 * The persistent class for the ENUM_INFERENCE_ANSWER_MIME_TYPE database table.
 * 
 */
@Entity
@Table(name="ENUM_INFERENCE_ANSWER_MIME_TYPE")
@NamedQuery(name="EnumInferenceAnswerMimeType.findAll", query="SELECT e FROM EnumInferenceAnswerMimeType e")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class EnumInferenceAnswerMimeType extends EnumParentEntity implements Serializable {
	private static final long serialVersionUID = 1L;


}