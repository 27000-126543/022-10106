package com.medical.triage.dto.response;

import com.medical.triage.enums.ConsultationType;
import com.medical.triage.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriageExplanationResponse {

    private Long id;

    private Long triageResultId;

    private Long appointmentId;

    private List<String> matchedAppointmentKeywords;

    private List<String> matchedQuestionAnswers;

    private List<String> matchedRiskKeywords;

    private List<String> matchedRules;

    private List<String> departmentScores;

    private List<String> groupScores;

    private Long finalDepartmentId;

    private String finalDepartmentName;

    private Long finalGroupId;

    private String finalGroupName;

    private ConsultationType finalConsultationType;

    private RiskLevel finalRiskLevel;

    private Boolean needDoctorAssessment;

    private Integer overallScore;

    private String explanationText;

    private LocalDateTime createdAt;
}
