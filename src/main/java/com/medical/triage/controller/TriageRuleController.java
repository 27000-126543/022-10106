package com.medical.triage.controller;

import com.medical.triage.dto.request.RuleGrayRequest;
import com.medical.triage.dto.request.RulePublishRequest;
import com.medical.triage.dto.request.RuleRollbackRequest;
import com.medical.triage.dto.request.TriageRuleCreateRequest;
import com.medical.triage.dto.response.ApiResponse;
import com.medical.triage.dto.response.TriageRuleVersionResponse;
import com.medical.triage.entity.TriageRule;
import com.medical.triage.entity.TriageRuleVersion;
import com.medical.triage.service.TriageRuleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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

    @PostMapping("/{id}/new-version")
    public ApiResponse<TriageRule> createNewVersion(
            @PathVariable @NotNull(message = "规则ID不能为空") Long id,
            @RequestParam @NotNull(message = "操作人名称不能为空") String operatorName) {
        TriageRule rule = triageRuleService.createNewVersion(id, operatorName);
        return ApiResponse.success("规则新版本创建成功", rule);
    }

    @PostMapping("/{id}/publish-gray")
    public ApiResponse<Void> publishGrayRule(
            @PathVariable @NotNull(message = "规则ID不能为空") Long id,
            @Valid @RequestBody RuleGrayRequest request) {
        request.setRuleId(id);
        triageRuleService.publishGrayRule(request);
        return ApiResponse.success("灰度规则发布成功", null);
    }

    @PostMapping("/{id}/publish-official")
    public ApiResponse<Void> publishOfficialRule(
            @PathVariable @NotNull(message = "规则ID不能为空") Long id,
            @Valid @RequestBody RulePublishRequest request) {
        request.setRuleId(id);
        triageRuleService.publishOfficialRule(request);
        return ApiResponse.success("正式规则发布成功", null);
    }

    @PostMapping("/{id}/rollback")
    public ApiResponse<Void> rollbackRule(
            @PathVariable @NotNull(message = "规则ID不能为空") Long id,
            @Valid @RequestBody RuleRollbackRequest request) {
        request.setRuleId(id);
        triageRuleService.rollbackRule(request);
        return ApiResponse.success("规则回滚成功", null);
    }

    @GetMapping("/{id}/versions")
    public ApiResponse<List<TriageRuleVersionResponse>> getRuleVersions(
            @PathVariable @NotNull(message = "规则ID不能为空") Long id) {
        List<TriageRuleVersion> versions = triageRuleService.getRuleVersions(id);
        List<TriageRuleVersionResponse> responseList = versions.stream()
                .map(this::buildVersionResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(responseList);
    }

    private TriageRuleVersionResponse buildVersionResponse(TriageRuleVersion version) {
        return TriageRuleVersionResponse.builder()
                .id(version.getId())
                .ruleId(version.getRuleId())
                .ruleCode(version.getRuleCode())
                .version(version.getVersion())
                .name(version.getName())
                .storeId(version.getStoreId())
                .consultationType(version.getConsultationType())
                .departmentType(version.getDepartmentType())
                .keywords(version.getKeywords())
                .priority(version.getPriority())
                .minAge(version.getMinAge())
                .maxAge(version.getMaxAge())
                .gender(version.getGender())
                .isEnabled(version.getIsEnabled())
                .status(version.getStatus())
                .createdBy(version.getCreatedBy())
                .createdAt(version.getCreatedAt())
                .description(version.getDescription())
                .build();
    }
}
