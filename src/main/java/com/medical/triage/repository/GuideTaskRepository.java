package com.medical.triage.repository;

import com.medical.triage.entity.GuideTask;
import com.medical.triage.enums.GuideTaskStatus;
import com.medical.triage.enums.GuideTaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GuideTaskRepository extends JpaRepository<GuideTask, Long> {

    List<GuideTask> findByAppointmentId(Long appointmentId);

    List<GuideTask> findByStoreIdAndStatus(Long storeId, GuideTaskStatus status);

    List<GuideTask> findByAssigneeId(Long assigneeId);

    List<GuideTask> findByAssigneeIdAndStatus(Long assigneeId, GuideTaskStatus status);

    List<GuideTask> findByStoreIdAndTaskTypeAndStatus(
            Long storeId, GuideTaskType taskType, GuideTaskStatus status);

    List<GuideTask> findByDepartmentIdAndStatus(Long departmentId, GuideTaskStatus status);

    List<GuideTask> findByStoreIdAndCreatedAtBetween(Long storeId, LocalDateTime startTime, LocalDateTime endTime);

    List<GuideTask> findByAssigneeIdAndCreatedAtBetween(Long assigneeId, LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT g FROM GuideTask g WHERE g.storeId = :storeId AND g.status = :status " +
           "AND g.dueTime < :now ORDER BY g.dueTime ASC")
    List<GuideTask> findOverdueTasks(
            @Param("storeId") Long storeId,
            @Param("status") GuideTaskStatus status,
            @Param("now") LocalDateTime now);

    @Query("SELECT g FROM GuideTask g WHERE g.assigneeId = :assigneeId AND g.status = :status " +
           "AND g.dueTime BETWEEN :now AND :endTime ORDER BY g.dueTime ASC")
    List<GuideTask> findUpcomingTasks(
            @Param("assigneeId") Long assigneeId,
            @Param("status") GuideTaskStatus status,
            @Param("now") LocalDateTime now,
            @Param("endTime") LocalDateTime endTime);

    long countByStoreIdAndStatus(Long storeId, GuideTaskStatus status);

    long countByAssigneeIdAndStatus(Long assigneeId, GuideTaskStatus status);

    long countByDepartmentIdAndStatus(Long departmentId, GuideTaskStatus status);

    boolean existsByAppointmentIdAndStatus(Long appointmentId, GuideTaskStatus status);
}
