package com.medical.triage.service;

import com.medical.triage.dto.request.TriageRuleCreateRequest;
import com.medical.triage.entity.TriageRule;
import com.medical.triage.repository.TriageRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TriageRuleService {

    private final TriageRuleRepository triageRuleRepository;

    @Transactional
    public TriageRule createRule(TriageRuleCreateRequest request) {
        log.info("创建分诊规则, 门店ID: {}, 规则名称: {}", request.getStoreId(), request.getName());

        if (triageRuleRepository.existsByStoreIdAndNameAndIsEnabledTrue(request.getStoreId(), request.getName())) {
            throw new IllegalArgumentException("该门店已存在同名规则: " + request.getName());
        }

        TriageRule rule = TriageRule.builder()
                .storeId(request.getStoreId())
                .name(request.getName())
                .consultationType(request.getConsultationType())
                .departmentType(request.getDepartmentType())
                .keywords(request.getKeywords())
                .priority(request.getPriority())
                .minAge(request.getMinAge())
                .maxAge(request.getMaxAge())
                .gender(request.getGender())
                .isEnabled(true)
                .build();

        TriageRule saved = triageRuleRepository.save(rule);
        log.info("分诊规则创建成功, ruleId: {}", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<TriageRule> getRulesByStore(Long storeId) {
        log.info("获取门店规则, storeId: {}", storeId);
        return triageRuleRepository.findByStoreIdAndIsEnabledTrueOrderByPriorityAsc(storeId);
    }

    @Transactional
    public void enableRule(Long ruleId) {
        log.info("启用规则, ruleId: {}", ruleId);

        TriageRule rule = triageRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("规则不存在: " + ruleId));

        rule.setIsEnabled(true);
        triageRuleRepository.save(rule);
        log.info("规则启用成功, ruleId: {}", ruleId);
    }

    @Transactional
    public void disableRule(Long ruleId) {
        log.info("禁用规则, ruleId: {}", ruleId);

        TriageRule rule = triageRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("规则不存在: " + ruleId));

        rule.setIsEnabled(false);
        triageRuleRepository.save(rule);
        log.info("规则禁用成功, ruleId: {}", ruleId);
    }

    @Transactional
    public void deleteRule(Long ruleId) {
        log.info("删除规则, ruleId: {}", ruleId);

        if (!triageRuleRepository.existsById(ruleId)) {
            throw new IllegalArgumentException("规则不存在: " + ruleId);
        }

        triageRuleRepository.deleteById(ruleId);
        log.info("规则删除成功, ruleId: {}", ruleId);
    }
}
