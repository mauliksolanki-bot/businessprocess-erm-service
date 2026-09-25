package com.org.erm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ERM_EMPLOYEE_ID_SEQUENCES")
public class ErmEmployeeIdSequence {

    @Id
    @Column(name = "DESIGNATION_CODE", length = 20)
    private String designationCode;

    @Column(name = "LAST_SEQUENCE", nullable = false)
    private Integer lastSequence = 0;

    public String getDesignationCode() {
        return designationCode;
    }

    public void setDesignationCode(String designationCode) {
        this.designationCode = designationCode;
    }

    public Integer getLastSequence() {
        return lastSequence;
    }

    public void setLastSequence(Integer lastSequence) {
        this.lastSequence = lastSequence;
    }
}
