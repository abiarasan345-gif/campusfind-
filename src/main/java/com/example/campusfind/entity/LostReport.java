package com.example.campusfind.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "lost_reports")
public class LostReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User reportingUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "item_name", nullable = false, length = 150)
    private String itemName;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, length = 200)
    private String location;

    @Column(name = "date_lost", nullable = false)
    private LocalDate dateLost;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_found_item_id", unique = true)
    private FoundItem matchedFoundItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LostStatus status = LostStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_state", length = 20)
    private CaseState caseState = CaseState.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = LostStatus.OPEN;
        }
    }

    public Long getId() { return id; }
    public User getReportingUser() { return reportingUser; }
    public Category getCategory() { return category; }
    public String getItemName() { return itemName; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public LocalDate getDateLost() { return dateLost; }
    public FoundItem getMatchedFoundItem() { return matchedFoundItem; }
    public LostStatus getStatus() { return status; }
    public CaseState getCaseState() { return caseState == null ? CaseState.PENDING : caseState; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setReportingUser(User reportingUser) { this.reportingUser = reportingUser; }
    public void setCategory(Category category) { this.category = category; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setDescription(String description) { this.description = description; }
    public void setLocation(String location) { this.location = location; }
    public void setDateLost(LocalDate dateLost) { this.dateLost = dateLost; }
    public void setMatchedFoundItem(FoundItem matchedFoundItem) { this.matchedFoundItem = matchedFoundItem; }
    public void setStatus(LostStatus status) { this.status = status; }
    public void setCaseState(CaseState caseState) { this.caseState = caseState; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
