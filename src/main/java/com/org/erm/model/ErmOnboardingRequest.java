package com.org.erm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ERM_ONBOARDING_REQUESTS")
public class ErmOnboardingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "FIRST_NAME", nullable = false, length = 100)
    private String firstName;

    @Column(name = "LAST_NAME", nullable = false, length = 100)
    private String lastName;

    @Column(name = "AADHAAR_CARD_NUMBER", nullable = false, unique = true, length = 12)
    private String aadhaarCardNumber;

    @Column(name = "PAN_CARD_NUMBER", nullable = false, unique = true, length = 10)
    private String panCardNumber;

    @Column(name = "PERSONAL_EMAIL_ADDRESS", nullable = false, length = 150)
    private String personalEmailAddress;

    @Column(name = "PERMANENT_ADDRESS", nullable = false, length = 500)
    private String permanentAddress;

    @Column(name = "PHONE_NUMBER", nullable = false, length = 25)
    private String phoneNumber;

    @Column(name = "DESIGNATION_ROLE_NAME", nullable = false, length = 100)
    private String designationRoleName;

    @Column(name = "REPORTING_MANAGER_USER_ID")
    private Long reportingManagerUserId;

    @Column(name = "REPORTING_MANAGER_USERNAME", length = 100)
    private String reportingManagerUsername;

    @Column(name = "REPORTING_MANAGER_FULL_NAME", length = 150)
    private String reportingManagerFullName;

    @Column(name = "REPORTING_MANAGER_ROLE_NAME", length = 100)
    private String reportingManagerRoleName;

    @Column(name = "EDUCATION_QUALIFICATION", length = 255)
    private String educationQualification;

    @Enumerated(EnumType.STRING)
    @Column(name = "INTERVIEW_STAGE", nullable = false, length = 20)
    private OnboardingInterviewStage interviewStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "WORKFLOW_STAGE", nullable = false, length = 40)
    private OnboardingWorkflowStage workflowStage = OnboardingWorkflowStage.HR_SUBMITTED;

    @Column(name = "CREATED_BY_USERNAME", nullable = false, length = 100)
    private String createdByUsername;

    @Column(name = "HR_COMMENT", length = 500)
    private String hrComment;

    @Column(name = "HEAD_HR_COMMENT", length = 500)
    private String headHrComment;

    @Column(name = "CHRO_COMMENT", length = 500)
    private String chroComment;

    @Column(name = "SUPER_ADMIN_COMMENT", length = 500)
    private String superAdminComment;

    @Column(name = "HR_ACTION_BY", length = 100)
    private String hrActionBy;

    @Column(name = "HEAD_HR_ACTION_BY", length = 100)
    private String headHrActionBy;

    @Column(name = "CHRO_ACTION_BY", length = 100)
    private String chroActionBy;

    @Column(name = "SUPER_ADMIN_ACTION_BY", length = 100)
    private String superAdminActionBy;

    @Column(name = "HR_ACTION_AT")
    private LocalDateTime hrActionAt;

    @Column(name = "HEAD_HR_ACTION_AT")
    private LocalDateTime headHrActionAt;

    @Column(name = "CHRO_ACTION_AT")
    private LocalDateTime chroActionAt;

    @Column(name = "SUPER_ADMIN_ACTION_AT")
    private LocalDateTime superAdminActionAt;

    @Column(name = "REFER_BACK_BY", length = 100)
    private String referBackBy;

    @Column(name = "REFER_BACK_AT")
    private LocalDateTime referBackAt;

    @Column(name = "REFER_BACK_COMMENT", length = 500)
    private String referBackComment;

    @Column(name = "REFER_BACK_STAGE", length = 100)
    private String referBackStage;

    @Column(name = "GENERATED_EMPLOYEE_ID", unique = true, length = 50)
    private String generatedEmployeeId;

    @Column(name = "GENERATED_EMAIL_ADDRESS", unique = true, length = 150)
    private String generatedEmailAddress;

    @Column(name = "LAST_REMINDER_AT")
    private LocalDateTime lastReminderAt;

    @Column(name = "REMINDER_COUNT", nullable = false)
    private int reminderCount = 0;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAadhaarCardNumber() {
        return aadhaarCardNumber;
    }

    public void setAadhaarCardNumber(String aadhaarCardNumber) {
        this.aadhaarCardNumber = aadhaarCardNumber;
    }

    public String getPanCardNumber() {
        return panCardNumber;
    }

    public void setPanCardNumber(String panCardNumber) {
        this.panCardNumber = panCardNumber;
    }

    public String getPersonalEmailAddress() {
        return personalEmailAddress;
    }

    public void setPersonalEmailAddress(String personalEmailAddress) {
        this.personalEmailAddress = personalEmailAddress;
    }

    public String getPermanentAddress() {
        return permanentAddress;
    }

    public void setPermanentAddress(String permanentAddress) {
        this.permanentAddress = permanentAddress;
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

    public String getDesignationRoleName() {
        return designationRoleName;
    }

    public void setDesignationRoleName(String designationRoleName) {
        this.designationRoleName = designationRoleName;
    }

    public Long getReportingManagerUserId() {
        return reportingManagerUserId;
    }

    public void setReportingManagerUserId(Long reportingManagerUserId) {
        this.reportingManagerUserId = reportingManagerUserId;
    }

    public String getReportingManagerUsername() {
        return reportingManagerUsername;
    }

    public void setReportingManagerUsername(String reportingManagerUsername) {
        this.reportingManagerUsername = reportingManagerUsername;
    }

    public String getReportingManagerFullName() {
        return reportingManagerFullName;
    }

    public void setReportingManagerFullName(String reportingManagerFullName) {
        this.reportingManagerFullName = reportingManagerFullName;
    }

    public String getReportingManagerRoleName() {
        return reportingManagerRoleName;
    }

    public void setReportingManagerRoleName(String reportingManagerRoleName) {
        this.reportingManagerRoleName = reportingManagerRoleName;
    }

    public OnboardingInterviewStage getInterviewStage() {
        return interviewStage;
    }

    public void setInterviewStage(OnboardingInterviewStage interviewStage) {
        this.interviewStage = interviewStage;
    }

    public String getGeneratedEmployeeId() {
        return generatedEmployeeId;
    }

    public void setGeneratedEmployeeId(String generatedEmployeeId) {
        this.generatedEmployeeId = generatedEmployeeId;
    }

    public String getGeneratedEmailAddress() {
        return generatedEmailAddress;
    }

    public void setGeneratedEmailAddress(String generatedEmailAddress) {
        this.generatedEmailAddress = generatedEmailAddress;
    }

    public OnboardingWorkflowStage getWorkflowStage() {
        return workflowStage;
    }

    public void setWorkflowStage(OnboardingWorkflowStage workflowStage) {
        this.workflowStage = workflowStage;
    }

    public String getCreatedByUsername() {
        return createdByUsername;
    }

    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }

    public String getHrComment() {
        return hrComment;
    }

    public void setHrComment(String hrComment) {
        this.hrComment = hrComment;
    }

    public String getHeadHrComment() {
        return headHrComment;
    }

    public void setHeadHrComment(String headHrComment) {
        this.headHrComment = headHrComment;
    }

    public String getChroComment() {
        return chroComment;
    }

    public void setChroComment(String chroComment) {
        this.chroComment = chroComment;
    }

    public String getSuperAdminComment() {
        return superAdminComment;
    }

    public void setSuperAdminComment(String superAdminComment) {
        this.superAdminComment = superAdminComment;
    }

    public String getHrActionBy() {
        return hrActionBy;
    }

    public void setHrActionBy(String hrActionBy) {
        this.hrActionBy = hrActionBy;
    }

    public String getHeadHrActionBy() {
        return headHrActionBy;
    }

    public void setHeadHrActionBy(String headHrActionBy) {
        this.headHrActionBy = headHrActionBy;
    }

    public String getChroActionBy() {
        return chroActionBy;
    }

    public void setChroActionBy(String chroActionBy) {
        this.chroActionBy = chroActionBy;
    }

    public String getSuperAdminActionBy() {
        return superAdminActionBy;
    }

    public void setSuperAdminActionBy(String superAdminActionBy) {
        this.superAdminActionBy = superAdminActionBy;
    }

    public LocalDateTime getHrActionAt() {
        return hrActionAt;
    }

    public void setHrActionAt(LocalDateTime hrActionAt) {
        this.hrActionAt = hrActionAt;
    }

    public LocalDateTime getHeadHrActionAt() {
        return headHrActionAt;
    }

    public void setHeadHrActionAt(LocalDateTime headHrActionAt) {
        this.headHrActionAt = headHrActionAt;
    }

    public LocalDateTime getChroActionAt() {
        return chroActionAt;
    }

    public void setChroActionAt(LocalDateTime chroActionAt) {
        this.chroActionAt = chroActionAt;
    }

    public LocalDateTime getSuperAdminActionAt() {
        return superAdminActionAt;
    }

    public void setSuperAdminActionAt(LocalDateTime superAdminActionAt) {
        this.superAdminActionAt = superAdminActionAt;
    }

    public String getReferBackBy() {
        return referBackBy;
    }

    public void setReferBackBy(String referBackBy) {
        this.referBackBy = referBackBy;
    }

    public LocalDateTime getReferBackAt() {
        return referBackAt;
    }

    public void setReferBackAt(LocalDateTime referBackAt) {
        this.referBackAt = referBackAt;
    }

    public String getReferBackComment() {
        return referBackComment;
    }

    public void setReferBackComment(String referBackComment) {
        this.referBackComment = referBackComment;
    }

    public String getReferBackStage() {
        return referBackStage;
    }

    public void setReferBackStage(String referBackStage) {
        this.referBackStage = referBackStage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getLastReminderAt() {
        return lastReminderAt;
    }

    public void setLastReminderAt(LocalDateTime lastReminderAt) {
        this.lastReminderAt = lastReminderAt;
    }

    public int getReminderCount() {
        return reminderCount;
    }

    public void setReminderCount(int reminderCount) {
        this.reminderCount = reminderCount;
    }
}
