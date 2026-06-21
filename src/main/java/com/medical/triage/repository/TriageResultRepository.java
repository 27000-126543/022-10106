package com.medical.triage.repository;

import com.medical.triage.entity.TriageResult;
import com.medical.triage.enums.ConsultationType;
import com.medical.triage.enums.RiskLevel;
import com.medical.triage.enums.TriageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TriageResultRepository extends JpaRepository<TriageResult, Long> {

    Optional<TriageResult> findByAppointmentId(Long appointmentId);

    List<TriageResult> findByStoreIdAndCreatedAtBetween(Long storeId, LocalDateTime startTime, LocalDateTime endTime);

    List<TriageResult> findByStoreIdAndRiskLevel(Long storeId, RiskLevel riskLevel);

    List<TriageResult> findByStoreIdAndStatus(Long storeId, TriageStatus status);

    List<TriageResult> findByDepartmentIdAndStatus(Long departmentId, TriageStatus status);

    List<TriageResult> findByStoreIdAndConsultationTypeAndCreatedAtBetween(
            Long storeId, ConsultationType consultationType, LocalDateTime startTime, LocalDateTime endTime);

    List<TriageResult> findByCustomerId(Long customerId);

    List<TriageResult> findByStoreIdAndIsReassignedTrue(Long storeId);

    @Query("SELECT COUNT(t) FROM TriageResult t WHERE t.storeId = :storeId " +
           "AND t.riskLevel = :riskLevel AND t.createdAt BETWEEN :startTime AND :endTime")
    long countByStoreIdAndRiskLevelAndDateRange(
            @Param("storeId") Long storeId,
            @Param("riskLevel") RiskLevel riskLevel,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(t) FROM TriageResult t WHERE t.storeId = :storeId " +
           "AND t.consultationType = :consultationType AND t.createdAt BETWEEN :startTime AND :endTime")
    long countByStoreIdAndConsultationTypeAndDateRange(
            @Param("storeId") Long storeId,
            @Param("consultationType") ConsultationType consultationType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    long countByStoreIdAndStatus(Long storeId, TriageStatus status);

    boolean existsByAppointmentId(Long appointmentId);
}
