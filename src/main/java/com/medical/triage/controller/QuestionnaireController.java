package com.medical.triage.controller;

import com.medical.triage.dto.request.QuestionnaireAnswerSubmitRequest;
import com.medical.triage.dto.response.ApiResponse;
import com.medical.triage.entity.QuestionnaireAnswer;
import com.medical.triage.entity.QuestionnaireQuestion;
import com.medical.triage.enums.ConsultationType;
import com.medical.triage.service.QuestionnaireService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questionnaires")
@RequiredArgsConstructor
public class QuestionnaireController {

    private final QuestionnaireService questionnaireService;

    @GetMapping("/questions")
    public ApiResponse<List<QuestionnaireQuestion>> getQuestions(
            @RequestParam Long storeId,
            @RequestParam ConsultationType consultationType) {
        List<QuestionnaireQuestion> questions = questionnaireService.getQuestions(storeId, consultationType);
        return ApiResponse.success(questions);
    }

    @PostMapping("/answers")
    public ApiResponse<Void> submitAnswers(@Valid @RequestBody QuestionnaireAnswerSubmitRequest request) {
        questionnaireService.submitAnswers(request);
        return ApiResponse.success("问卷答案提交成功", null);
    }

    @GetMapping("/answers/appointment/{appointmentId}")
    public ApiResponse<List<QuestionnaireAnswer>> getAnswersByAppointment(@PathVariable Long appointmentId) {
        List<QuestionnaireAnswer> answers = questionnaireService.getAnswersByAppointment(appointmentId);
        return ApiResponse.success(answers);
    }
}
