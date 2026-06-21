package com.medical.triage.dto.request;

import jakarta.validation.Valid;
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
public class QuestionnaireAnswerSubmitRequest {

    @NotNull(message = "预约ID不能为空")
    private Long appointmentId;

    @NotNull(message = "客户ID不能为空")
    private Long customerId;

    @NotEmpty(message = "答案列表不能为空")
    @Valid
    private List<AnswerItem> answers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerItem {

        @NotNull(message = "问题ID不能为空")
        private Long questionId;

        @NotBlank(message = "答案内容不能为空")
        private String answerText;
    }
}
