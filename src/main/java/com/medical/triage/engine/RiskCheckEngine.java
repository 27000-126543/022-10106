package com.medical.triage.engine;

import com.medical.triage.config.TriageConfig;
import com.medical.triage.dto.response.RiskCheckResponse;
import com.medical.triage.entity.QuestionnaireAnswer;
import com.medical.triage.entity.RiskKeyword;
import com.medical.triage.enums.RiskLevel;
import com.medical.triage.repository.QuestionnaireAnswerRepository;
import com.medical.triage.repository.RiskKeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskCheckEngine {

    private final RiskKeywordRepository riskKeywordRepository;
    private final TriageConfig triageConfig;
    private final QuestionnaireAnswerRepository questionnaireAnswerRepository;

    public RiskCheckResponse checkRisk(Long storeId, Long appointmentId, String content) {
        log.info("开始风险检查, appointmentId: {}, storeId: {}", appointmentId, storeId);

        List<String> matchedKeywords = new ArrayList<>();
        Set<RiskKeyword> matchedRiskKeywords = new HashSet<>();

        if (content != null && !content.trim().isEmpty()) {
            List<RiskKeyword> dbMatchedKeywords = riskKeywordRepository.findMatchingKeywords(content);
            matchedRiskKeywords.addAll(dbMatchedKeywords);
            matchedKeywords.addAll(dbMatchedKeywords.stream()
                    .map(RiskKeyword::getKeyword)
                    .collect(Collectors.toList()));

            List<String> configHighRiskKeywords = triageConfig.getRisk().getHighRiskKeywords();
            if (configHighRiskKeywords != null) {
                for (String keyword : configHighRiskKeywords) {
                    if (content.contains(keyword) && !matchedKeywords.contains(keyword)) {
                        matchedKeywords.add(keyword);
                        RiskKeyword configRiskKeyword = RiskKeyword.builder()
                                .keyword(keyword)
                                .riskLevel(RiskLevel.HIGH)
                                .needDoctorAssessment(true)
                                .description("配置高风险关键词")
                                .build();
                        matchedRiskKeywords.add(configRiskKeyword);
                    }
                }
            }
        }

        List<QuestionnaireAnswer> answers = questionnaireAnswerRepository.findByAppointmentId(appointmentId);
        for (QuestionnaireAnswer answer : answers) {
            String answerText = answer.getAnswerText();
            if (answerText != null && !answerText.trim().isEmpty()) {
                List<RiskKeyword> answerMatched = riskKeywordRepository.findMatchingKeywords(answerText);
                for (RiskKeyword keyword : answerMatched) {
                    if (!matchedKeywords.contains(keyword.getKeyword())) {
                        matchedKeywords.add(keyword.getKeyword());
                        matchedRiskKeywords.add(keyword);
                    }
                }

                List<String> configKeywords = triageConfig.getRisk().getHighRiskKeywords();
                if (configKeywords != null) {
                    for (String keyword : configKeywords) {
                        if (answerText.contains(keyword) && !matchedKeywords.contains(keyword)) {
                            matchedKeywords.add(keyword);
                            RiskKeyword configRiskKeyword = RiskKeyword.builder()
                                    .keyword(keyword)
                                    .riskLevel(RiskLevel.HIGH)
                                    .needDoctorAssessment(true)
                                    .description("配置高风险关键词")
                                    .build();
                            matchedRiskKeywords.add(configRiskKeyword);
                        }
                    }
                }
            }
        }

        RiskLevel riskLevel = calculateRiskLevel(matchedKeywords);
        boolean needDoctor = needDoctorAssessment(matchedKeywords);

        boolean passed = riskLevel != RiskLevel.EXTREME;

        String warningMessage = null;
        if (!matchedKeywords.isEmpty()) {
            List<String> highRiskWords = matchedRiskKeywords.stream()
                    .filter(k -> k.getRiskLevel() == RiskLevel.HIGH || k.getRiskLevel() == RiskLevel.EXTREME)
                    .map(RiskKeyword::getKeyword)
                    .collect(Collectors.toList());
            if (!highRiskWords.isEmpty()) {
                warningMessage = "检测到高风险关键词: " + String.join(", ", highRiskWords);
            } else {
                warningMessage = "检测到风险关键词: " + String.join(", ", matchedKeywords);
            }
        }

        log.info("风险检查完成, appointmentId: {}, riskLevel: {}, matchedKeywords: {}, needDoctor: {}",
                appointmentId, riskLevel, matchedKeywords, needDoctor);

        return RiskCheckResponse.builder()
                .passed(passed)
                .riskLevel(riskLevel)
                .matchedKeywords(matchedKeywords)
                .needDoctorAssessment(needDoctor)
                .warningMessage(warningMessage)
                .build();
    }

    public boolean needDoctorAssessment(List<String> matchedKeywords) {
        if (matchedKeywords == null || matchedKeywords.isEmpty()) {
            return false;
        }

        List<String> autoUpgradeKeywords = triageConfig.getRisk().getAutoUpgradeToDoctorKeywords();
        if (autoUpgradeKeywords != null) {
            for (String keyword : matchedKeywords) {
                if (autoUpgradeKeywords.contains(keyword)) {
                    return true;
                }
            }
        }

        for (String keyword : matchedKeywords) {
            Optional<RiskKeyword> riskKeywordOpt = riskKeywordRepository.findByKeyword(keyword);
            if (riskKeywordOpt.isPresent() && riskKeywordOpt.get().getNeedDoctorAssessment()) {
                return true;
            }
        }

        return false;
    }

    public RiskLevel calculateRiskLevel(List<String> matchedKeywords) {
        if (matchedKeywords == null || matchedKeywords.isEmpty()) {
            return RiskLevel.LOW;
        }

        RiskLevel maxLevel = RiskLevel.LOW;

        for (String keyword : matchedKeywords) {
            RiskLevel currentLevel = RiskLevel.LOW;

            List<String> highRiskKeywords = triageConfig.getRisk().getHighRiskKeywords();
            if (highRiskKeywords != null && highRiskKeywords.contains(keyword)) {
                currentLevel = RiskLevel.HIGH;
            }

            Optional<RiskKeyword> riskKeywordOpt = riskKeywordRepository.findByKeyword(keyword);
            if (riskKeywordOpt.isPresent()) {
                currentLevel = riskKeywordOpt.get().getRiskLevel();
            }

            if (currentLevel.ordinal() > maxLevel.ordinal()) {
                maxLevel = currentLevel;
            }
        }

        int highRiskCount = 0;
        List<String> highRiskKeywords = triageConfig.getRisk().getHighRiskKeywords();
        for (String keyword : matchedKeywords) {
            boolean isHighRisk = false;
            if (highRiskKeywords != null && highRiskKeywords.contains(keyword)) {
                isHighRisk = true;
            }
            Optional<RiskKeyword> riskKeywordOpt = riskKeywordRepository.findByKeyword(keyword);
            if (riskKeywordOpt.isPresent()) {
                RiskLevel level = riskKeywordOpt.get().getRiskLevel();
                if (level == RiskLevel.HIGH || level == RiskLevel.EXTREME) {
                    isHighRisk = true;
                }
            }
            if (isHighRisk) {
                highRiskCount++;
            }
        }

        if (highRiskCount >= 3) {
            return RiskLevel.EXTREME;
        }

        return maxLevel;
    }
}
