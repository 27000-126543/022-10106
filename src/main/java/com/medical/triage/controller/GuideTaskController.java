package com.medical.triage.controller;

import com.medical.triage.dto.request.GuideTaskReassignRequest;
import com.medical.triage.dto.response.ApiResponse;
import com.medical.triage.dto.response.GuideTaskResponse;
import com.medical.triage.enums.GuideTaskStatus;
import com.medical.triage.service.GuideTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/guide-tasks")
@RequiredArgsConstructor
public class GuideTaskController {

    private final GuideTaskService guideTaskService;

    @GetMapping("/{id}")
    public ApiResponse<GuideTaskResponse> getTask(@PathVariable Long id) {
        GuideTaskResponse response = guideTaskService.getTask(id);
        return ApiResponse.success(response);
    }

    @GetMapping("/appointment/{appointmentId}")
    public ApiResponse<List<GuideTaskResponse>> getTasksByAppointment(@PathVariable Long appointmentId) {
        List<GuideTaskResponse> responses = guideTaskService.getTasksByAppointment(appointmentId);
        return ApiResponse.success(responses);
    }

    @GetMapping
    public ApiResponse<Page<GuideTaskResponse>> getTasksByStore(
            @RequestParam Long storeId,
            @RequestParam(required = false) GuideTaskStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<GuideTaskResponse> responses = guideTaskService.getTasksByStore(storeId, status, pageable);
        return ApiResponse.success(responses);
    }

    @PostMapping("/reassign")
    public ApiResponse<GuideTaskResponse> reassignTask(@Valid @RequestBody GuideTaskReassignRequest request) {
        GuideTaskResponse response = guideTaskService.reassignTask(request);
        return ApiResponse.success("任务改派成功", response);
    }
}
