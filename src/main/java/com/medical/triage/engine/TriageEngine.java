package com.medical.triage.engine;

import com.medical.triage.dto.response.RiskCheckResponse;
import com.medical.triage.entity.*;
import com.medical.triage.enums.ConsultationType;
import com.medical.triage.enums.DepartmentType;
import com.medical.triage.enums.RiskLevel;
import com.medical.triage.enums.TriageStatus;
import com.medical.triage.repository.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
    private final TriageExplanationRepository triageExplanationRepository;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentMatchResult {
        private Department department;
        private Map<DepartmentType, Integer> departmentScores;
        private List<String> scoreDetails;
        private List<String> matchedRules;
        private List<String> matchedKeywords;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsultantGroupMatchResult {
        private ConsultantGroup group;
        private Map<Long, Integer> groupScores;
        private List<String> scoreDetails;
    }

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

        DepartmentMatchResult departmentResult = matchDepartment(
                appointment.getStoreId(), consultationType, keywords, customer);
        log.info("匹配科室结果, appointmentId: {}, departmentId: {}, departmentName: {}",
                appointmentId, departmentResult.getDepartment().getId(), departmentResult.getDepartment().getName());

        ConsultantGroupMatchResult groupResult = matchConsultantGroup(
                departmentResult.getDepartment().getId(), riskCheckResponse.getRiskLevel(), riskCheckResponse.getNeedDoctorAssessment());
        log.info("匹配咨询师组结果, appointmentId: {}, groupId: {}, groupName: {}",
                appointmentId, groupResult.getGroup().getId(), groupResult.getGroup().getName());

        TriageResult triageResult = TriageResult.builder()
                .appointmentId(appointmentId)
                .customerId(appointment.getCustomerId())
                .storeId(appointment.getStoreId())
                .departmentId(departmentResult.getDepartment().getId())
                .consultantGroupId(groupResult.getGroup().getId())
                .consultationType(consultationType)
                .riskLevel(riskCheckResponse.getRiskLevel())
                .triageTime(LocalDateTime.now())
                .isReassigned(false)
                .status(TriageStatus.TRIAGED)
                .build();

        TriageExplanation explanation = generateExplanation(
                appointmentId,
                triageResult,
                appointment.getNotes(),
                answers,
                keywords,
                riskCheckResponse,
                departmentResult,
                groupResult,
                customer);

        TriageExplanation savedExplanation = triageExplanationRepository.save(explanation);
        triageResult.setExplanationId(savedExplanation.getId());

        log.info("分诊完成, appointmentId: {}, triageResultId: {}, explanationId: {}",
                appointmentId, triageResult.getId(), savedExplanation.getId());

        return triageResult;
    }

    public DepartmentMatchResult matchDepartment(Long storeId, ConsultationType consultationType,
                                                  List<String> keywords, Customer customer) {
        log.info("开始匹配科室, storeId: {}, consultationType: {}, keywords: {}",
                storeId, consultationType, keywords);

        List<TriageRule> rules = triageRuleRepository.findMatchingRules(
                storeId, customer.getAge(), customer.getGender());
        log.debug("获取到 {} 条分诊规则", rules.size());

        Map<DepartmentType, Integer> departmentScores = new HashMap<>();
        List<String> scoreDetails = new ArrayList<>();
        List<String> matchedRules = new ArrayList<>();
        List<String> matchedKeywords = new ArrayList<>();

        for (TriageRule rule : rules) {
            boolean ruleMatched = false;

            if (rule.getConsultationType() != null && rule.getConsultationType() == consultationType) {
                departmentScores.merge(rule.getDepartmentType(), 10, Integer::sum);
                String detail = String.format("规则[%s]匹配面诊类型[%s]，科室[%s]加10分",
                        rule.getName(), consultationType, rule.getDepartmentType());
                scoreDetails.add(detail);
                log.debug(detail);
                ruleMatched = true;
            }

            if (rule.getKeywords() != null && keywords != null) {
                List<String> ruleMatchedKeywords = keywords.stream()
                        .filter(k -> rule.getKeywords().contains(k))
                        .collect(Collectors.toList());
                if (!ruleMatchedKeywords.isEmpty()) {
                    int score = ruleMatchedKeywords.size() * 5;
                    departmentScores.merge(rule.getDepartmentType(), score, Integer::sum);
                    String detail = String.format("规则[%s]匹配关键词%s，科室[%s]加%d分",
                            rule.getName(), ruleMatchedKeywords, rule.getDepartmentType(), score);
                    scoreDetails.add(detail);
                    log.debug(detail);
                    matchedKeywords.addAll(ruleMatchedKeywords);
                    ruleMatched = true;
                }
            }

            if (ruleMatched) {
                matchedRules.add(rule.getName() + "(ID:" + rule.getId() + ")");
            }
        }

        DepartmentType defaultDepartmentType = mapConsultationTypeToDepartment(consultationType);
        departmentScores.merge(defaultDepartmentType, 5, Integer::sum);
        String defaultDetail = String.format("面诊类型[%s]默认映射到科室[%s]，加5分",
                consultationType, defaultDepartmentType);
        scoreDetails.add(defaultDetail);
        log.debug(defaultDetail);

        if (keywords != null && !keywords.isEmpty()) {
            for (String keyword : keywords) {
                DepartmentType inferredType = inferDepartmentFromKeyword(keyword);
                if (inferredType != null) {
                    departmentScores.merge(inferredType, 3, Integer::sum);
                    String detail = String.format("关键词[%s]推断科室[%s]，加3分", keyword, inferredType);
                    scoreDetails.add(detail);
                    log.debug(detail);
                    if (!matchedKeywords.contains(keyword)) {
                        matchedKeywords.add(keyword);
                    }
                }
            }
        }

        DepartmentType bestDepartmentType = departmentScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(defaultDepartmentType);

        String finalDetail = String.format("科室评分结果: %s, 最优科室: %s", departmentScores, bestDepartmentType);
        scoreDetails.add(finalDetail);
        log.info(finalDetail);

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

        return DepartmentMatchResult.builder()
                .department(bestDepartment)
                .departmentScores(departmentScores)
                .scoreDetails(scoreDetails)
                .matchedRules(matchedRules)
                .matchedKeywords(matchedKeywords)
                .build();
    }

    public ConsultantGroupMatchResult matchConsultantGroup(Long departmentId, RiskLevel riskLevel, boolean needDoctor) {
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
        List<String> scoreDetails = new ArrayList<>();

        for (ConsultantGroup group : activeGroups) {
            int score = 0;

            if (needDoctor) {
                List<String> tags = group.getSpecialtyTags();
                if (tags != null && tags.stream().anyMatch(t ->
                        t.contains("医生") || t.contains("主任") || t.contains("医师") || t.contains("medical"))) {
                    score += 20;
                    String detail = String.format("咨询师组[%s]包含医生资质标签，加20分", group.getName());
                    scoreDetails.add(detail);
                    log.debug(detail);
                }
            }

            if (riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.EXTREME) {
                List<String> tags = group.getSpecialtyTags();
                if (tags != null && tags.stream().anyMatch(t ->
                        t.contains("高风险") || t.contains("重症") || t.contains("专家"))) {
                    score += 15;
                    String detail = String.format("咨询师组[%s]包含高风险处理标签，加15分", group.getName());
                    scoreDetails.add(detail);
                    log.debug(detail);
                }
            }

            if (group.getMemberCount() != null) {
                int capacityScore = Math.min(group.getMemberCount(), 10);
                score += capacityScore;
                String detail = String.format("咨询师组[%s]成员数%d，加%d分",
                        group.getName(), group.getMemberCount(), capacityScore);
                scoreDetails.add(detail);
                log.debug(detail);
            }

            groupScores.put(group.getId(), score);
            String totalDetail = String.format("咨询师组[%s]总分: %d", group.getName(), score);
            scoreDetails.add(totalDetail);
            log.debug(totalDetail);
        }

        Long bestGroupId = groupScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(activeGroups.get(0).getId());

        ConsultantGroup bestGroup = activeGroups.stream()
                .filter(g -> g.getId().equals(bestGroupId))
                .findFirst()
                .orElse(activeGroups.get(0));

        String finalDetail = String.format("匹配到咨询师组: id=%d, name=%s, score=%d",
                bestGroup.getId(), bestGroup.getName(), groupScores.get(bestGroupId));
        scoreDetails.add(finalDetail);
        log.info(finalDetail);

        return ConsultantGroupMatchResult.builder()
                .group(bestGroup)
                .groupScores(groupScores)
                .scoreDetails(scoreDetails)
                .build();
    }

    private TriageExplanation generateExplanation(Long appointmentId, TriageResult triageResult,
                                                   String notes, List<QuestionnaireAnswer> answers,
                                                   List<String> riskKeywords, RiskCheckResponse riskCheckResponse,
                                                   DepartmentMatchResult departmentResult,
                                                   ConsultantGroupMatchResult groupResult,
                                                   Customer customer) {
        log.info("生成分诊解释, appointmentId: {}", appointmentId);

        List<String> matchedAppointmentKeywords = new ArrayList<>();
        if (notes != null && !notes.trim().isEmpty()) {
            matchedAppointmentKeywords = extractKeywordsFromText(notes);
        }

        List<String> matchedQuestionAnswers = new ArrayList<>();
        if (answers != null && !answers.isEmpty()) {
            for (QuestionnaireAnswer answer : answers) {
                if (answer.getAnswerText() != null && !answer.getAnswerText().trim().isEmpty()) {
                    matchedQuestionAnswers.add(
                            "问题[" + answer.getQuestionId() + "]: " + answer.getAnswerText());
                }
            }
        }

        List<String> matchedRiskKeywords = new ArrayList<>();
        if (riskCheckResponse.getMatchedKeywords() != null) {
            matchedRiskKeywords = riskCheckResponse.getMatchedKeywords();
        }

        List<String> matchedRules = departmentResult.getMatchedRules();

        int overallScore = departmentResult.getDepartmentScores().values().stream()
                .max(Integer::compareTo)
                .orElse(0);

        StringBuilder explanationText = new StringBuilder();
        explanationText.append("### 分诊结果说明\n\n");
        explanationText.append(String.format("**顾客信息**: %s (年龄: %d, 性别: %s)\n\n",
                customer.getName(), customer.getAge(), customer.getGender()));

        explanationText.append("**科室评分详情**:\n");
        for (String detail : departmentResult.getScoreDetails()) {
            explanationText.append("- ").append(detail).append("\n");
        }
        explanationText.append("\n");

        explanationText.append("**咨询师组评分详情**:\n");
        for (String detail : groupResult.getScoreDetails()) {
            explanationText.append("- ").append(detail).append("\n");
        }
        explanationText.append("\n");

        explanationText.append("**命中信息**:\n");
        if (!departmentResult.getMatchedKeywords().isEmpty()) {
            explanationText.append("- 命中关键词: ").append(departmentResult.getMatchedKeywords()).append("\n");
        }
        if (!matchedRiskKeywords.isEmpty()) {
            explanationText.append("- 风险关键词: ").append(matchedRiskKeywords).append("\n");
        }
        if (!matchedRules.isEmpty()) {
            explanationText.append("- 命中规则: ").append(matchedRules).append("\n");
        }
        explanationText.append("\n");

        explanationText.append("**最终结果**:\n");
        explanationText.append(String.format("- 科室: %s (ID: %d)\n",
                departmentResult.getDepartment().getName(), departmentResult.getDepartment().getId()));
        explanationText.append(String.format("- 咨询师组: %s (ID: %d)\n",
                groupResult.getGroup().getName(), groupResult.getGroup().getId()));
        explanationText.append(String.format("- 风险等级: %s\n", riskCheckResponse.getRiskLevel()));
        explanationText.append(String.format("- 需医生评估: %s\n", riskCheckResponse.getNeedDoctorAssessment()));

        List<String> departmentScores = departmentResult.getDepartmentScores().entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue() + "分")
                .collect(Collectors.toList());

        List<String> groupScores = groupResult.getGroupScores().entrySet().stream()
                .map(e -> "组" + e.getKey() + ": " + e.getValue() + "分")
                .collect(Collectors.toList());

        return TriageExplanation.builder()
                .triageResultId(triageResult.getId())
                .appointmentId(appointmentId)
                .matchedAppointmentKeywords(matchedAppointmentKeywords)
                .matchedQuestionAnswers(matchedQuestionAnswers)
                .matchedRiskKeywords(matchedRiskKeywords)
                .matchedRules(matchedRules)
                .departmentScores(departmentScores)
                .groupScores(groupScores)
                .finalDepartmentId(departmentResult.getDepartment().getId())
                .finalDepartmentName(departmentResult.getDepartment().getName())
                .finalGroupId(groupResult.getGroup().getId())
                .finalGroupName(groupResult.getGroup().getName())
                .finalConsultationType(triageResult.getConsultationType())
                .finalRiskLevel(riskCheckResponse.getRiskLevel())
                .needDoctorAssessment(riskCheckResponse.getNeedDoctorAssessment())
                .overallScore(overallScore)
                .explanationText(explanationText.toString())
                .build();
    }

    private List<String> extractKeywordsFromText(String text) {
        List<String> keywords = new ArrayList<>();
        String[] candidateKeywords = {"手术", "整形", "皮肤", "注射", "激光", "光子", "祛斑", "祛痘",
                "玻尿酸", "肉毒素", "填充", "瘦脸", "除皱", "眼", "鼻", "胸", "轮廓", "吸脂"};
        for (String keyword : candidateKeywords) {
            if (text.contains(keyword)) {
                keywords.add(keyword);
            }
        }
        return keywords;
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
