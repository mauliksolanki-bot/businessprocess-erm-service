package com.org.erm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ERM_SUPPORT_CATEGORIES")
public class ErmSupportCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CATEGORY_CODE", nullable = false, unique = true, length = 100)
    private String categoryCode;

    @Column(name = "CATEGORY_TITLE", nullable = false, length = 120)
    private String categoryTitle;

    @Column(name = "TICKET_TYPE", nullable = false, length = 50)
    private String ticketType;

    @Column(name = "PARENT_CATEGORY_CODE", length = 100)
    private String parentCategoryCode;

    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "IS_ACTIVE", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public String getCategoryTitle() {
        return categoryTitle;
    }

    public String getTicketType() {
        return ticketType;
    }

    public String getParentCategoryCode() {
        return parentCategoryCode;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }
}
