package com.medical.triage.entity;

import com.medical.triage.enums.MessageChannel;
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
@Table(name = "message_log")
public class MessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    private LocalDateTime sentTime;

    private Boolean isSuccess;

    @Column(columnDefinition = "TEXT")
    private String failReason;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
