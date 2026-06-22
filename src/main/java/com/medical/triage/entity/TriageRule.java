package com.medical.triage.entity;

import com.medical.triage.enums.AppointmentSource;
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
@Table(name = "triage_rule")
public class TriageRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long storeId;

    private String name;

    private String ruleCode;

    private Integer version;

    @Enumerated(EnumType.STRING)
    private RuleStatus status;

    @Enumerated(EnumType.STRING)
    private ConsultationType consultationType;

    @Enumerated(EnumType.STRING)
    private DepartmentType departmentType;

    @ElementCollection
    private List<String> keywords;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    private List<AppointmentSource> graySources;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    private List<ConsultationType> grayProjects;

    private Integer grayPercentage;

    private Boolean isOfficial;

    private Long parentRuleId;

    private Integer priority;

    private Integer minAge;

    private Integer maxAge;

    private String gender;

    private Boolean isEnabled;

    private String publishedBy;

    private LocalDateTime publishedAt;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (version == null) {
            version = 1;
        }
        if (status == null) {
            status = RuleStatus.DRAFT;
        }
        if (isOfficial == null) {
            isOfficial = false;
        }
        if (grayPercentage == null) {
            grayPercentage = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
