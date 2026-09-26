package com.org.erm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ERM_EMPLOYEE_ID_ROLE_FORMATS")
public class ErmEmployeeIdRoleFormat {

    @Id
    @Column(name = "ROLE_NAME", length = 100)
    private String roleName;

    @Column(name = "ID_PREFIX", nullable = false, length = 20)
    private String idPrefix;

    @Column(name = "SEQUENCE_ENABLED", nullable = false)
    private boolean sequenceEnabled;

    @Column(name = "PRIORITY_ORDER", nullable = false)
    private int priorityOrder;

    public String getRoleName() {
        return roleName;
    }

    public String getIdPrefix() {
        return idPrefix;
    }

    public boolean isSequenceEnabled() {
        return sequenceEnabled;
    }

    public int getPriorityOrder() {
        return priorityOrder;
    }
}
