package com.medical.triage.service;

import com.medical.triage.dto.request.RuleGrayRequest;
import com.medical.triage.dto.request.RulePublishRequest;
import com.medical.triage.dto.request.RuleRollbackRequest;
import com.medical.triage.dto.request.TriageRuleCreateRequest;
import com.medical.triage.entity.Appointment;
import com.medical.triage.entity.TriageRule;
import com.medical.triage.entity.TriageRuleVersion;
import com.medical.triage.enums.AppointmentSource;
import com.medical.triage.enums.ConsultationType;
import com.medical.triage.enums.RuleStatus;
import com.medical.triage.repository.AppointmentRepository;
import com.medical.triage.repository.TriageRuleRepository;
import com.medical.triage.repository.TriageRuleVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TriageRuleService {

    private final TriageRuleRepository triageRuleRepository;
    private final TriageRuleVersionRepository triageRuleVersionRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional
    public TriageRule createRule(TriageRuleCreateRequest request) {
        log.info("创建分诊规则, 门店ID: {}, 规则名称: {}", request.getStoreId(), request.getName());

        if (triageRuleRepository.existsByStoreIdAndNameAndIsEnabledTrue(request.getStoreId(), request.getName())) {
            throw new IllegalArgumentException("该门店已存在同名规则: " + request.getName());
        }

        String ruleCode = "RULE_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

        TriageRule rule = TriageRule.builder()
                .storeId(request.getStoreId())
                .name(request.getName())
                .ruleCode(ruleCode)
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
        log.info("分诊规则创建成功, ruleId: {}, ruleCode: {}", saved.getId(), saved.getRuleCode());

        saveRuleVersion(saved, "创建规则");

        return saved;
    }

    @Transactional
    public TriageRule createNewVersion(Long ruleId, String operatorName) {
        log.info("创建规则新版本, ruleId: {}, operator: {}", ruleId, operatorName);

        TriageRule existingRule = triageRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("规则不存在: " + ruleId));

        saveRuleVersion(existingRule, operatorName + " 创建新版本");

        TriageRule newVersionRule = TriageRule.builder()
                .storeId(existingRule.getStoreId())
                .name(existingRule.getName())
                .ruleCode(existingRule.getRuleCode())
                .version(existingRule.getVersion() + 1)
                .status(RuleStatus.DRAFT)
                .consultationType(existingRule.getConsultationType())
                .departmentType(existingRule.getDepartmentType())
                .keywords(new ArrayList<>(existingRule.getKeywords()))
                .priority(existingRule.getPriority())
                .minAge(existingRule.getMinAge())
                .maxAge(existingRule.getMaxAge())
                .gender(existingRule.getGender())
                .isOfficial(false)
                .parentRuleId(existingRule.getId())
                .isEnabled(true)
                .build();

        TriageRule saved = triageRuleRepository.save(newVersionRule);
        log.info("规则新版本创建成功, ruleId: {}, version: {}", saved.getId(), saved.getVersion());

        return saved;
    }

    @Transactional
    public void publishGrayRule(RuleGrayRequest request) {
        log.info("发布灰度规则, ruleId: {}, operator: {}", request.getRuleId(), request.getOperatorName());

        TriageRule rule = triageRuleRepository.findById(request.getRuleId())
                .orElseThrow(() -> new IllegalArgumentException("规则不存在: " + request.getRuleId()));

        if (rule.getStatus() != RuleStatus.DRAFT) {
            throw new IllegalStateException("只有草稿状态的规则才能发布灰度");
        }

        rule.setGraySources(request.getGraySources());
        rule.setGrayProjects(request.getGrayProjects());
        rule.setGrayPercentage(request.getGrayPercentage() != null ? request.getGrayPercentage() : 10);
        rule.setStatus(RuleStatus.GRAY);
        rule.setPublishedBy(request.getOperatorName());
        rule.setPublishedAt(LocalDateTime.now());

        triageRuleRepository.save(rule);
        saveRuleVersion(rule, request.getOperatorName() + " 发布灰度规则");

        log.info("灰度规则发布成功, ruleId: {}, grayPercentage: {}", rule.getId(), rule.getGrayPercentage());
    }

    @Transactional
    public void publishOfficialRule(RulePublishRequest request) {
        log.info("发布正式规则, ruleId: {}, operator: {}", request.getRuleId(), request.getOperatorName());

        TriageRule rule = triageRuleRepository.findById(request.getRuleId())
                .orElseThrow(() -> new IllegalArgumentException("规则不存在: " + request.getRuleId()));

        if (rule.getStatus() != RuleStatus.GRAY && rule.getStatus() != RuleStatus.DRAFT) {
            throw new IllegalStateException("只有草稿或灰度状态的规则才能发布为正式规则");
        }

        List<TriageRule> officialRules = triageRuleRepository.findByStoreIdAndIsOfficialTrueAndIsEnabledTrue(rule.getStoreId());
        for (TriageRule officialRule : officialRules) {
            if (officialRule.getRuleCode() != null && officialRule.getRuleCode().equals(rule.getRuleCode())
                    && !officialRule.getId().equals(rule.getId())) {
                officialRule.setIsOfficial(false);
                officialRule.setStatus(RuleStatus.ARCHIVED);
                triageRuleRepository.save(officialRule);
                saveRuleVersion(officialRule, request.getOperatorName() + " 归档旧版本");
            }
        }

        rule.setIsOfficial(true);
        rule.setStatus(RuleStatus.OFFICIAL);
        rule.setPublishedBy(request.getOperatorName());
        rule.setPublishedAt(LocalDateTime.now());

        triageRuleRepository.save(rule);
        saveRuleVersion(rule, request.getOperatorName() + " 发布为正式规则");

        log.info("正式规则发布成功, ruleId: {}, ruleCode: {}", rule.getId(), rule.getRuleCode());
    }

    @Transactional
    public void rollbackRule(RuleRollbackRequest request) {
        log.info("回滚规则, ruleId: {}, targetVersionId: {}, operator: {}",
                request.getRuleId(), request.getTargetVersionId(), request.getOperatorName());

        TriageRule currentRule = triageRuleRepository.findById(request.getRuleId())
                .orElseThrow(() -> new IllegalArgumentException("规则不存在: " + request.getRuleId()));

        TriageRuleVersion targetVersion = triageRuleVersionRepository.findById(request.getTargetVersionId())
                .orElseThrow(() -> new IllegalArgumentException("目标版本不存在: " + request.getTargetVersionId()));

        if (!targetVersion.getRuleId().equals(request.getRuleId())) {
            throw new IllegalArgumentException("目标版本不属于该规则");
        }

        saveRuleVersion(currentRule, request.getOperatorName() + " 回滚前保存当前版本");

        currentRule.setName(targetVersion.getName());
        currentRule.setConsultationType(targetVersion.getConsultationType());
        currentRule.setDepartmentType(targetVersion.getDepartmentType());
        currentRule.setKeywords(new ArrayList<>(targetVersion.getKeywords()));
        currentRule.setPriority(targetVersion.getPriority());
        currentRule.setMinAge(targetVersion.getMinAge());
        currentRule.setMaxAge(targetVersion.getMaxAge());
        currentRule.setGender(targetVersion.getGender());
        currentRule.setStatus(RuleStatus.DRAFT);
        currentRule.setVersion(currentRule.getVersion() + 1);

        triageRuleRepository.save(currentRule);
        saveRuleVersion(currentRule, request.getOperatorName() + " 回滚到版本 " + targetVersion.getVersion());

        log.info("规则回滚成功, ruleId: {}, 回滚到版本: {}", currentRule.getId(), targetVersion.getVersion());
    }

    @Transactional(readOnly = true)
    public List<TriageRuleVersion> getRuleVersions(Long ruleId) {
        log.info("获取规则历史版本, ruleId: {}", ruleId);

        if (!triageRuleRepository.existsById(ruleId)) {
            throw new IllegalArgumentException("规则不存在: " + ruleId);
        }

        return triageRuleVersionRepository.findByRuleIdOrderByVersionDesc(ruleId);
    }

    @Transactional(readOnly = true)
    public List<TriageRule> findMatchingRules(Long storeId, Integer age, String gender, Long appointmentId) {
        log.info("查找匹配的规则, storeId: {}, age: {}, gender: {}, appointmentId: {}",
                storeId, age, gender, appointmentId);

        List<TriageRule> allRules = triageRuleRepository.findMatchingRules(storeId, age, gender);

        Appointment appointment = null;
        if (appointmentId != null) {
            appointment = appointmentRepository.findById(appointmentId).orElse(null);
        }

        List<TriageRule> grayRules = new ArrayList<>();
        List<TriageRule> officialRules = new ArrayList<>();

        for (TriageRule rule : allRules) {
            if (rule.getStatus() == RuleStatus.GRAY) {
                if (appointment != null && checkGrayMatch(rule, appointment)) {
                    grayRules.add(rule);
                }
            } else if (rule.getStatus() == RuleStatus.OFFICIAL) {
                officialRules.add(rule);
            }
        }

        if (!grayRules.isEmpty()) {
            log.info("使用灰度规则, 规则数量: {}", grayRules.size());
            return grayRules;
        }

        log.info("使用正式规则, 规则数量: {}", officialRules.size());
        return officialRules;
    }

    public boolean checkGrayMatch(TriageRule grayRule, Appointment appointment) {
        log.debug("检查灰度规则匹配, ruleId: {}, appointmentId: {}", grayRule.getId(), appointment.getId());

        if (grayRule.getGraySources() != null && !grayRule.getGraySources().isEmpty()) {
            AppointmentSource source = appointment.getSource();
            if (!grayRule.getGraySources().contains(source)) {
                log.debug("预约来源不匹配, source: {}, allowedSources: {}", source, grayRule.getGraySources());
                return false;
            }
        }

        if (grayRule.getGrayProjects() != null && !grayRule.getGrayProjects().isEmpty()) {
            ConsultationType consultationType = appointment.getConsultationType();
            if (!grayRule.getGrayProjects().contains(consultationType)) {
                log.debug("咨询类型不匹配, consultationType: {}, allowedProjects: {}",
                        consultationType, grayRule.getGrayProjects());
                return false;
            }
        }

        if (grayRule.getGrayPercentage() != null && grayRule.getGrayPercentage() > 0) {
            int hash = Math.abs(appointment.getId().hashCode());
            int mod = hash % 100;
            if (mod >= grayRule.getGrayPercentage()) {
                log.debug("灰度百分比不命中, hash: {}, mod: {}, percentage: {}",
                        hash, mod, grayRule.getGrayPercentage());
                return false;
            }
        }

        log.debug("灰度规则匹配成功, ruleId: {}, appointmentId: {}", grayRule.getId(), appointment.getId());
        return true;
    }

    private void saveRuleVersion(TriageRule rule, String description) {
        TriageRuleVersion version = TriageRuleVersion.builder()
                .ruleId(rule.getId())
                .ruleCode(rule.getRuleCode())
                .version(rule.getVersion())
                .name(rule.getName())
                .storeId(rule.getStoreId())
                .consultationType(rule.getConsultationType())
                .departmentType(rule.getDepartmentType())
                .keywords(rule.getKeywords() != null ? new ArrayList<>(rule.getKeywords()) : null)
                .priority(rule.getPriority())
                .minAge(rule.getMinAge())
                .maxAge(rule.getMaxAge())
                .gender(rule.getGender())
                .isEnabled(rule.getIsEnabled())
                .status(rule.getStatus())
                .createdBy(description)
                .description(description)
                .build();

        triageRuleVersionRepository.save(version);
        log.debug("保存规则版本, ruleId: {}, version: {}", rule.getId(), rule.getVersion());
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
        saveRuleVersion(rule, "启用规则");
        log.info("规则启用成功, ruleId: {}", ruleId);
    }

    @Transactional
    public void disableRule(Long ruleId) {
        log.info("禁用规则, ruleId: {}", ruleId);

        TriageRule rule = triageRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("规则不存在: " + ruleId));

        rule.setIsEnabled(false);
        triageRuleRepository.save(rule);
        saveRuleVersion(rule, "禁用规则");
        log.info("规则禁用成功, ruleId: {}", ruleId);
    }

    @Transactional
    public void deleteRule(Long ruleId) {
        log.info("删除规则, ruleId: {}", ruleId);

        TriageRule rule = triageRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("规则不存在: " + ruleId));

        rule.setIsEnabled(false);
        rule.setStatus(RuleStatus.ARCHIVED);
        triageRuleRepository.save(rule);
        saveRuleVersion(rule, "删除/归档规则");
        log.info("规则删除成功, ruleId: {}", ruleId);
    }
}
