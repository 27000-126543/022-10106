package com.medical.triage.dto.response;

import com.medical.triage.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskStats {

    private RiskLevel riskLevel;

    private Integer count;

    private BigDecimal percentage;

    private Integer doctorAssessmentCount;
}
