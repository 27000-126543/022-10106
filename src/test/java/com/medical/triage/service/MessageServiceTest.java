package com.medical.triage.service;

import com.medical.triage.dto.request.MessageResendRequest;
import com.medical.triage.entity.Appointment;
import com.medical.triage.entity.Customer;
import com.medical.triage.entity.MessageLog;
import com.medical.triage.entity.MessageQueue;
import com.medical.triage.enums.AppointmentStatus;
import com.medical.triage.enums.MessageChannel;
import com.medical.triage.enums.MessageQueueStatus;
import com.medical.triage.enums.MessageType;
import com.medical.triage.repository.AppointmentRepository;
import com.medical.triage.repository.CustomerRepository;
import com.medical.triage.repository.MessageLogRepository;
import com.medical.triage.repository.MessageQueueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageQueueRepository messageQueueRepository;

    @Mock
    private MessageLogRepository messageLogRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private MessageService messageService;

    private MessageQueue pendingQueue;
    private MessageQueue failedQueue;
    private Appointment testAppointment;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = Customer.builder()
                .id(1L)
                .name("张三")
                .phone("13800138000")
                .build();

        testAppointment = Appointment.builder()
                .id(1L)
                .customerId(1L)
                .storeId(1L)
                .status(AppointmentStatus.PENDING)
                .build();

        pendingQueue = MessageQueue.builder()
                .id(1L)
                .messageLogId(1L)
                .appointmentId(1L)
                .customerId(1L)
                .messageType(MessageType.WAIT_REMINDER)
                .channel(MessageChannel.SMS)
                .content("【XX医美】您的预约即将开始，请提前15分钟到店")
                .targetPhone("13800138000")
                .status(MessageQueueStatus.PENDING)
                .retryCount(0)
                .maxRetryCount(3)
                .priority(1)
                .scheduledTime(LocalDateTime.now())
                .build();

        failedQueue = MessageQueue.builder()
                .id(2L)
                .messageLogId(2L)
                .appointmentId(1L)
                .customerId(1L)
                .messageType(MessageType.WAIT_REMINDER)
                .channel(MessageChannel.MINI_PROGRAM)
                .content("【XX医美】您的预约即将开始")
                .targetOpenid("openid_123")
                .status(MessageQueueStatus.FAILED)
                .retryCount(3)
                .maxRetryCount(3)
                .lastFailReason("小程序接口调用超时")
                .lastRetryTime(LocalDateTime.now().minusMinutes(5))
                .priority(1)
                .build();
    }

    @Test
    void testCreateMessageQueue() {
        when(messageQueueRepository.save(any(MessageQueue.class))).thenReturn(pendingQueue);
        when(messageLogRepository.save(any(MessageLog.class))).thenAnswer(invocation -> {
            MessageLog log = invocation.getArgument(0);
            log.setId(1L);
            return log;
        });
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(testAppointment));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        MessageQueue result = messageService.createMessageQueue(
                1L, MessageType.WAIT_REMINDER, MessageChannel.SMS,
                "测试内容", "13800138000", null, 1);

        assertNotNull(result);
        assertEquals(MessageQueueStatus.PENDING, result.getStatus());
        assertEquals(0, result.getRetryCount());
        assertEquals(3, result.getMaxRetryCount());
        verify(messageLogRepository, times(1)).save(any(MessageLog.class));
    }

    @Test
    void testResendMessage_Success() {
        when(messageQueueRepository.findById(2L)).thenReturn(Optional.of(failedQueue));
        when(messageQueueRepository.save(any(MessageQueue.class))).thenReturn(failedQueue);
        when(messageLogRepository.save(any(MessageLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(testAppointment));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        boolean result = messageService.resendMessage(2L, 100L, "管理员");

        assertTrue(result);
        assertEquals(MessageQueueStatus.PENDING, failedQueue.getStatus());
        assertEquals(0, failedQueue.getRetryCount());
        assertNotNull(failedQueue.getNextRetryTime());
        verify(messageQueueRepository, times(1)).save(failedQueue);
    }

    @Test
    void testResendMessage_QueueNotFound() {
        when(messageQueueRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> messageService.resendMessage(999L, 100L, "管理员"));
    }

    @Test
    void testResendMessages_Batch() {
        List<Long> ids = Arrays.asList(1L, 2L);
        MessageResendRequest request = MessageResendRequest.builder()
                .messageQueueIds(ids)
                .operatorId(100L)
                .operatorName("管理员")
                .build();

        when(messageQueueRepository.findById(1L)).thenReturn(Optional.of(pendingQueue));
        when(messageQueueRepository.findById(2L)).thenReturn(Optional.of(failedQueue));
        when(messageQueueRepository.save(any(MessageQueue.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageLogRepository.save(any(MessageLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(testAppointment));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        int successCount = messageService.resendMessages(request);

        assertEquals(2, successCount);
        verify(messageQueueRepository, times(2)).save(any(MessageQueue.class));
    }

    @Test
    void testGetQueueList() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<MessageQueue> page = new PageImpl<>(Arrays.asList(pendingQueue, failedQueue), pageable, 2);

        when(messageQueueRepository.findByStoreIdAndStatus(1L, MessageQueueStatus.FAILED, pageable))
                .thenReturn(page);

        Page<MessageQueue> result = messageService.getQueueList(1L, MessageQueueStatus.FAILED, pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(MessageQueueStatus.FAILED, result.getContent().get(1).getStatus());
    }

    @Test
    void testGetQueueList_AllStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<MessageQueue> page = new PageImpl<>(Arrays.asList(pendingQueue, failedQueue), pageable, 2);

        when(messageQueueRepository.findByStoreId(1L, pageable)).thenReturn(page);

        Page<MessageQueue> result = messageService.getQueueList(1L, null, pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(messageQueueRepository, times(1)).findByStoreId(1L, pageable);
    }

    @Test
    void testGetMessageQueueByAppointmentId() {
        when(messageQueueRepository.findByAppointmentId(1L))
                .thenReturn(Arrays.asList(pendingQueue, failedQueue));

        List<MessageQueue> result = messageService.getMessageQueueByAppointmentId(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testRetryFailedMessages_ScheduledTask() {
        MessageQueue retryQueue = MessageQueue.builder()
                .id(3L)
                .status(MessageQueueStatus.FAILED)
                .retryCount(1)
                .maxRetryCount(3)
                .nextRetryTime(LocalDateTime.now().minusMinutes(1))
                .appointmentId(1L)
                .build();

        when(messageQueueRepository.findByStatusAndNextRetryTimeBefore(
                eq(MessageQueueStatus.FAILED), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(retryQueue));
        when(messageQueueRepository.save(any(MessageQueue.class))).thenReturn(retryQueue);
        when(messageLogRepository.save(any(MessageLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(testAppointment));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        messageService.retryFailedMessages();

        verify(messageQueueRepository, times(1)).updateStatusAndIncrementRetry(
                eq(3L), eq(MessageQueueStatus.RETRYING), any(LocalDateTime.class));
    }

    @Test
    void testRetryFailedMessages_MaxRetryReached() {
        MessageQueue maxRetryQueue = MessageQueue.builder()
                .id(4L)
                .status(MessageQueueStatus.FAILED)
                .retryCount(3)
                .maxRetryCount(3)
                .nextRetryTime(LocalDateTime.now().minusMinutes(1))
                .build();

        when(messageQueueRepository.findByStatusAndNextRetryTimeBefore(
                eq(MessageQueueStatus.FAILED), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(maxRetryQueue));

        messageService.retryFailedMessages();

        verify(messageQueueRepository, never()).updateStatusAndIncrementRetry(any(), any(), any());
    }
}
