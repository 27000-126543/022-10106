package com.medical.triage.dto.request;

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
public class MessageResendRequest {

    @NotEmpty(message = "消息队列ID列表不能为空")
    private List<Long> messageQueueIds;

    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;

    @NotNull(message = "操作人名称不能为空")
    private String operatorName;
}
