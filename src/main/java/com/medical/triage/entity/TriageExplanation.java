package com.medical.triage.entity;

import com.medical.triage.enums.ConsultationType;
import com.medical.triage.enums.RiskLevel;
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
@Table(name = "triage_explanation")
public class TriageExplanation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long triageResultId;

    private Long appointmentId;

    @ElementCollection
    private List<String> matchedAppointmentKeywords;

    @ElementCollection
    private List<String> matchedQuestionAnswers;

    @ElementCollection
    private List<String> matchedRiskKeywords;

    @ElementCollection
    private List<String> matchedRules;

    @ElementCollection
    private List<String> departmentScores;

    @ElementCollection
    private List<String> groupScores;

    private Long finalDepartmentId;

    private String finalDepartmentName;

    private Long finalGroupId;

    private String finalGroupName;

    @Enumerated(EnumType.STRING)
    private ConsultationType finalConsultationType;

    @Enumerated(EnumType.STRING)
    private RiskLevel finalRiskLevel;

    private Boolean needDoctorAssessment;

    private Integer overallScore;

    @Column(columnDefinition = "TEXT")
    private String explanationText;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
