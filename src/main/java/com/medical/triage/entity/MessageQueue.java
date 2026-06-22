package com.medical.triage.entity;

import com.medical.triage.enums.MessageChannel;
import com.medical.triage.enums.MessageQueueStatus;
import com.medical.triage.enums.MessageType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "message_queue")
public class MessageQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long messageLogId;

    private Long appointmentId;

    private Long customerId;

    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    @Enumerated(EnumType.STRING)
    private MessageChannel channel;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String targetPhone;

    private String targetOpenid;

    @Enumerated(EnumType.STRING)
    private MessageQueueStatus status;

    private Integer retryCount;

    private Integer maxRetryCount;

    private LocalDateTime nextRetryTime;

    private LocalDateTime lastRetryTime;

    @Column(columnDefinition = "TEXT")
    private String lastFailReason;

    private Integer priority;

    private LocalDateTime scheduledTime;

    private LocalDateTime sentTime;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (retryCount == null) {
            retryCount = 0;
        }
        if (maxRetryCount == null) {
            maxRetryCount = 3;
        }
        if (priority == null) {
            priority = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
