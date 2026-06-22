package com.medical.triage.entity;

import com.medical.triage.enums.ConsultationType;
import com.medical.triage.enums.RiskLevel;
import com.medical.triage.enums.TriageStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "triage_result")
public class TriageResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long appointmentId;

    private Long customerId;

    private Long storeId;

    private Long departmentId;

    private Long consultantGroupId;

    private Long explanationId;

    @Enumerated(EnumType.STRING)
    private ConsultationType consultationType;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    private Long triagedBy;

    private LocalDateTime triageTime;

    private Boolean isReassigned;

    @Column(columnDefinition = "TEXT")
    private String reassignedReason;

    @Enumerated(EnumType.STRING)
    private TriageStatus status;

    private Long usedRuleVersion;

    private Boolean isGrayRuleUsed;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
