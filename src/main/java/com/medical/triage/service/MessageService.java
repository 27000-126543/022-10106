package com.medical.triage.service;

import com.medical.triage.config.TriageConfig;
import com.medical.triage.entity.Appointment;
import com.medical.triage.entity.ConsultationResult;
import com.medical.triage.entity.Customer;
import com.medical.triage.entity.MessageLog;
import com.medical.triage.enums.MessageChannel;
import com.medical.triage.enums.MessageType;
import com.medical.triage.repository.AppointmentRepository;
import com.medical.triage.repository.CustomerRepository;
import com.medical.triage.repository.MessageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final MessageLogRepository messageLogRepository;
    private final TriageConfig triageConfig;

    @Async
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

            sendMessage(appointmentId, customer.getId(), customer.getPhone(),
                    MessageType.WAIT_REMINDER, MessageChannel.SMS, content);

            log.info("候诊提醒发送成功, appointmentId: {}", appointmentId);
        } catch (Exception e) {
            log.error("发送候诊提醒失败, appointmentId: {}", appointmentId, e);
            saveMessageLog(appointmentId, null, null, MessageType.WAIT_REMINDER,
                    MessageChannel.SMS, null, false, e.getMessage());
        }
    }

    @Async
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

            sendMessage(appointmentId, customer.getId(), null,
                    MessageType.ARRIVAL_NOTICE, MessageChannel.IN_APP, content);

            log.info("到店通知发送成功, appointmentId: {}", appointmentId);
        } catch (Exception e) {
            log.error("发送到店通知失败, appointmentId: {}", appointmentId, e);
            saveMessageLog(appointmentId, null, null, MessageType.ARRIVAL_NOTICE,
                    MessageChannel.IN_APP, null, false, e.getMessage());
        }
    }

    @Async
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

            sendMessage(appointmentId, customer.getId(), null,
                    MessageType.CONSULTATION_NOTICE, MessageChannel.IN_APP, content);

            log.info("面诊通知发送成功, appointmentId: {}", appointmentId);
        } catch (Exception e) {
            log.error("发送面诊通知失败, appointmentId: {}", appointmentId, e);
            saveMessageLog(appointmentId, null, null, MessageType.CONSULTATION_NOTICE,
                    MessageChannel.IN_APP, null, false, e.getMessage());
        }
    }

    @Async
    @Transactional
    public void sendResultNotice(Long appointmentId) {
        log.info("异步发送结果通知, appointmentId: {}", appointmentId);

        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("预约不存在: " + appointmentId));

            Customer customer = customerRepository.findById(appointment.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("顾客不存在: " + appointment.getCustomerId()));

            String content = String.format("尊敬的%s，您的面诊已完成，请查看面诊结果和后续建议。", customer.getName());

            sendMessage(appointmentId, customer.getId(), customer.getPhone(),
                    MessageType.RESULT_NOTICE, MessageChannel.SMS, content);

            sendMessage(appointmentId, customer.getId(), null,
                    MessageType.RESULT_NOTICE, MessageChannel.WECHAT, content);

            log.info("结果通知发送成功, appointmentId: {}", appointmentId);
        } catch (Exception e) {
            log.error("发送结果通知失败, appointmentId: {}", appointmentId, e);
            saveMessageLog(appointmentId, null, null, MessageType.RESULT_NOTICE,
                    MessageChannel.SMS, null, false, e.getMessage());
        }
    }

    @Async
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

            sendMessage(appointmentId, customer.getId(), null,
                    MessageType.RESULT_NOTICE, MessageChannel.INTERNAL_SYSTEM, content.toString());

            log.info("内部系统通知发送成功, appointmentId: {}", appointmentId);
        } catch (Exception e) {
            log.error("向内部系统回传面诊结果失败, appointmentId: {}", appointmentId, e);
            saveMessageLog(appointmentId, null, null, MessageType.RESULT_NOTICE,
                    MessageChannel.INTERNAL_SYSTEM, null, false, e.getMessage());
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
}
