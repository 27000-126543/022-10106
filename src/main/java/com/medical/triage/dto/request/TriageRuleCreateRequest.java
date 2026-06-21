package com.medical.triage.dto.request;

import com.medical.triage.enums.ConsultationType;
import com.medical.triage.enums.DepartmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriageRuleCreateRequest {

    @NotNull(message = "门店ID不能为空")
    private Long storeId;

    @NotBlank(message = "规则名称不能为空")
    private String name;

    @NotNull(message = "咨询类型不能为空")
    private ConsultationType consultationType;

    @NotNull(message = "科室类型不能为空")
    private DepartmentType departmentType;

    @NotEmpty(message = "关键词列表不能为空")
    private List<String> keywords;

    @NotNull(message = "优先级不能为空")
    private Integer priority;

    private Integer minAge;

    private Integer maxAge;

    private String gender;
}
