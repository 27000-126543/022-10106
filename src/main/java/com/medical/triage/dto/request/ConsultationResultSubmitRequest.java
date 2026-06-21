package com.medical.triage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationResultSubmitRequest {

    @NotNull(message = "预约ID不能为空")
    private Long appointmentId;

    @NotNull(message = "咨询师ID不能为空")
    private Long consultantId;

    @NotBlank(message = "咨询师姓名不能为空")
    private String consultantName;

    @NotNull(message = "医生ID不能为空")
    private Long doctorId;

    @NotBlank(message = "医生姓名不能为空")
    private String doctorName;

    @NotBlank(message = "诊断结果不能为空")
    private String diagnosis;

    @NotBlank(message = "建议不能为空")
    private String suggestion;

    private String treatmentPlan;

    private LocalDate followUpDate;
}
