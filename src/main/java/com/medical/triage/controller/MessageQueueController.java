package com.medical.triage.controller;

import com.medical.triage.dto.request.MessageResendRequest;
import com.medical.triage.dto.response.ApiResponse;
import com.medical.triage.dto.response.MessageQueueResponse;
import com.medical.triage.entity.MessageQueue;
import com.medical.triage.enums.MessageQueueStatus;
import com.medical.triage.service.MessageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/message-queue")
@RequiredArgsConstructor
public class MessageQueueController {

    private final MessageService messageService;

    @GetMapping
    public ApiResponse<Page<MessageQueueResponse>> getMessageQueue(
            @RequestParam @NotNull(message = "门店ID不能为空") Long storeId,
            @RequestParam(required = false) MessageQueueStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<MessageQueue> page = messageService.getQueueList(storeId, status, pageable);
        Page<MessageQueueResponse> responsePage = page.map(this::buildResponse);
        return ApiResponse.success(responsePage);
    }

    @PostMapping("/{id}/resend")
    public ApiResponse<Void> resendMessage(
            @PathVariable @NotNull(message = "消息队列ID不能为空") Long id) {
        messageService.resendMessage(id);
        return ApiResponse.success("消息重发成功", null);
    }

    @PostMapping("/batch-resend")
    public ApiResponse<Void> batchResendMessages(
            @Valid @RequestBody MessageResendRequest request) {
        messageService.resendMessages(request.getMessageQueueIds());
        return ApiResponse.success("批量重发消息成功", null);
    }

    @GetMapping("/appointment/{appointmentId}")
    public ApiResponse<List<MessageQueueResponse>> getMessageQueueByAppointment(
            @PathVariable @NotNull(message = "预约ID不能为空") Long appointmentId) {
        List<MessageQueue> queueList = messageService.getMessageQueueByAppointmentId(appointmentId);
        List<MessageQueueResponse> responseList = queueList.stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(responseList);
    }

    private MessageQueueResponse buildResponse(MessageQueue queue) {
        return MessageQueueResponse.builder()
                .id(queue.getId())
                .messageLogId(queue.getMessageLogId())
                .appointmentId(queue.getAppointmentId())
                .customerId(queue.getCustomerId())
                .messageType(queue.getMessageType())
                .channel(queue.getChannel())
                .content(queue.getContent())
                .targetPhone(queue.getTargetPhone())
                .targetOpenid(queue.getTargetOpenid())
                .status(queue.getStatus())
                .retryCount(queue.getRetryCount())
                .maxRetryCount(queue.getMaxRetryCount())
                .nextRetryTime(queue.getNextRetryTime())
                .lastRetryTime(queue.getLastRetryTime())
                .lastFailReason(queue.getLastFailReason())
                .priority(queue.getPriority())
                .scheduledTime(queue.getScheduledTime())
                .sentTime(queue.getSentTime())
                .createdBy(queue.getCreatedBy())
                .createdAt(queue.getCreatedAt())
                .updatedAt(queue.getUpdatedAt())
                .build();
    }
}
