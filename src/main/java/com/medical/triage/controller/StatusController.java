package com.medical.triage.controller;

import com.medical.triage.dto.request.StatusUpdateRequest;
import com.medical.triage.dto.response.ApiResponse;
import com.medical.triage.entity.StatusRecord;
import com.medical.triage.service.StatusFlowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/status")
@RequiredArgsConstructor
public class StatusController {

    private final StatusFlowService statusFlowService;

    @PostMapping("/update")
    public ApiResponse<Void> updateStatus(@Valid @RequestBody StatusUpdateRequest request) {
        statusFlowService.recordStatusChange(request);
        return ApiResponse.success("状态更新成功", null);
    }

    @GetMapping("/history/{appointmentId}")
    public ApiResponse<List<StatusRecord>> getStatusHistory(@PathVariable Long appointmentId) {
        List<StatusRecord> history = statusFlowService.getStatusHistory(appointmentId);
        return ApiResponse.success(history);
    }
}
