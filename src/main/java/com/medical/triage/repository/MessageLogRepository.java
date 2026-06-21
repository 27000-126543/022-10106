package com.medical.triage.repository;

import com.medical.triage.entity.MessageLog;
import com.medical.triage.enums.MessageChannel;
import com.medical.triage.enums.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageLogRepository extends JpaRepository<MessageLog, Long> {

    List<MessageLog> findByAppointmentId(Long appointmentId);

    List<MessageLog> findByCustomerId(Long customerId);

    List<MessageLog> findByMessageType(MessageType messageType);

    List<MessageLog> findByChannel(MessageChannel channel);

    List<MessageLog> findByIsSuccess(Boolean isSuccess);

    List<MessageLog> findByAppointmentIdAndMessageType(Long appointmentId, MessageType messageType);

    List<MessageLog> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    List<MessageLog> findByTargetPhone(String targetPhone);

    List<MessageLog> findByMessageTypeAndIsSuccessAndCreatedAtBetween(
            MessageType messageType, Boolean isSuccess, LocalDateTime startTime, LocalDateTime endTime);

    long countByMessageTypeAndIsSuccess(MessageType messageType, Boolean isSuccess);

    long countByChannelAndCreatedAtBetween(MessageChannel channel, LocalDateTime startTime, LocalDateTime endTime);

    void deleteByAppointmentId(Long appointmentId);

    void deleteByCreatedAtBefore(LocalDateTime cutoffTime);
}
