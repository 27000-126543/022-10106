package com.medical.triage.service;

import com.medical.triage.dto.response.StatisticsResponse;
import com.medical.triage.entity.Appointment;
import com.medical.triage.entity.Store;
import com.medical.triage.enums.ConsultationType;
import com.medical.triage.enums.RiskLevel;
import com.medical.triage.repository.AppointmentRepository;
import com.medical.triage.repository.StoreRepository;
import com.medical.triage.repository.TriageResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
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
}
