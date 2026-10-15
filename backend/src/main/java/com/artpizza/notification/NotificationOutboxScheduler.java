package com.artpizza.notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDateTime;
@Component @RequiredArgsConstructor
@ConditionalOnProperty(name="integration.notification.enabled",havingValue="true")
public class NotificationOutboxScheduler {
    private final NotificationEventRepository events;
    private final NotificationOutboxWorker worker;
    @Scheduled(fixedDelayString="${integration.notification.poll-ms:5000}")
    public void dispatch(){for(String id:events.due(LocalDateTime.now(),PageRequest.of(0,50))) worker.deliver(id);}
}
