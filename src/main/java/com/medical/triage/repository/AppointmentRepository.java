package com.medical.triage.repository;

import com.medical.triage.entity.Appointment;
import com.medical.triage.enums.AppointmentSource;
import com.medical.triage.enums.AppointmentStatus;
import com.medical.triage.enums.ConsultationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByStoreIdAndAppointmentTimeBetween(Long storeId, LocalDateTime startTime, LocalDateTime endTime);

    List<Appointment> findByCustomerId(Long customerId);

    List<Appointment> findByStatus(AppointmentStatus status);

    List<Appointment> findByStoreIdAndStatus(Long storeId, AppointmentStatus status);

    List<Appointment> findByStoreIdAndConsultationType(Long storeId, ConsultationType consultationType);

    List<Appointment> findBySource(AppointmentSource source);

    List<Appointment> findByCustomerIdAndStatus(Long customerId, AppointmentStatus status);

    @Query("SELECT a FROM Appointment a WHERE a.storeId = :storeId AND a.appointmentTime >= :startTime AND a.appointmentTime < :endTime AND a.status = :status")
    List<Appointment> findByStoreIdAndDateRangeAndStatus(
            @Param("storeId") Long storeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("status") AppointmentStatus status);

    long countByStoreIdAndStatus(Long storeId, AppointmentStatus status);

    long countByStoreIdAndConsultationTypeAndAppointmentTimeBetween(
            Long storeId, ConsultationType consultationType, LocalDateTime startTime, LocalDateTime endTime);

    boolean existsByCustomerIdAndAppointmentTimeBetween(Long customerId, LocalDateTime startTime, LocalDateTime endTime);
}
