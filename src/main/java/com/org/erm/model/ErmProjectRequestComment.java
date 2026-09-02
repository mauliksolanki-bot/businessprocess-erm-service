package com.org.erm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "ERM_PROJECT_REQUEST_COMMENTS")
public class ErmProjectRequestComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "PROJECT_REQUEST_ID", nullable = false)
    private Long projectRequestId;

    @Column(name = "STEP", nullable = false, length = 120)
    private String step;

    @Column(name = "ACTOR", nullable = false, length = 100)
    private String actor;

    @Column(name = "DECISION", nullable = false, length = 50)
    private String decision;

    @Column(name = "COMMENT_TEXT", length = 500)
    private String commentText;

    @Column(name = "ACTION_AT", nullable = false)
    private LocalDateTime actionAt;

    public Long getId() {
        return id;
    }

    public Long getProjectRequestId() {
        return projectRequestId;
    }

    public void setProjectRequestId(Long projectRequestId) {
        this.projectRequestId = projectRequestId;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public LocalDateTime getActionAt() {
        return actionAt;
    }

    public void setActionAt(LocalDateTime actionAt) {
        this.actionAt = actionAt;
    }
}
