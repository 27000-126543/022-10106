package com.medical.triage.controller;

import com.medical.triage.dto.response.ApiResponse;
import com.medical.triage.dto.response.TriageExplanationResponse;
import com.medical.triage.service.TriageService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/triage-explanations")
@RequiredArgsConstructor
public class TriageExplanationController {

    private final TriageService triageService;

    @GetMapping("/appointment/{appointmentId}")
    public ApiResponse<TriageExplanationResponse> getTriageExplanationByAppointment(
            @PathVariable @NotNull(message = "预约ID不能为空") Long appointmentId) {
        TriageExplanationResponse explanation = triageService.getTriageExplanation(appointmentId);
        return ApiResponse.success(explanation);
    }

    @GetMapping("/{id}")
    public ApiResponse<TriageExplanationResponse> getTriageExplanationById(
            @PathVariable @NotNull(message = "解释ID不能为空") Long id) {
        TriageExplanationResponse explanation = triageService.getTriageExplanationById(id);
        return ApiResponse.success(explanation);
    }
}
