package com.maestro.po.ms.inference.model.annotation;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;

@Data
@MappedSuperclass
public class EnumParentEntity
{
    @Id
    @Column(name = "VALUE")
    private String value;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATE_TS")
    private Date createTs;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "DESCRIPTION")
    private String description;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "EFFECTIVE_END_DATE")
    private Date effectiveEndDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "EFFECTIVE_START_DATE")
    private Date effectiveStartDate;

    @Column(name = "ORDER_BY_DEFAULT")
    private Integer orderByDefault;

    @Column(name = "REPORT_LABEL")
    private String reportLabel;

    @Column(name = "SCOPE")
    private String scope;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "UPDATE_TS")
    private Date updateTs;

    @Column(name = "UPDATED_BY")
    private String updatedBy;
}
