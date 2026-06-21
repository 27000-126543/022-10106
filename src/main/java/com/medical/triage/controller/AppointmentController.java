package com.medical.triage.controller;

import com.medical.triage.dto.request.AppointmentCreateRequest;
import com.medical.triage.dto.response.ApiResponse;
import com.medical.triage.dto.response.AppointmentResponse;
import com.medical.triage.dto.response.TriageResultResponse;
import com.medical.triage.service.AppointmentService;
import com.medical.triage.service.TriageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final TriageService triageService;

    @PostMapping
    public ApiResponse<AppointmentResponse> createAppointment(
            @Valid @RequestBody AppointmentCreateRequest request) {
        AppointmentResponse response = appointmentService.createAppointment(request);
        return ApiResponse.success("预约创建成功", response);
    }

    @GetMapping("/{id}")
    public ApiResponse<AppointmentResponse> getAppointment(@PathVariable Long id) {
        AppointmentResponse response = appointmentService.getAppointment(id);
        return ApiResponse.success(response);
    }

    @GetMapping
    public ApiResponse<Page<AppointmentResponse>> getAppointments(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AppointmentResponse> response = appointmentService.getAppointments(storeId, start, end, pageable);
        return ApiResponse.success(response);
    }

    @PostMapping("/{id}/arrival")
    public ApiResponse<Void> confirmArrival(@PathVariable Long id) {
        appointmentService.confirmArrival(id);
        return ApiResponse.success("确认到店成功", null);
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancelAppointment(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        appointmentService.cancelAppointment(id, reason);
        return ApiResponse.success("预约取消成功", null);
    }

    @PostMapping("/{id}/triage")
    public ApiResponse<TriageResultResponse> triageAppointment(@PathVariable Long id) {
        TriageResultResponse response = triageService.triageAppointment(id);
        return ApiResponse.success("分诊执行成功", response);
    }

    @GetMapping("/{id}/triage-result")
    public ApiResponse<TriageResultResponse> getTriageResult(@PathVariable Long id) {
        TriageResultResponse response = triageService.getTriageResult(id);
        return ApiResponse.success(response);
    }
}
