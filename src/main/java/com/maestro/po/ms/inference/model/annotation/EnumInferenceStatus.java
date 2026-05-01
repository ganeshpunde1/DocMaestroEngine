package com.maestro.po.ms.inference.model.annotation;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


/**
 * The persistent class for the ENUM_INFERENCE_STATUS database table.
 * 
 */
@Entity
@Table(name="ENUM_INFERENCE_STATUS")
@NamedQuery(name="EnumInferenceStatus.findAll", query="SELECT e FROM EnumInferenceStatus e")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class EnumInferenceStatus extends EnumParentEntity implements Serializable {
	private static final long serialVersionUID = 1L;


}