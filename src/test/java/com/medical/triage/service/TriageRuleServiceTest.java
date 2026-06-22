package com.medical.triage.service;

import com.medical.triage.dto.request.RuleGrayRequest;
import com.medical.triage.dto.request.RulePublishRequest;
import com.medical.triage.dto.request.RuleRollbackRequest;
import com.medical.triage.entity.TriageRule;
import com.medical.triage.entity.TriageRuleVersion;
import com.medical.triage.enums.AppointmentSource;
import com.medical.triage.enums.ConsultationType;
import com.medical.triage.enums.DepartmentType;
import com.medical.triage.enums.RuleStatus;
import com.medical.triage.repository.TriageRuleRepository;
import com.medical.triage.repository.TriageRuleVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TriageRuleServiceTest {

    @Mock
    private TriageRuleRepository triageRuleRepository;

    @Mock
    private TriageRuleVersionRepository triageRuleVersionRepository;

    @InjectMocks
    private TriageRuleService triageRuleService;

    private TriageRule officialRule;
    private TriageRule draftRule;

    @BeforeEach
    void setUp() {
        officialRule = TriageRule.builder()
                .id(1L)
                .storeId(1L)
                .ruleCode("RULE_EYE")
                .name("双眼皮分诊")
                .version(1)
                .status(RuleStatus.OFFICIAL)
                .isOfficial(true)
                .consultationType(ConsultationType.SURGERY_CONSULTATION)
                .departmentType(DepartmentType.PLASTIC_SURGERY)
                .keywords(Arrays.asList("双眼皮", "眼综合"))
                .priority(100)
                .isEnabled(true)
                .publishedBy("运营")
                .publishedAt(LocalDateTime.now())
                .build();

        draftRule = TriageRule.builder()
                .id(2L)
                .storeId(1L)
                .ruleCode("RULE_EYE")
                .name("双眼皮分诊_v2")
                .version(2)
                .status(RuleStatus.DRAFT)
                .isOfficial(false)
                .consultationType(ConsultationType.SURGERY_CONSULTATION)
                .departmentType(DepartmentType.PLASTIC_SURGERY)
                .keywords(Arrays.asList("双眼皮", "眼综合", "眼部修复"))
                .priority(100)
                .minAge(18)
                .maxAge(50)
                .isEnabled(true)
                .parentRuleId(1L)
                .build();
    }

    @Test
    void testCreateNewVersion() {
        when(triageRuleRepository.findById(1L)).thenReturn(Optional.of(officialRule));
        when(triageRuleRepository.save(any(TriageRule.class))).thenReturn(draftRule);

        TriageRule newVersion = triageRuleService.createNewVersion(1L, "运营专员");

        assertNotNull(newVersion);
        assertEquals(2, newVersion.getVersion());
        assertEquals(RuleStatus.DRAFT, newVersion.getStatus());
        assertFalse(newVersion.getIsOfficial());
        assertEquals(1L, newVersion.getParentRuleId());
        verify(triageRuleRepository, times(1)).save(any(TriageRule.class));
        verify(triageRuleVersionRepository, times(1)).save(any(TriageRuleVersion.class));
    }

    @Test
    void testCreateNewVersion_RuleNotFound() {
        when(triageRuleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> triageRuleService.createNewVersion(999L, "运营专员"));
    }

    @Test
    void testPublishGrayRule() {
        RuleGrayRequest request = RuleGrayRequest.builder()
                .ruleId(2L)
                .graySources(Arrays.asList(AppointmentSource.MINI_PROGRAM))
                .grayProjects(Arrays.asList(ConsultationType.SURGERY_CONSULTATION))
                .grayPercentage(30)
                .operatorId(100L)
                .operatorName("运营主管")
                .build();

        when(triageRuleRepository.findById(2L)).thenReturn(Optional.of(draftRule));
        when(triageRuleRepository.save(any(TriageRule.class))).thenReturn(draftRule);

        TriageRule result = triageRuleService.publishGrayRule(request);

        assertNotNull(result);
        assertEquals(RuleStatus.GRAY, result.getStatus());
        assertEquals(30, result.getGrayPercentage());
        assertEquals("运营主管", result.getPublishedBy());
        assertNotNull(result.getPublishedAt());
        verify(triageRuleVersionRepository, times(1)).save(any(TriageRuleVersion.class));
    }

    @Test
    void testPublishGrayRule_InvalidStatus() {
        draftRule.setStatus(RuleStatus.OFFICIAL);
        RuleGrayRequest request = RuleGrayRequest.builder()
                .ruleId(2L)
                .graySources(Arrays.asList(AppointmentSource.MINI_PROGRAM))
                .grayPercentage(30)
                .build();

        when(triageRuleRepository.findById(2L)).thenReturn(Optional.of(draftRule));

        assertThrows(IllegalStateException.class, () -> triageRuleService.publishGrayRule(request));
    }

    @Test
    void testPublishOfficialRule() {
        draftRule.setStatus(RuleStatus.GRAY);
        draftRule.setGrayPercentage(30);

        RulePublishRequest request = RulePublishRequest.builder()
                .ruleId(2L)
                .operatorId(100L)
                .operatorName("运营总监")
                .build();

        when(triageRuleRepository.findById(2L)).thenReturn(Optional.of(draftRule));
        when(triageRuleRepository.findByStoreIdAndRuleCodeAndIsOfficialTrue(1L, "RULE_EYE"))
                .thenReturn(Optional.of(officialRule));
        when(triageRuleRepository.save(any(TriageRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TriageRule result = triageRuleService.publishOfficialRule(request);

        assertNotNull(result);
        assertEquals(RuleStatus.OFFICIAL, result.getStatus());
        assertTrue(result.getIsOfficial());
        assertEquals(0, result.getGrayPercentage());
        assertNull(result.getGraySources());
        assertNull(result.getGrayProjects());

        verify(triageRuleRepository, times(1)).save(officialRule);
        assertEquals(RuleStatus.ARCHIVED, officialRule.getStatus());
        assertFalse(officialRule.getIsOfficial());
        verify(triageRuleVersionRepository, times(1)).save(any(TriageRuleVersion.class));
    }

    @Test
    void testPublishOfficialRule_InvalidStatus() {
        RulePublishRequest request = RulePublishRequest.builder()
                .ruleId(2L)
                .build();

        when(triageRuleRepository.findById(2L)).thenReturn(Optional.of(draftRule));

        assertThrows(IllegalStateException.class, () -> triageRuleService.publishOfficialRule(request));
    }

    @Test
    void testRollbackRule() {
        TriageRuleVersion version1 = TriageRuleVersion.builder()
                .id(1L)
                .ruleId(1L)
                .ruleCode("RULE_EYE")
                .version(1)
                .name("双眼皮分诊_v1")
                .status(RuleStatus.OFFICIAL)
                .keywords(Arrays.asList("双眼皮"))
                .build();

        RuleRollbackRequest request = RuleRollbackRequest.builder()
                .ruleId(1L)
                .targetVersionId(1L)
                .operatorId(100L)
                .operatorName("技术总监")
                .build();

        when(triageRuleRepository.findById(1L)).thenReturn(Optional.of(officialRule));
        when(triageRuleVersionRepository.findById(1L)).thenReturn(Optional.of(version1));
        when(triageRuleRepository.save(any(TriageRule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(triageRuleVersionRepository.save(any(TriageRuleVersion.class))).thenReturn(version1);

        TriageRule result = triageRuleService.rollbackRule(request);

        assertNotNull(result);
        assertEquals(2, result.getVersion());
        assertEquals(RuleStatus.DRAFT, result.getStatus());
        assertFalse(result.getIsOfficial());
        verify(triageRuleRepository, times(1)).save(any(TriageRule.class));
    }

    @Test
    void testCheckGrayMatch_MatchSource() {
        draftRule.setStatus(RuleStatus.GRAY);
        draftRule.setGraySources(Arrays.asList(AppointmentSource.MINI_PROGRAM, AppointmentSource.OFFICIAL_WEBSITE));
        draftRule.setGrayPercentage(100);

        boolean result = triageRuleService.checkGrayMatch(draftRule, AppointmentSource.MINI_PROGRAM,
                ConsultationType.SURGERY_CONSULTATION, 12345L);

        assertTrue(result);
    }

    @Test
    void testCheckGrayMatch_NoMatchSource() {
        draftRule.setStatus(RuleStatus.GRAY);
        draftRule.setGraySources(Arrays.asList(AppointmentSource.MINI_PROGRAM));
        draftRule.setGrayPercentage(100);

        boolean result = triageRuleService.checkGrayMatch(draftRule, AppointmentSource.CRM,
                ConsultationType.SURGERY_CONSULTATION, 12345L);

        assertFalse(result);
    }

    @Test
    void testCheckGrayMatch_Percentage() {
        draftRule.setStatus(RuleStatus.GRAY);
        draftRule.setGraySources(null);
        draftRule.setGrayProjects(null);
        draftRule.setGrayPercentage(50);

        int matchCount = 0;
        for (long i = 0; i < 100; i++) {
            if (triageRuleService.checkGrayMatch(draftRule, AppointmentSource.MINI_PROGRAM,
                    ConsultationType.SURGERY_CONSULTATION, i)) {
                matchCount++;
            }
        }

        assertTrue(matchCount >= 30 && matchCount <= 70, "百分比灰度应在30%-70%之间，实际: " + matchCount + "%");
    }

    @Test
    void testGetRuleVersions() {
        List<TriageRuleVersion> versions = Arrays.asList(
                TriageRuleVersion.builder().version(2).build(),
                TriageRuleVersion.builder().version(1).build()
        );

        when(triageRuleVersionRepository.findByRuleIdOrderByVersionDesc(1L)).thenReturn(versions);

        List<TriageRuleVersion> result = triageRuleService.getRuleVersions(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(2, result.get(0).getVersion());
    }
}
