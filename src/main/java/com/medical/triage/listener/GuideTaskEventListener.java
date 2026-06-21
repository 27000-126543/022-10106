package com.medical.triage.listener;

import com.medical.triage.enums.GuideTaskStatus;
import com.medical.triage.event.GuideTaskStatusChangedEvent;
import com.medical.triage.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GuideTaskEventListener {

    private final MessageService messageService;

    @Async
    @EventListener
    public void onGuideTaskStatusChanged(GuideTaskStatusChangedEvent event) {
        log.info("异步处理导诊任务状态变更事件, taskId: {}, from: {}, to: {}",
                event.getTaskId(), event.getFromStatus(), event.getToStatus());
        try {
            GuideTaskStatus toStatus = event.getToStatus();
            switch (toStatus) {
                case IN_PROGRESS:
                    log.info("任务开始执行通知, taskId: {}", event.getTaskId());
                    break;
                case COMPLETED:
                    log.info("任务完成通知, taskId: {}", event.getTaskId());
                    messageService.sendConsultationNotice(event.getAppointmentId(), null);
                    break;
                case REASSIGNED:
                    log.info("任务改派通知, taskId: {}", event.getTaskId());
                    break;
                case CANCELLED:
                    log.info("任务取消通知, taskId: {}", event.getTaskId());
                    break;
                default:
                    log.debug("无需处理的任务状态, taskId: {}, status: {}", event.getTaskId(), toStatus);
                    break;
            }
        } catch (Exception e) {
            log.error("导诊任务状态变更处理失败, taskId: {}", event.getTaskId(), e);
        }
    }
}
