package com.medical.triage.repository;

import com.medical.triage.entity.MessageQueue;
import com.medical.triage.enums.MessageQueueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageQueueRepository extends JpaRepository<MessageQueue, Long> {

    List<MessageQueue> findByStatusIn(List<MessageQueueStatus> statuses);

    List<MessageQueue> findByStatusAndNextRetryTimeBefore(MessageQueueStatus status, LocalDateTime time);

    Page<MessageQueue> findByStoreIdAndStatus(Long storeId, MessageQueueStatus status, Pageable pageable);

    Page<MessageQueue> findByStoreId(Long storeId, Pageable pageable);

    List<MessageQueue> findByAppointmentId(Long appointmentId);

    @Modifying
    @Query("UPDATE MessageQueue m SET m.status = :newStatus, m.retryCount = m.retryCount + 1, m.lastRetryTime = :now WHERE m.id = :id")
    int updateStatusAndIncrementRetry(@Param("id") Long id, @Param("newStatus") MessageQueueStatus newStatus, @Param("now") LocalDateTime now);
}
