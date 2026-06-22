package com.medical.triage.service;

import com.medical.triage.dto.response.ChannelStats;
import com.medical.triage.dto.response.DailyTrendItem;
import com.medical.triage.dto.response.ProjectStats;
import com.medical.triage.dto.response.RiskStats;
import com.medical.triage.dto.response.TrendStatisticsResponse;
import com.medical.triage.entity.Appointment;
import com.medical.triage.entity.Store;
import com.medical.triage.entity.TriageResult;
import com.medical.triage.enums.AppointmentSource;
import com.medical.triage.enums.ConsultationType;
import com.medical.triage.enums.RiskLevel;
import com.medical.triage.repository.AppointmentRepository;
import com.medical.triage.repository.StoreRepository;
import com.medical.triage.repository.TriageResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private TriageResultRepository triageResultRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private StatisticsService statisticsService;

    private Store testStore;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<Appointment> testAppointments;

    @BeforeEach
    void setUp() {
        testStore = Store.builder()
                .id(1L)
                .name("北京朝阳店")
                .status(1)
                .build();

        startDate = LocalDate.of(2026, 6, 1);
        endDate = LocalDate.of(2026, 6, 7);

        testAppointments = Arrays.asList(
                createAppointment(1L, ConsultationType.SURGERY_CONSULTATION, AppointmentSource.OFFICIAL_WEBSITE, startDate.atTime(10, 0)),
                createAppointment(2L, ConsultationType.SURGERY_CONSULTATION, AppointmentSource.MINI_PROGRAM, startDate.atTime(14, 0)),
                createAppointment(3L, ConsultationType.INJECTION_CONSULTATION, AppointmentSource.MINI_PROGRAM, startDate.plusDays(1).atTime(11, 0)),
                createAppointment(4L, ConsultationType.SKIN_CARE_CONSULTATION, AppointmentSource.CRM, startDate.plusDays(2).atTime(9, 0)),
                createAppointment(5L, ConsultationType.SURGERY_CONSULTATION, AppointmentSource.STORE_FRONT, startDate.plusDays(3).atTime(15, 0)),
                createAppointment(6L, ConsultationType.NON_SURGERY_CONSULTATION, AppointmentSource.OFFICIAL_WEBSITE, startDate.plusDays(4).atTime(10, 0)),
                createAppointment(7L, ConsultationType.INJECTION_CONSULTATION, AppointmentSource.MINI_PROGRAM, startDate.plusDays(5).atTime(16, 0))
        );
    }

    private Appointment createAppointment(Long id, ConsultationType type, AppointmentSource source, LocalDateTime time) {
        return Appointment.builder()
                .id(id)
                .storeId(1L)
                .customerId(id)
                .consultationType(type)
                .source(source)
                .appointmentTime(time)
                .build();
    }

    @Test
    void testGetTrendStatistics_DailyTrends() {
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        when(appointmentRepository.findByStoreIdAndAppointmentTimeBetween(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(testAppointments);
        when(triageResultRepository.findByStoreIdAndCreatedAtBetween(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        TrendStatisticsResponse result = statisticsService.getTrendStatistics(1L, startDate, endDate);

        assertNotNull(result);
        assertNotNull(result.getDailyTrends());
        assertTrue(result.getDailyTrends().size() >= 7);

        DailyTrendItem day1 = result.getDailyTrends().stream()
                .filter(d -> d.getDate().equals(startDate))
                .findFirst()
                .orElse(null);

        assertNotNull(day1);
        assertEquals(2, day1.getTotalCount());
        assertEquals(2, day1.getSurgeryCount());
    }

    @Test
    void testGetTrendStatistics_ChannelStats() {
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        when(appointmentRepository.findByStoreIdAndAppointmentTimeBetween(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(testAppointments);
        when(triageResultRepository.findByStoreIdAndCreatedAtBetween(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        TrendStatisticsResponse result = statisticsService.getTrendStatistics(1L, startDate, endDate);

        assertNotNull(result.getChannelStats());
        assertTrue(result.getChannelStats().size() > 0);

        ChannelStats miniProgram = result.getChannelStats().stream()
                .filter(c -> c.getSource() == AppointmentSource.MINI_PROGRAM)
                .findFirst()
                .orElse(null);

        assertNotNull(miniProgram);
        assertEquals(3, miniProgram.getCount());
        assertEquals(42.9, miniProgram.getPercentage(), 0.1);
    }

    @Test
    void testGetTrendStatistics_ProjectStats() {
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        when(appointmentRepository.findByStoreIdAndAppointmentTimeBetween(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(testAppointments);
        when(triageResultRepository.findByStoreIdAndCreatedAtBetween(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        TrendStatisticsResponse result = statisticsService.getTrendStatistics(1L, startDate, endDate);

        assertNotNull(result.getProjectStats());

        ProjectStats surgery = result.getProjectStats().stream()
                .filter(p -> p.getConsultationType() == ConsultationType.SURGERY_CONSULTATION)
                .findFirst()
                .orElse(null);

        assertNotNull(surgery);
        assertEquals(3, surgery.getCount());
        assertEquals(42.9, surgery.getPercentage(), 0.1);
    }

    @Test
    void testGetTrendStatistics_RiskStats() {
        List<TriageResult> triageResults = Arrays.asList(
                TriageResult.builder().id(1L).appointmentId(1L).riskLevel(RiskLevel.LOW).build(),
                TriageResult.builder().id(2L).appointmentId(2L).riskLevel(RiskLevel.MEDIUM).build(),
                TriageResult.builder().id(3L).appointmentId(3L).riskLevel(RiskLevel.HIGH).build(),
                TriageResult.builder().id(4L).appointmentId(5L).riskLevel(RiskLevel.EXTREME).build()
        );

        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        when(appointmentRepository.findByStoreIdAndAppointmentTimeBetween(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(testAppointments);
        when(triageResultRepository.findByStoreIdAndCreatedAtBetween(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(triageResults);

        TrendStatisticsResponse result = statisticsService.getTrendStatistics(1L, startDate, endDate);

        assertNotNull(result.getRiskStats());

        RiskStats highRisk = result.getRiskStats().stream()
                .filter(r -> r.getRiskLevel() == RiskLevel.HIGH)
                .findFirst()
                .orElse(null);

        RiskStats extremeRisk = result.getRiskStats().stream()
                .filter(r -> r.getRiskLevel() == RiskLevel.EXTREME)
                .findFirst()
                .orElse(null);

        assertNotNull(highRisk);
        assertEquals(1, highRisk.getCount());
        assertEquals(25.0, highRisk.getPercentage(), 0.1);

        assertNotNull(extremeRisk);
        assertEquals(1, extremeRisk.getCount());
        assertEquals(1, extremeRisk.getDoctorAssessmentCount());
    }

    @Test
    void testGetTrendStatistics_DateRange() {
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        when(appointmentRepository.findByStoreIdAndAppointmentTimeBetween(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(testAppointments);
        when(triageResultRepository.findByStoreIdAndCreatedAtBetween(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        TrendStatisticsResponse result = statisticsService.getTrendStatistics(1L, startDate, endDate);

        assertNotNull(result.getDateRange());
        assertTrue(result.getDateRange().contains(startDate.toString()));
        assertTrue(result.getDateRange().contains(endDate.toString()));
    }

    @Test
    void testGetTrendStatistics_StoreNotFound() {
        when(storeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                statisticsService.getTrendStatistics(999L, startDate, endDate));
    }

    @Test
    void testGetTrendStatistics_EmptyData() {
        when(storeRepository.findById(1L)).thenReturn(Optional.of(testStore));
        when(appointmentRepository.findByStoreIdAndAppointmentTimeBetween(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(triageResultRepository.findByStoreIdAndCreatedAtBetween(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        TrendStatisticsResponse result = statisticsService.getTrendStatistics(1L, startDate, endDate);

        assertNotNull(result);
        assertNotNull(result.getDailyTrends());
        assertTrue(result.getDailyTrends().size() >= 7);

        for (DailyTrendItem item : result.getDailyTrends()) {
            assertEquals(0, item.getTotalCount());
        }

        assertTrue(result.getChannelStats().isEmpty());
        assertTrue(result.getProjectStats().isEmpty());
        assertTrue(result.getRiskStats().isEmpty());
    }
}
