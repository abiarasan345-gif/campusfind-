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
import jakarta.persistence.Table;

@Entity
@Table(name = "found_items")
public class FoundItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_user_id", nullable = false)
    private User reportingStaff;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "item_name", nullable = false, length = 150)
    private String itemName;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, length = 200)
    private String location;

    @Column(name = "date_found", nullable = false)
    private LocalDate dateFound;

    @Column(name = "finder_phone_number", length = 30)
    private String finderPhoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FoundStatus status = FoundStatus.AVAILABLE;

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
            status = FoundStatus.AVAILABLE;
        }
    }

    public Long getId() { return id; }
    public User getReportingStaff() { return reportingStaff; }
    public Category getCategory() { return category; }
    public String getItemName() { return itemName; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public LocalDate getDateFound() { return dateFound; }
    public String getFinderPhoneNumber() { return finderPhoneNumber; }
    public FoundStatus getStatus() { return status; }
    public CaseState getCaseState() { return caseState == null ? CaseState.PENDING : caseState; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setReportingStaff(User reportingStaff) { this.reportingStaff = reportingStaff; }
    public void setCategory(Category category) { this.category = category; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setDescription(String description) { this.description = description; }
    public void setLocation(String location) { this.location = location; }
    public void setDateFound(LocalDate dateFound) { this.dateFound = dateFound; }
    public void setFinderPhoneNumber(String finderPhoneNumber) { this.finderPhoneNumber = finderPhoneNumber; }
    public void setStatus(FoundStatus status) { this.status = status; }
    public void setCaseState(CaseState caseState) { this.caseState = caseState; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
