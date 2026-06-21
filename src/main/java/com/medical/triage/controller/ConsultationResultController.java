package com.medical.triage.controller;

import com.medical.triage.dto.request.ConsultationResultSubmitRequest;
import com.medical.triage.dto.response.ApiResponse;
import com.medical.triage.entity.ConsultationResult;
import com.medical.triage.service.ConsultationResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/consultation-results")
@RequiredArgsConstructor
public class ConsultationResultController {

    private final ConsultationResultService consultationResultService;

    @PostMapping
    public ApiResponse<Void> submitResult(@Valid @RequestBody ConsultationResultSubmitRequest request) {
        consultationResultService.submitResult(request);
        return ApiResponse.success("面诊结果提交成功", null);
    }

    @GetMapping("/appointment/{appointmentId}")
    public ApiResponse<ConsultationResult> getResultByAppointment(@PathVariable Long appointmentId) {
        ConsultationResult result = consultationResultService.getResultByAppointment(appointmentId);
        return ApiResponse.success(result);
    }
}
