package com.medical.triage.service;

import com.medical.triage.config.TriageConfig;
import com.medical.triage.entity.Appointment;
import com.medical.triage.entity.ConsultationResult;
import com.medical.triage.entity.Customer;
import com.medical.triage.entity.MessageLog;
import com.medical.triage.entity.MessageQueue;
import com.medical.triage.enums.MessageChannel;
import com.medical.triage.enums.MessageQueueStatus;
import com.medical.triage.enums.MessageType;
import com.medical.triage.repository.AppointmentRepository;
import com.medical.triage.repository.CustomerRepository;
import com.medical.triage.repository.MessageLogRepository;
import com.medical.triage.repository.MessageQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final MessageLogRepository messageLogRepository;
    private final MessageQueueRepository messageQueueRepository;
    private final TriageConfig triageConfig;

    @Transactional
    public MessageQueue createMessageQueue(Long appointmentId, Long customerId, String targetPhone,
                                           String targetOpenid, MessageType messageType,
                                           MessageChannel channel, String content,
                                           Integer priority, LocalDateTime scheduledTime) {
        log.info("创建消息队列记录, appointmentId: {}, messageType: {}, channel: {}",
                appointmentId, messageType, channel);

        MessageLog messageLog = MessageLog.builder()
                .appointmentId(appointmentId)
                .customerId(customerId)
                .messageType(messageType)
                .channel(channel)
                .sendStatus(MessageQueueStatus.PENDING)
                .content(content)
                .targetPhone(targetPhone)
                .targetOpenid(targetOpenid)
                .retryCount(0)
                .maxRetryCount(3)
                .build();
        MessageLog savedLog = messageLogRepository.save(messageLog);

        MessageQueue messageQueue = MessageQueue.builder()
                .messageLogId(savedLog.getId())
                .appointmentId(appointmentId)
                .customerId(customerId)
                .messageType(messageType)
                .channel(channel)
                .content(content)
                .targetPhone(targetPhone)
                .targetOpenid(targetOpenid)
                .status(MessageQueueStatus.PENDING)
                .retryCount(0)
                .maxRetryCount(3)
                .priority(priority != null ? priority : 0)
                .scheduledTime(scheduledTime)
                .build();

        MessageQueue savedQueue = messageQueueRepository.save(messageQueue);

        savedLog.setQueueId(savedQueue.getId());
        messageLogRepository.save(savedLog);

        log.info("消息队列记录创建成功, queueId: {}, logId: {}", savedQueue.getId(), savedLog.getId());
        return savedQueue;
    }

    @Async("triageTaskExecutor")
    @Transactional
    public void sendWaitReminder(Long appointmentId) {
        log.info("异步发送候诊提醒, appointmentId: {}", appointmentId);

        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("预约不存在: " + appointmentId));

            Customer customer = customerRepository.findById(appointment.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("顾客不存在: " + appointment.getCustomerId()));

            String content = String.format("尊敬的%s，您的预约时间为%s，请提前到店候诊。",
                    customer.getName(), appointment.getAppointmentTime());

            MessageQueue queue = createMessageQueue(appointmentId, customer.getId(), customer.getPhone(),
                    null, MessageType.WAIT_REMINDER, MessageChannel.SMS, content, 10, null);

            processMessageQueue(queue.getId());

            log.info("候诊提醒发送成功, appointmentId: {}", appointmentId);
        } catch (Exception e) {
            log.error("发送候诊提醒失败, appointmentId: {}", appointmentId, e);
            saveFailedMessageLog(appointmentId, null, null, MessageType.WAIT_REMINDER,
                    MessageChannel.SMS, null, e.getMessage());
        }
    }

    @Async("triageTaskExecutor")
    @Transactional
    public void sendArrivalNotice(Long appointmentId) {
        log.info("异步发送到店通知, appointmentId: {}", appointmentId);

        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("预约不存在: " + appointmentId));

            Customer customer = customerRepository.findById(appointment.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("顾客不存在: " + appointment.getCustomerId()));

            String content = String.format("顾客%s已到店，请安排接待。手机号：%s",
                    customer.getName(), customer.getPhone());

            MessageQueue queue = createMessageQueue(appointmentId, customer.getId(), null,
                    null, MessageType.ARRIVAL_NOTICE, MessageChannel.IN_APP, content, 20, null);

            processMessageQueue(queue.getId());

            log.info("到店通知发送成功, appointmentId: {}", appointmentId);
        } catch (Exception e) {
            log.error("发送到店通知失败, appointmentId: {}", appointmentId, e);
            saveFailedMessageLog(appointmentId, null, null, MessageType.ARRIVAL_NOTICE,
                    MessageChannel.IN_APP, null, e.getMessage());
        }
    }

    @Async("triageTaskExecutor")
    @Transactional
    public void sendConsultationNotice(Long appointmentId, Long consultantId) {
        log.info("异步发送面诊通知, appointmentId: {}, consultantId: {}", appointmentId, consultantId);

        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("预约不存在: " + appointmentId));

            Customer customer = customerRepository.findById(appointment.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("顾客不存在: " + appointment.getCustomerId()));

            String content = String.format("请咨询师准备面诊，顾客：%s，手机号：%s，预约时间：%s",
                    customer.getName(), customer.getPhone(), appointment.getAppointmentTime());

            MessageQueue queue = createMessageQueue(appointmentId, customer.getId(), null,
                    null, MessageType.CONSULTATION_NOTICE, MessageChannel.IN_APP, content, 15, null);

            processMessageQueue(queue.getId());

            log.info("面诊通知发送成功, appointmentId: {}", appointmentId);
        } catch (Exception e) {
            log.error("发送面诊通知失败, appointmentId: {}", appointmentId, e);
            saveFailedMessageLog(appointmentId, null, null, MessageType.CONSULTATION_NOTICE,
                    MessageChannel.IN_APP, null, e.getMessage());
        }
    }

    @Async("triageTaskExecutor")
    @Transactional
    public void sendResultNotice(Long appointmentId) {
        log.info("异步发送结果通知, appointmentId: {}", appointmentId);

        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("预约不存在: " + appointmentId));

            Customer customer = customerRepository.findById(appointment.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("顾客不存在: " + appointment.getCustomerId()));

            String content = String.format("尊敬的%s，您的面诊已完成，请查看面诊结果和后续建议。", customer.getName());

            MessageQueue smsQueue = createMessageQueue(appointmentId, customer.getId(), customer.getPhone(),
                    null, MessageType.RESULT_NOTICE, MessageChannel.SMS, content, 5, null);
            processMessageQueue(smsQueue.getId());

            MessageQueue wechatQueue = createMessageQueue(appointmentId, customer.getId(), null,
                    null, MessageType.RESULT_NOTICE, MessageChannel.WECHAT, content, 5, null);
            processMessageQueue(wechatQueue.getId());

            log.info("结果通知发送成功, appointmentId: {}", appointmentId);
        } catch (Exception e) {
            log.error("发送结果通知失败, appointmentId: {}", appointmentId, e);
            saveFailedMessageLog(appointmentId, null, null, MessageType.RESULT_NOTICE,
                    MessageChannel.SMS, null, e.getMessage());
        }
    }

    @Async("triageTaskExecutor")
    @Transactional
    public void notifyInternalSystem(Long appointmentId, ConsultationResult result) {
        log.info("异步向内部系统回传面诊结果, appointmentId: {}", appointmentId);

        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("预约不存在: " + appointmentId));

            Customer customer = customerRepository.findById(appointment.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("顾客不存在: " + appointment.getCustomerId()));

            StringBuilder content = new StringBuilder();
            content.append("面诊结果回传：").append("\n");
            content.append("预约ID：").append(appointmentId).append("\n");
            content.append("顾客：").append(customer.getName()).append("\n");
            content.append("咨询师：").append(result.getConsultantName()).append("\n");
            content.append("医生：").append(result.getDoctorName()).append("\n");
            content.append("诊断：").append(result.getDiagnosis()).append("\n");
            content.append("建议：").append(result.getSuggestion()).append("\n");
            if (result.getTreatmentPlan() != null) {
                content.append("治疗方案：").append(result.getTreatmentPlan()).append("\n");
            }
            if (result.getFollowUpDate() != null) {
                content.append("复诊日期：").append(result.getFollowUpDate()).append("\n");
            }

            MessageQueue queue = createMessageQueue(appointmentId, customer.getId(), null,
                    null, MessageType.RESULT_NOTICE, MessageChannel.INTERNAL_SYSTEM, content.toString(), 8, null);

            processMessageQueue(queue.getId());

            log.info("内部系统通知发送成功, appointmentId: {}", appointmentId);
        } catch (Exception e) {
            log.error("向内部系统回传面诊结果失败, appointmentId: {}", appointmentId, e);
            saveFailedMessageLog(appointmentId, null, null, MessageType.RESULT_NOTICE,
                    MessageChannel.INTERNAL_SYSTEM, null, e.getMessage());
        }
    }

    @Transactional
    public void processMessageQueue(Long queueId) {
        log.info("处理消息队列, queueId: {}", queueId);

        MessageQueue queue = messageQueueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("消息队列记录不存在: " + queueId));

        if (queue.getStatus() != MessageQueueStatus.PENDING && queue.getStatus() != MessageQueueStatus.RETRYING) {
            log.warn("消息队列状态不允许发送, queueId: {}, status: {}", queueId, queue.getStatus());
            return;
        }

        MessageLog messageLog = messageLogRepository.findById(queue.getMessageLogId())
                .orElseThrow(() -> new IllegalArgumentException("消息日志不存在: " + queue.getMessageLogId()));

        try {
            queue.setStatus(MessageQueueStatus.SENDING);
            messageLog.setSendStatus(MessageQueueStatus.SENDING);
            messageQueueRepository.save(queue);
            messageLogRepository.save(messageLog);

            doSendMessage(queue);

            queue.setStatus(MessageQueueStatus.SUCCESS);
            queue.setSentTime(LocalDateTime.now());
            queue.setLastRetryTime(LocalDateTime.now());

            messageLog.setSendStatus(MessageQueueStatus.SUCCESS);
            messageLog.setIsSuccess(true);
            messageLog.setSentTime(LocalDateTime.now());
            messageLog.setLastRetryTime(LocalDateTime.now());
            messageLog.setRetryCount(queue.getRetryCount());

            messageQueueRepository.save(queue);
            messageLogRepository.save(messageLog);

            log.info("消息发送成功, queueId: {}, messageType: {}", queueId, queue.getMessageType());
        } catch (Exception e) {
            log.error("消息发送失败, queueId: {}, messageType: {}", queueId, queue.getMessageType(), e);

            int newRetryCount = queue.getRetryCount() + 1;
            boolean maxRetriesReached = newRetryCount >= queue.getMaxRetryCount();

            if (maxRetriesReached) {
                queue.setStatus(MessageQueueStatus.FAILED);
                messageLog.setSendStatus(MessageQueueStatus.FAILED);
                messageLog.setIsSuccess(false);
            } else {
                queue.setStatus(MessageQueueStatus.RETRYING);
                queue.setNextRetryTime(LocalDateTime.now().plusMinutes(5 + newRetryCount * 5L));
                messageLog.setSendStatus(MessageQueueStatus.RETRYING);
                messageLog.setNextRetryTime(queue.getNextRetryTime());
            }

            queue.setRetryCount(newRetryCount);
            queue.setLastRetryTime(LocalDateTime.now());
            queue.setLastFailReason(e.getMessage());

            messageLog.setRetryCount(newRetryCount);
            messageLog.setLastRetryTime(LocalDateTime.now());
            messageLog.setFailReason(e.getMessage());
            messageLog.setLastError(e.getMessage());

            messageQueueRepository.save(queue);
            messageLogRepository.save(messageLog);

            if (maxRetriesReached) {
                log.error("消息已达到最大重试次数, queueId: {}, retryCount: {}", queueId, newRetryCount);
            }
        }
    }

    private void doSendMessage(MessageQueue queue) {
        if (Boolean.TRUE.equals(triageConfig.getMessage().getMockEnabled())) {
            log.info("【模拟发送】消息类型: {}, 渠道: {}, 目标: {}, 内容: {}",
                    queue.getMessageType(), queue.getChannel(),
                    queue.getTargetPhone() != null ? queue.getTargetPhone() : queue.getTargetOpenid(),
                    queue.getContent());
            return;
        }

        log.info("实际发送消息, 类型: {}, 渠道: {}, 目标: {}",
                queue.getMessageType(), queue.getChannel(),
                queue.getTargetPhone() != null ? queue.getTargetPhone() : queue.getTargetOpenid());
    }

    @Transactional
    public void resendMessage(Long queueId) {
        log.info("手动重发消息, queueId: {}", queueId);

        MessageQueue queue = messageQueueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("消息队列记录不存在: " + queueId));

        if (queue.getStatus() == MessageQueueStatus.SUCCESS) {
            throw new IllegalStateException("消息已发送成功，无需重发");
        }

        queue.setStatus(MessageQueueStatus.RETRYING);
        queue.setRetryCount(0);
        queue.setNextRetryTime(null);
        queue.setLastFailReason(null);

        MessageLog messageLog = messageLogRepository.findById(queue.getMessageLogId())
                .orElseThrow(() -> new IllegalArgumentException("消息日志不存在: " + queue.getMessageLogId()));
        messageLog.setSendStatus(MessageQueueStatus.RETRYING);
        messageLog.setRetryCount(0);
        messageLog.setIsSuccess(null);
        messageLog.setFailReason(null);
        messageLog.setLastError(null);

        messageQueueRepository.save(queue);
        messageLogRepository.save(messageLog);

        processMessageQueue(queueId);

        log.info("手动重发消息完成, queueId: {}", queueId);
    }

    @Transactional
    public void resendMessages(List<Long> queueIds) {
        log.info("批量重发消息, queueIds: {}", queueIds);

        for (Long queueId : queueIds) {
            try {
                resendMessage(queueId);
            } catch (Exception e) {
                log.error("批量重发消息失败, queueId: {}", queueId, e);
            }
        }

        log.info("批量重发消息完成, 总数量: {}", queueIds.size());
    }

    @Transactional(readOnly = true)
    public Page<MessageQueue> getQueueList(Long storeId, MessageQueueStatus status, Pageable pageable) {
        log.info("查询消息队列列表, storeId: {}, status: {}", storeId, status);
        if (status != null) {
            return messageQueueRepository.findByStoreIdAndStatus(storeId, status, pageable);
        } else {
            return messageQueueRepository.findByStoreId(storeId, pageable);
        }
    }

    @Transactional(readOnly = true)
    public List<MessageQueue> getMessageQueueByAppointmentId(Long appointmentId) {
        log.info("查询预约的消息队列, appointmentId: {}", appointmentId);
        return messageQueueRepository.findByAppointmentId(appointmentId);
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void retryFailedMessages() {
        log.debug("定时任务开始：重试失败消息");

        try {
            List<MessageQueue> retryableMessages = messageQueueRepository.findByStatusAndNextRetryTimeBefore(
                    MessageQueueStatus.RETRYING, LocalDateTime.now());

            if (retryableMessages.isEmpty()) {
                log.debug("没有需要重试的消息");
                return;
            }

            log.info("找到 {} 条需要重试的消息", retryableMessages.size());

            for (MessageQueue queue : retryableMessages) {
                try {
                    processMessageQueue(queue.getId());
                } catch (Exception e) {
                    log.error("重试消息失败, queueId: {}", queue.getId(), e);
                }
            }

            log.info("定时任务完成：重试失败消息，处理了 {} 条", retryableMessages.size());
        } catch (Exception e) {
            log.error("定时任务异常：重试失败消息", e);
        }
    }

    private void sendMessage(Long appointmentId, Long customerId, String targetPhone,
                             MessageType messageType, MessageChannel channel, String content) {
        if (Boolean.TRUE.equals(triageConfig.getMessage().getMockEnabled())) {
            log.info("【模拟发送】消息类型: {}, 渠道: {}, 内容: {}", messageType, channel, content);
            saveMessageLog(appointmentId, customerId, targetPhone, messageType, channel, content, true, null);
            return;
        }

        log.info("发送消息, 类型: {}, 渠道: {}, 目标: {}", messageType, channel, targetPhone);
        saveMessageLog(appointmentId, customerId, targetPhone, messageType, channel, content, true, null);
    }

    private void saveMessageLog(Long appointmentId, Long customerId, String targetPhone,
                                MessageType messageType, MessageChannel channel, String content,
                                boolean isSuccess, String failReason) {
        MessageLog messageLog = MessageLog.builder()
                .appointmentId(appointmentId)
                .customerId(customerId)
                .messageType(messageType)
                .channel(channel)
                .content(content)
                .targetPhone(targetPhone)
                .sentTime(LocalDateTime.now())
                .isSuccess(isSuccess)
                .failReason(failReason)
                .build();
        messageLogRepository.save(messageLog);
    }

    private void saveFailedMessageLog(Long appointmentId, Long customerId, String targetPhone,
                                      MessageType messageType, MessageChannel channel, String content,
                                      String failReason) {
        MessageLog messageLog = MessageLog.builder()
                .appointmentId(appointmentId)
                .customerId(customerId)
                .messageType(messageType)
                .channel(channel)
                .content(content)
                .targetPhone(targetPhone)
                .sendStatus(MessageQueueStatus.FAILED)
                .isSuccess(false)
                .failReason(failReason)
                .sentTime(LocalDateTime.now())
                .retryCount(0)
                .maxRetryCount(3)
                .build();
        messageLogRepository.save(messageLog);
    }
}
