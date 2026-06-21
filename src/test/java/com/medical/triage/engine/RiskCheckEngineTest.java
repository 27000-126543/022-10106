package com.medical.triage.engine;

import com.medical.triage.config.TriageConfig;
import com.medical.triage.dto.response.RiskCheckResponse;
import com.medical.triage.entity.RiskKeyword;
import com.medical.triage.enums.RiskLevel;
import com.medical.triage.repository.QuestionnaireAnswerRepository;
import com.medical.triage.repository.RiskKeywordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskCheckEngineTest {

    @Mock
    private RiskKeywordRepository riskKeywordRepository;

    @Mock
    private QuestionnaireAnswerRepository questionnaireAnswerRepository;

    @Mock
    private TriageConfig triageConfig;

    @InjectMocks
    private RiskCheckEngine riskCheckEngine;

    @BeforeEach
    void setUp() {
        TriageConfig.Risk risk = new TriageConfig.Risk();
        risk.setHighRiskKeywords(Arrays.asList("瘢痕体质", "严重心脏病", "高血压"));
        risk.setAutoUpgradeToDoctorKeywords(Arrays.asList("瘢痕体质", "严重心脏病"));
        when(triageConfig.getRisk()).thenReturn(risk);
    }

    @Test
    void testCheckRisk_NoRisk() {
        when(riskKeywordRepository.findAll()).thenReturn(Collections.emptyList());
        when(questionnaireAnswerRepository.findByAppointmentId(1L)).thenReturn(Collections.emptyList());

        RiskCheckResponse response = riskCheckEngine.checkRisk(1L, 1L, "想做双眼皮手术");

        assertTrue(response.isPassed());
        assertEquals(RiskLevel.LOW, response.getRiskLevel());
        assertFalse(response.isNeedDoctorAssessment());
        assertTrue(response.getMatchedKeywords().isEmpty());
    }

    @Test
    void testCheckRisk_HighRiskFromContent() {
        RiskKeyword keyword = RiskKeyword.builder()
                .keyword("瘢痕体质")
                .riskLevel(RiskLevel.HIGH)
                .needDoctorAssessment(true)
                .build();
        when(riskKeywordRepository.findAll()).thenReturn(Collections.singletonList(keyword));
        when(questionnaireAnswerRepository.findByAppointmentId(1L)).thenReturn(Collections.emptyList());

        RiskCheckResponse response = riskCheckEngine.checkRisk(1L, 1L, "我是瘢痕体质，想做双眼皮");

        assertFalse(response.isPassed());
        assertEquals(RiskLevel.HIGH, response.getRiskLevel());
        assertTrue(response.isNeedDoctorAssessment());
        assertTrue(response.getMatchedKeywords().contains("瘢痕体质"));
        assertNotNull(response.getWarningMessage());
    }

    @Test
    void testCheckRisk_ExtremeRisk() {
        RiskKeyword keyword1 = RiskKeyword.builder()
                .keyword("严重心脏病")
                .riskLevel(RiskLevel.EXTREME)
                .needDoctorAssessment(true)
                .build();
        RiskKeyword keyword2 = RiskKeyword.builder()
                .keyword("高血压")
                .riskLevel(RiskLevel.HIGH)
                .needDoctorAssessment(true)
                .build();
        when(riskKeywordRepository.findAll()).thenReturn(Arrays.asList(keyword1, keyword2));
        when(questionnaireAnswerRepository.findByAppointmentId(1L)).thenReturn(Collections.emptyList());

        RiskCheckResponse response = riskCheckEngine.checkRisk(1L, 1L, "我有严重心脏病和高血压，想做隆胸手术");

        assertFalse(response.isPassed());
        assertEquals(RiskLevel.EXTREME, response.getRiskLevel());
        assertTrue(response.isNeedDoctorAssessment());
        assertEquals(2, response.getMatchedKeywords().size());
    }

    @Test
    void testNeedDoctorAssessment() {
        assertTrue(riskCheckEngine.needDoctorAssessment(Collections.singletonList("瘢痕体质")));
        assertTrue(riskCheckEngine.needDoctorAssessment(Collections.singletonList("严重心脏病")));
        assertFalse(riskCheckEngine.needDoctorAssessment(Collections.singletonList("过敏体质")));
        assertFalse(riskCheckEngine.needDoctorAssessment(Collections.emptyList()));
    }

    @Test
    void testCalculateRiskLevel() {
        assertEquals(RiskLevel.LOW, riskCheckEngine.calculateRiskLevel(Collections.emptyList()));
        assertEquals(RiskLevel.MEDIUM, riskCheckEngine.calculateRiskLevel(Collections.singletonList("过敏体质")));
        assertEquals(RiskLevel.HIGH, riskCheckEngine.calculateRiskLevel(Collections.singletonList("高血压")));
        assertEquals(RiskLevel.EXTREME, riskCheckEngine.calculateRiskLevel(Collections.singletonList("严重心脏病")));
    }
}
