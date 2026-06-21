package com.medical.triage.engine;

import com.medical.triage.dto.response.RiskCheckResponse;
import com.medical.triage.entity.*;
import com.medical.triage.enums.ConsultationType;
import com.medical.triage.enums.DepartmentType;
import com.medical.triage.enums.RiskLevel;
import com.medical.triage.enums.TriageStatus;
import com.medical.triage.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TriageEngine {

    private final TriageRuleRepository triageRuleRepository;
    private final DepartmentRepository departmentRepository;
    private final ConsultantGroupRepository consultantGroupRepository;
    private final RiskCheckEngine riskCheckEngine;
    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final QuestionnaireAnswerRepository questionnaireAnswerRepository;

    @Transactional
    public TriageResult executeTriage(Long appointmentId) {
        log.info("开始执行分诊, appointmentId: {}", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("预约不存在: " + appointmentId));

        Customer customer = customerRepository.findById(appointment.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("顾客不存在: " + appointment.getCustomerId()));

        List<QuestionnaireAnswer> answers = questionnaireAnswerRepository.findByAppointmentId(appointmentId);

        ConsultationType consultationType = determineConsultationType(appointment.getNotes(), answers);
        log.info("确定面诊类型, appointmentId: {}, consultationType: {}", appointmentId, consultationType);

        StringBuilder contentBuilder = new StringBuilder();
        if (appointment.getNotes() != null) {
            contentBuilder.append(appointment.getNotes());
        }
        for (QuestionnaireAnswer answer : answers) {
            if (answer.getAnswerText() != null) {
                contentBuilder.append(" ").append(answer.getAnswerText());
            }
        }
        String content = contentBuilder.toString();

        RiskCheckResponse riskCheckResponse = riskCheckEngine.checkRisk(
                appointment.getStoreId(), appointmentId, content);
        log.info("风险检查结果, appointmentId: {}, riskLevel: {}, needDoctor: {}",
                appointmentId, riskCheckResponse.getRiskLevel(), riskCheckResponse.getNeedDoctorAssessment());

        List<String> keywords = riskCheckResponse.getMatchedKeywords();

        Department department = matchDepartment(
                appointment.getStoreId(), consultationType, keywords, customer);
        log.info("匹配科室结果, appointmentId: {}, departmentId: {}, departmentName: {}",
                appointmentId, department.getId(), department.getName());

        ConsultantGroup consultantGroup = matchConsultantGroup(
                department.getId(), riskCheckResponse.getRiskLevel(), riskCheckResponse.getNeedDoctorAssessment());
        log.info("匹配咨询师组结果, appointmentId: {}, groupId: {}, groupName: {}",
                appointmentId, consultantGroup.getId(), consultantGroup.getName());

        TriageResult triageResult = TriageResult.builder()
                .appointmentId(appointmentId)
                .customerId(appointment.getCustomerId())
                .storeId(appointment.getStoreId())
                .departmentId(department.getId())
                .consultantGroupId(consultantGroup.getId())
                .consultationType(consultationType)
                .riskLevel(riskCheckResponse.getRiskLevel())
                .triageTime(LocalDateTime.now())
                .isReassigned(false)
                .status(TriageStatus.TRIAGED)
                .build();

        log.info("分诊完成, appointmentId: {}, triageResultId: {}", appointmentId, triageResult.getId());

        return triageResult;
    }

    public Department matchDepartment(Long storeId, ConsultationType consultationType,
                                       List<String> keywords, Customer customer) {
        log.info("开始匹配科室, storeId: {}, consultationType: {}, keywords: {}",
                storeId, consultationType, keywords);

        List<TriageRule> rules = triageRuleRepository.findMatchingRules(
                storeId, customer.getAge(), customer.getGender());
        log.debug("获取到 {} 条分诊规则", rules.size());

        Map<DepartmentType, Integer> departmentScores = new HashMap<>();

        for (TriageRule rule : rules) {
            if (rule.getConsultationType() != null && rule.getConsultationType() == consultationType) {
                departmentScores.merge(rule.getDepartmentType(), 10, Integer::sum);
                log.debug("规则 {} 匹配面诊类型，科室 {} 加10分", rule.getName(), rule.getDepartmentType());
            }

            if (rule.getKeywords() != null && keywords != null) {
                long matchCount = keywords.stream()
                        .filter(k -> rule.getKeywords().contains(k))
                        .count();
                if (matchCount > 0) {
                    int score = (int) matchCount * 5;
                    departmentScores.merge(rule.getDepartmentType(), score, Integer::sum);
                    log.debug("规则 {} 匹配 {} 个关键词，科室 {} 加{}分",
                            rule.getName(), matchCount, rule.getDepartmentType(), score);
                }
            }
        }

        DepartmentType defaultDepartmentType = mapConsultationTypeToDepartment(consultationType);
        departmentScores.merge(defaultDepartmentType, 5, Integer::sum);
        log.debug("面诊类型 {} 默认映射到科室 {}，加5分", consultationType, defaultDepartmentType);

        if (keywords != null && !keywords.isEmpty()) {
            for (String keyword : keywords) {
                DepartmentType inferredType = inferDepartmentFromKeyword(keyword);
                if (inferredType != null) {
                    departmentScores.merge(inferredType, 3, Integer::sum);
                    log.debug("关键词 {} 推断科室 {}，加3分", keyword, inferredType);
                }
            }
        }

        DepartmentType bestDepartmentType = departmentScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(defaultDepartmentType);

        log.info("科室评分结果: {}, 最优科室: {}", departmentScores, bestDepartmentType);

        List<Department> departments = departmentRepository.findByStoreIdAndDepartmentType(storeId, bestDepartmentType);
        if (departments.isEmpty()) {
            departments = departmentRepository.findByStoreId(storeId);
            if (departments.isEmpty()) {
                throw new IllegalStateException("门店 " + storeId + " 没有可用科室");
            }
            log.warn("门店 {} 没有 {} 类型的科室，使用默认科室", storeId, bestDepartmentType);
        }

        Department bestDepartment = departments.stream()
                .filter(d -> d.getStatus() == 1)
                .max(Comparator.comparingInt(d -> d.getCapacity() != null ? d.getCapacity() : 0))
                .orElse(departments.get(0));

        log.info("匹配到科室: id={}, name={}, type={}",
                bestDepartment.getId(), bestDepartment.getName(), bestDepartment.getDepartmentType());

        return bestDepartment;
    }

    public ConsultantGroup matchConsultantGroup(Long departmentId, RiskLevel riskLevel, boolean needDoctor) {
        log.info("开始匹配咨询师组, departmentId: {}, riskLevel: {}, needDoctor: {}",
                departmentId, riskLevel, needDoctor);

        List<ConsultantGroup> groups = consultantGroupRepository.findByDepartmentId(departmentId);
        if (groups.isEmpty()) {
            throw new IllegalStateException("科室 " + departmentId + " 没有咨询师组");
        }

        List<ConsultantGroup> activeGroups = groups.stream()
                .filter(g -> g.getStatus() == 1)
                .collect(Collectors.toList());
        if (activeGroups.isEmpty()) {
            throw new IllegalStateException("科室 " + departmentId + " 没有可用的咨询师组");
        }

        Map<Long, Integer> groupScores = new HashMap<>();

        for (ConsultantGroup group : activeGroups) {
            int score = 0;

            if (needDoctor) {
                List<String> tags = group.getSpecialtyTags();
                if (tags != null && tags.stream().anyMatch(t ->
                        t.contains("医生") || t.contains("主任") || t.contains("医师") || t.contains("medical"))) {
                    score += 20;
                    log.debug("咨询师组 {} 包含医生资质标签，加20分", group.getName());
                }
            }

            if (riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.EXTREME) {
                List<String> tags = group.getSpecialtyTags();
                if (tags != null && tags.stream().anyMatch(t ->
                        t.contains("高风险") || t.contains("重症") || t.contains("专家"))) {
                    score += 15;
                    log.debug("咨询师组 {} 包含高风险处理标签，加15分", group.getName());
                }
            }

            if (group.getMemberCount() != null) {
                score += Math.min(group.getMemberCount(), 10);
                log.debug("咨询师组 {} 成员数 {}，加{}分", group.getName(), group.getMemberCount(), Math.min(group.getMemberCount(), 10));
            }

            groupScores.put(group.getId(), score);
            log.debug("咨询师组 {} 总分: {}", group.getName(), score);
        }

        Long bestGroupId = groupScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(activeGroups.get(0).getId());

        ConsultantGroup bestGroup = activeGroups.stream()
                .filter(g -> g.getId().equals(bestGroupId))
                .findFirst()
                .orElse(activeGroups.get(0));

        log.info("匹配到咨询师组: id={}, name={}, score={}",
                bestGroup.getId(), bestGroup.getName(), groupScores.get(bestGroupId));

        return bestGroup;
    }

    public ConsultationType determineConsultationType(String notes, List<QuestionnaireAnswer> answers) {
        log.info("开始确定面诊类型, notes: {}, answersCount: {}",
                notes != null ? notes.length() : 0, answers != null ? answers.size() : 0);

        Map<ConsultationType, Integer> typeScores = new EnumMap<>(ConsultationType.class);
        for (ConsultationType type : ConsultationType.values()) {
            typeScores.put(type, 0);
        }

        if (notes != null && !notes.trim().isEmpty()) {
            String lowerNotes = notes.toLowerCase();

            if (containsAny(lowerNotes, "手术", "整形", "开刀", "surgery", "operation")) {
                typeScores.merge(ConsultationType.SURGERY_CONSULTATION, 10, Integer::sum);
            }
            if (containsAny(lowerNotes, "皮肤", "美容", "护肤", "祛斑", "祛痘", "skin", "dermatology")) {
                typeScores.merge(ConsultationType.SKIN_CARE_CONSULTATION, 10, Integer::sum);
            }
            if (containsAny(lowerNotes, "注射", "玻尿酸", "肉毒素", "填充", "injection", "filler")) {
                typeScores.merge(ConsultationType.INJECTION_CONSULTATION, 10, Integer::sum);
            }
            if (containsAny(lowerNotes, "非手术", "无创", "激光", "光子", "non-surgery", "laser")) {
                typeScores.merge(ConsultationType.NON_SURGERY_CONSULTATION, 10, Integer::sum);
            }
            if (containsAny(lowerNotes, "综合", "全面", "套餐", "comprehensive")) {
                typeScores.merge(ConsultationType.COMPREHENSIVE, 10, Integer::sum);
            }
        }

        if (answers != null && !answers.isEmpty()) {
            for (QuestionnaireAnswer answer : answers) {
                String answerText = answer.getAnswerText();
                if (answerText == null) continue;

                String lowerAnswer = answerText.toLowerCase();

                if (containsAny(lowerAnswer, "手术", "整形", "开刀", "surgery", "operation")) {
                    typeScores.merge(ConsultationType.SURGERY_CONSULTATION, 5, Integer::sum);
                }
                if (containsAny(lowerAnswer, "皮肤", "美容", "护肤", "祛斑", "祛痘", "skin", "dermatology")) {
                    typeScores.merge(ConsultationType.SKIN_CARE_CONSULTATION, 5, Integer::sum);
                }
                if (containsAny(lowerAnswer, "注射", "玻尿酸", "肉毒素", "填充", "injection", "filler")) {
                    typeScores.merge(ConsultationType.INJECTION_CONSULTATION, 5, Integer::sum);
                }
                if (containsAny(lowerAnswer, "非手术", "无创", "激光", "光子", "non-surgery", "laser")) {
                    typeScores.merge(ConsultationType.NON_SURGERY_CONSULTATION, 5, Integer::sum);
                }
                if (containsAny(lowerAnswer, "综合", "全面", "套餐", "comprehensive")) {
                    typeScores.merge(ConsultationType.COMPREHENSIVE, 5, Integer::sum);
                }
            }
        }

        ConsultationType bestType = typeScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(ConsultationType.COMPREHENSIVE);

        int maxScore = typeScores.get(bestType);
        if (maxScore == 0) {
            bestType = ConsultationType.COMPREHENSIVE;
            log.info("未检测到明确的面诊类型关键词，默认使用: {}", bestType);
        } else {
            log.info("面诊类型评分: {}, 确定类型: {}", typeScores, bestType);
        }

        return bestType;
    }

    private DepartmentType mapConsultationTypeToDepartment(ConsultationType consultationType) {
        return switch (consultationType) {
            case SURGERY_CONSULTATION -> DepartmentType.PLASTIC_SURGERY;
            case NON_SURGERY_CONSULTATION, SKIN_CARE_CONSULTATION -> DepartmentType.DERMATOLOGY;
            case INJECTION_CONSULTATION -> DepartmentType.INJECTION;
            case COMPREHENSIVE -> DepartmentType.PLASTIC_SURGERY;
        };
    }

    private DepartmentType inferDepartmentFromKeyword(String keyword) {
        String lowerKeyword = keyword.toLowerCase();

        if (containsAny(lowerKeyword, "眼", "鼻", "胸", "轮廓", "吸脂", "手术", "整形")) {
            return DepartmentType.PLASTIC_SURGERY;
        }
        if (containsAny(lowerKeyword, "皮肤", "斑", "痘", "疤", "痕", "激光", "光子", "嫩肤")) {
            return DepartmentType.DERMATOLOGY;
        }
        if (containsAny(lowerKeyword, "注射", "玻尿酸", "肉毒素", "填充", "瘦脸", "除皱")) {
            return DepartmentType.INJECTION;
        }
        if (containsAny(lowerKeyword, "麻醉", "无痛")) {
            return DepartmentType.ANESTHESIOLOGY;
        }
        if (containsAny(lowerKeyword, "牙", "齿", "口腔")) {
            return DepartmentType.DENTAL;
        }
        if (containsAny(lowerKeyword, "中医", "针灸", "调理", "养生")) {
            return DepartmentType.TRADITIONAL_CHINESE_MEDICINE;
        }

        return null;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
