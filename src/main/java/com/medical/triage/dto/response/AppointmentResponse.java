package com.medical.triage.dto.response;

import com.medical.triage.enums.AppointmentStatus;
import com.medical.triage.enums.ConsultationType;
import com.medical.triage.enums.RiskLevel;
import com.medical.triage.enums.TriageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {

    private Long id;

    private Long customerId;

    private String customerName;

    private String customerPhone;

    private Long storeId;

    private String storeName;

    private LocalDateTime appointmentTime;

    private ConsultationType consultationType;

    private AppointmentStatus status;

    private TriageStatus triageStatus;

    private RiskLevel riskLevel;

    private LocalDateTime createdAt;
}
