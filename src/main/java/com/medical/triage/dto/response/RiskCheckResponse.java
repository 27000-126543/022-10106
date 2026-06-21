package com.medical.triage.dto.response;

import com.medical.triage.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskCheckResponse {

    private Boolean passed;

    private RiskLevel riskLevel;

    private List<String> matchedKeywords;

    private Boolean needDoctorAssessment;

    private String warningMessage;
}
