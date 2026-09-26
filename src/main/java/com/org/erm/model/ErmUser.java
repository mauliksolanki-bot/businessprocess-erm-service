package com.org.erm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ERM_USERS")
public class ErmUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USERNAME", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "EMPLOYEE_ID", unique = true, length = 50)
    private String employeeId;

    @Column(name = "EMAIL", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "FULL_NAME", nullable = false, length = 150)
    private String fullName;

    @Column(name = "DEPARTMENT", nullable = false, length = 100)
    private String department;

    @Column(name = "EMPLOYMENT_STATUS", nullable = false, length = 30)
    private String employmentStatus;

    @Column(name = "REPORTING_MANAGER_USER_ID")
    private Long reportingManagerUserId;

    @Column(name = "REPORTING_MANAGER_EMPLOYEE_ID", length = 50)
    private String reportingManagerEmployeeId;

    @Column(name = "REPORTING_MANAGER_ROLE_NAME", length = 100)
    private String reportingManagerRoleName;

    @Column(name = "PRIMARY_ROLE_ID")
    private Long primaryRoleId;

    @Column(name = "PASSWORD_HASH", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "PERSONAL_EMAIL_ADDRESS", length = 150)
    private String personalEmailAddress;

    @Column(name = "PHONE_NUMBER", length = 25)
    private String phoneNumber;

    @Column(name = "EDUCATION_QUALIFICATION", length = 255)
    private String educationQualification;

    @Column(name = "IS_ACTIVE", nullable = false)
    private boolean active = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "ERM_USER_ROLES",
            joinColumns = @JoinColumn(name = "USER_ID"),
            inverseJoinColumns = @JoinColumn(name = "ROLE_ID")
    )
    private Set<ErmRole> roles = new HashSet<>();

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
    }

    public void setEmploymentStatus(String employmentStatus) {
        this.employmentStatus = employmentStatus;
    }

    public Long getReportingManagerUserId() {
        return reportingManagerUserId;
    }

    public void setReportingManagerUserId(Long reportingManagerUserId) {
        this.reportingManagerUserId = reportingManagerUserId;
    }

    public String getReportingManagerEmployeeId() {
        return reportingManagerEmployeeId;
    }

    public void setReportingManagerEmployeeId(String reportingManagerEmployeeId) {
        this.reportingManagerEmployeeId = reportingManagerEmployeeId;
    }

    public String getReportingManagerRoleName() {
        return reportingManagerRoleName;
    }

    public void setReportingManagerRoleName(String reportingManagerRoleName) {
        this.reportingManagerRoleName = reportingManagerRoleName;
    }

    public Long getPrimaryRoleId() {
        return primaryRoleId;
    }

    public void setPrimaryRoleId(Long primaryRoleId) {
        this.primaryRoleId = primaryRoleId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPersonalEmailAddress() {
        return personalEmailAddress;
    }

    public void setPersonalEmailAddress(String personalEmailAddress) {
        this.personalEmailAddress = personalEmailAddress;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEducationQualification() {
        return educationQualification;
    }

    public void setEducationQualification(String educationQualification) {
        this.educationQualification = educationQualification;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<ErmRole> getRoles() {
        return roles;
    }

    public void setRoles(Set<ErmRole> roles) {
        this.roles = roles;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
