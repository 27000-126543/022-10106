package com.medical.triage.service;

import com.medical.triage.dto.response.ChannelStats;
import com.medical.triage.dto.response.DailyTrendItem;
import com.medical.triage.dto.response.ProjectStats;
import com.medical.triage.dto.response.RiskStats;
import com.medical.triage.dto.response.StatisticsResponse;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final AppointmentRepository appointmentRepository;
    private final TriageResultRepository triageResultRepository;
    private final StoreRepository storeRepository;

    @Transactional(readOnly = true)
    public StatisticsResponse getStoreStatistics(Long storeId, LocalDate startDate, LocalDate endDate) {
        log.info("获取门店统计, storeId: {}, dateRange: {} ~ {}", storeId, startDate, endDate);

        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("门店不存在: " + storeId));

        List<Appointment> appointments = appointmentRepository.findByStoreIdAndAppointmentTimeBetween(
                storeId, startTime, endTime);
        long totalCount = appointments.size();

        long surgeryCount = appointmentRepository.countByStoreIdAndConsultationTypeAndAppointmentTimeBetween(
                storeId, ConsultationType.SURGERY_CONSULTATION, startTime, endTime);

        long nonSurgeryCount = appointmentRepository.countByStoreIdAndConsultationTypeAndAppointmentTimeBetween(
                storeId, ConsultationType.NON_SURGERY_CONSULTATION, startTime, endTime);

        long injectionCount = appointmentRepository.countByStoreIdAndConsultationTypeAndAppointmentTimeBetween(
                storeId, ConsultationType.INJECTION_CONSULTATION, startTime, endTime);

        long skinCareCount = appointmentRepository.countByStoreIdAndConsultationTypeAndAppointmentTimeBetween(
                storeId, ConsultationType.SKIN_CARE_CONSULTATION, startTime, endTime);

        long highRiskCount = triageResultRepository.countByStoreIdAndRiskLevelAndDateRange(
                storeId, RiskLevel.HIGH, startTime, endTime);

        highRiskCount += triageResultRepository.countByStoreIdAndRiskLevelAndDateRange(
                storeId, RiskLevel.EXTREME, startTime, endTime);

        long doctorAssessmentCount = appointmentRepository.countByStoreIdAndConsultationTypeAndAppointmentTimeBetween(
                storeId, ConsultationType.COMPREHENSIVE, startTime, endTime);

        String dateRange = startDate + " ~ " + endDate;

        return StatisticsResponse.builder()
                .storeId(storeId)
                .storeName(store.getName())
                .totalCount(totalCount)
                .surgeryCount(surgeryCount)
                .nonSurgeryCount(nonSurgeryCount)
                .injectionCount(injectionCount)
                .skinCareCount(skinCareCount)
                .highRiskCount(highRiskCount)
                .doctorAssessmentCount(doctorAssessmentCount)
                .dateRange(dateRange)
                .build();
    }

    @Transactional(readOnly = true)
    public List<StatisticsResponse> getAllStoresStatistics(LocalDate startDate, LocalDate endDate) {
        log.info("获取所有门店统计, dateRange: {} ~ {}", startDate, endDate);

        List<Store> stores = storeRepository.findByStatus(1);
        if (stores.isEmpty()) {
            log.warn("没有找到活跃门店");
            return new ArrayList<>();
        }

        return stores.stream()
                .map(store -> getStoreStatistics(store.getId(), startDate, endDate))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TrendStatisticsResponse getTrendStatistics(Long storeId, LocalDate startDate, LocalDate endDate) {
        log.info("获取趋势统计, storeId: {}, dateRange: {} ~ {}", storeId, startDate, endDate);

        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("门店不存在: " + storeId));

        List<Appointment> appointments = appointmentRepository.findByStoreIdAndAppointmentTimeBetween(
                storeId, startTime, endTime);

        List<TriageResult> triageResults = triageResultRepository.findByStoreIdAndCreatedAtBetween(
                storeId, startTime, endTime);

        String dateRange = startDate + " ~ " + endDate;

        return TrendStatisticsResponse.builder()
                .dateRange(dateRange)
                .dailyTrends(buildDailyTrends(appointments, triageResults, startDate, endDate))
                .channelStats(buildChannelStats(appointments))
                .projectStats(buildProjectStats(appointments))
                .riskStats(buildRiskStats(triageResults, appointments))
                .build();
    }

    private List<DailyTrendItem> buildDailyTrends(List<Appointment> appointments, List<TriageResult> triageResults,
                                                   LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, List<Appointment>> appointmentsByDate = appointments.stream()
                .collect(Collectors.groupingBy(a -> a.getAppointmentTime().toLocalDate()));

        Map<LocalDate, List<TriageResult>> triageByDate = triageResults.stream()
                .collect(Collectors.groupingBy(t -> t.getCreatedAt().toLocalDate()));

        List<DailyTrendItem> dailyTrends = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            List<Appointment> dayAppointments = appointmentsByDate.getOrDefault(currentDate, new ArrayList<>());
            List<TriageResult> dayTriage = triageByDate.getOrDefault(currentDate, new ArrayList<>());

            int totalCount = dayAppointments.size();
            int surgeryCount = (int) dayAppointments.stream()
                    .filter(a -> a.getConsultationType() == ConsultationType.SURGERY_CONSULTATION)
                    .count();
            int highRiskCount = (int) dayTriage.stream()
                    .filter(t -> t.getRiskLevel() == RiskLevel.HIGH || t.getRiskLevel() == RiskLevel.EXTREME)
                    .count();
            int doctorAssessmentCount = (int) dayAppointments.stream()
                    .filter(a -> a.getConsultationType() == ConsultationType.COMPREHENSIVE)
                    .count();

            dailyTrends.add(DailyTrendItem.builder()
                    .date(currentDate)
                    .totalCount(totalCount)
                    .surgeryCount(surgeryCount)
                    .highRiskCount(highRiskCount)
                    .doctorAssessmentCount(doctorAssessmentCount)
                    .build());

            currentDate = currentDate.plusDays(1);
        }

        return dailyTrends;
    }

    private List<ChannelStats> buildChannelStats(List<Appointment> appointments) {
        Map<AppointmentSource, Long> channelCounts = appointments.stream()
                .collect(Collectors.groupingBy(
                        Appointment::getSource,
                        () -> new EnumMap<>(AppointmentSource.class),
                        Collectors.counting()));

        int total = appointments.size();

        return channelCounts.entrySet().stream()
                .map(entry -> {
                    BigDecimal percentage = total > 0
                            ? BigDecimal.valueOf(entry.getValue())
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    return ChannelStats.builder()
                            .source(entry.getKey())
                            .count(entry.getValue().intValue())
                            .percentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<ProjectStats> buildProjectStats(List<Appointment> appointments) {
        Map<ConsultationType, Long> projectCounts = appointments.stream()
                .collect(Collectors.groupingBy(
                        Appointment::getConsultationType,
                        () -> new EnumMap<>(ConsultationType.class),
                        Collectors.counting()));

        int total = appointments.size();

        return projectCounts.entrySet().stream()
                .map(entry -> {
                    BigDecimal percentage = total > 0
                            ? BigDecimal.valueOf(entry.getValue())
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    return ProjectStats.builder()
                            .consultationType(entry.getKey())
                            .count(entry.getValue().intValue())
                            .percentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<RiskStats> buildRiskStats(List<TriageResult> triageResults, List<Appointment> appointments) {
        Map<RiskLevel, Long> riskCounts = triageResults.stream()
                .collect(Collectors.groupingBy(
                        TriageResult::getRiskLevel,
                        () -> new EnumMap<>(RiskLevel.class),
                        Collectors.counting()));

        Map<Long, ConsultationType> appointmentTypeMap = appointments.stream()
                .collect(Collectors.toMap(Appointment::getId, Appointment::getConsultationType));

        int total = triageResults.size();

        return riskCounts.entrySet().stream()
                .map(entry -> {
                    BigDecimal percentage = total > 0
                            ? BigDecimal.valueOf(entry.getValue())
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    int doctorAssessmentCount = (int) triageResults.stream()
                            .filter(t -> t.getRiskLevel() == entry.getKey())
                            .filter(t -> {
                                ConsultationType type = appointmentTypeMap.get(t.getAppointmentId());
                                return type == ConsultationType.COMPREHENSIVE;
                            })
                            .count();

                    return RiskStats.builder()
                            .riskLevel(entry.getKey())
                            .count(entry.getValue().intValue())
                            .percentage(percentage)
                            .doctorAssessmentCount(doctorAssessmentCount)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
