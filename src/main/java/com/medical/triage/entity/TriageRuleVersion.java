package com.medical.triage.entity;

import com.medical.triage.enums.ConsultationType;
import com.medical.triage.enums.DepartmentType;
import com.medical.triage.enums.RuleStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "triage_rule_version")
public class TriageRuleVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ruleId;

    private String ruleCode;

    private Integer version;

    private String name;

    private Long storeId;

    @Enumerated(EnumType.STRING)
    private ConsultationType consultationType;

    @Enumerated(EnumType.STRING)
    private DepartmentType departmentType;

    @ElementCollection
    private List<String> keywords;

    private Integer priority;

    private Integer minAge;

    private Integer maxAge;

    private String gender;

    private Boolean isEnabled;

    @Enumerated(EnumType.STRING)
    private RuleStatus status;

    private String createdBy;

    private LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String description;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
