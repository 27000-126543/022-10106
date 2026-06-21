package com.medical.triage.dto.response;

import com.medical.triage.enums.ConsultationType;
import com.medical.triage.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriageResultResponse {

    private Long id;

    private Long appointmentId;

    private String customerName;

    private String storeName;

    private String departmentName;

    private String consultantGroupName;

    private ConsultationType consultationType;

    private RiskLevel riskLevel;

    private Boolean needDoctorAssessment;

    private LocalDateTime triageTime;

    private Boolean isReassigned;

    private String reassignedReason;
}
