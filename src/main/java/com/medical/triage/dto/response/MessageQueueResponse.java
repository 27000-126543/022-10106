package com.medical.triage.dto.response;

import com.medical.triage.enums.MessageChannel;
import com.medical.triage.enums.MessageQueueStatus;
import com.medical.triage.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageQueueResponse {

    private Long id;

    private Long messageLogId;

    private Long appointmentId;

    private Long customerId;

    private MessageType messageType;

    private MessageChannel channel;

    private String content;

    private String targetPhone;

    private String targetOpenid;

    private MessageQueueStatus status;

    private Integer retryCount;

    private Integer maxRetryCount;

    private LocalDateTime nextRetryTime;

    private LocalDateTime lastRetryTime;

    private String lastFailReason;

    private Integer priority;

    private LocalDateTime scheduledTime;

    private LocalDateTime sentTime;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
