package com.medical.triage.controller;

import com.medical.triage.dto.request.TriageRuleCreateRequest;
import com.medical.triage.dto.response.ApiResponse;
import com.medical.triage.entity.TriageRule;
import com.medical.triage.service.TriageRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/triage-rules")
@RequiredArgsConstructor
public class TriageRuleController {

    private final TriageRuleService triageRuleService;

    @PostMapping
    public ApiResponse<TriageRule> createRule(@Valid @RequestBody TriageRuleCreateRequest request) {
        TriageRule rule = triageRuleService.createRule(request);
        return ApiResponse.success("规则创建成功", rule);
    }

    @GetMapping("/store/{storeId}")
    public ApiResponse<List<TriageRule>> getRulesByStore(@PathVariable Long storeId) {
        List<TriageRule> rules = triageRuleService.getRulesByStore(storeId);
        return ApiResponse.success(rules);
    }

    @PostMapping("/{id}/enable")
    public ApiResponse<Void> enableRule(@PathVariable Long id) {
        triageRuleService.enableRule(id);
        return ApiResponse.success("规则启用成功", null);
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<Void> disableRule(@PathVariable Long id) {
        triageRuleService.disableRule(id);
        return ApiResponse.success("规则禁用成功", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRule(@PathVariable Long id) {
        triageRuleService.deleteRule(id);
        return ApiResponse.success("规则删除成功", null);
    }
}
