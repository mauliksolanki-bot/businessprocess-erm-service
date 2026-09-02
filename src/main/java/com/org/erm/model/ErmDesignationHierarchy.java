package com.org.erm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ERM_DESIGNATION_HIERARCHY")
public class ErmDesignationHierarchy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "DESIGNATION_ROLE_NAME", nullable = false, length = 100)
    private String designationRoleName;

    @Column(name = "REPORTS_TO_ROLE_NAME", nullable = false, length = 100)
    private String reportsToRoleName;

    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "IS_ACTIVE", nullable = false)
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public String getDesignationRoleName() {
        return designationRoleName;
    }

    public void setDesignationRoleName(String designationRoleName) {
        this.designationRoleName = designationRoleName;
    }

    public String getReportsToRoleName() {
        return reportsToRoleName;
    }

    public void setReportsToRoleName(String reportsToRoleName) {
        this.reportsToRoleName = reportsToRoleName;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
