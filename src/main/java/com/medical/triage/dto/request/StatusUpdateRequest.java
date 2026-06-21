package com.medical.triage.dto.request;

import com.medical.triage.enums.AppointmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateRequest {

    @NotNull(message = "预约ID不能为空")
    private Long appointmentId;

    private Long guideTaskId;

    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;

    @NotBlank(message = "操作人姓名不能为空")
    private String operatorName;

    @NotBlank(message = "操作人角色不能为空")
    private String operatorRole;

    @NotNull(message = "目标状态不能为空")
    private AppointmentStatus toStatus;

    private String remark;
}
