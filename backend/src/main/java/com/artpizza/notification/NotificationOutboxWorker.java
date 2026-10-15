package com.artpizza.notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
@Service @RequiredArgsConstructor
public class NotificationOutboxWorker {
    private final NotificationEventRepository events;
    private final NotificationClient client;
    @Transactional public void deliver(String id) {
        var event=events.lock(id).orElse(null);var now=LocalDateTime.now();
        if(event==null || event.getDeliveredAt()!=null || event.getNextAttemptAt().isAfter(now)) return;
        event.setAttempts(event.getAttempts()+1);
        try {client.send(event);event.setDeliveredAt(LocalDateTime.now());event.setLastError(null);}
        catch(RuntimeException ex) {
            event.setLastError("NOTIFICATION_API_UNAVAILABLE");
            event.setNextAttemptAt(now.plusSeconds(Math.min(3600,5L << Math.min(10,event.getAttempts()-1))));
        }
    }
}
