package com.medical.triage.dto.request;

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
public class GuideTaskReassignRequest {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;

    @NotBlank(message = "操作人姓名不能为空")
    private String operatorName;

    @NotNull(message = "新指派人ID不能为空")
    private Long newAssigneeId;

    @NotBlank(message = "新指派人姓名不能为空")
    private String newAssigneeName;

    @NotNull(message = "新科室ID不能为空")
    private Long newDepartmentId;

    @NotBlank(message = "转派原因不能为空")
    private String reason;
}
