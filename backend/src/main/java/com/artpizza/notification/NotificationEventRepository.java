package com.artpizza.notification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.*;
public interface NotificationEventRepository extends JpaRepository<NotificationEvent,String> {
    @Query("select n.id from NotificationEvent n where n.deliveredAt is null and n.nextAttemptAt <= :now order by n.createdAt")
    List<String> due(LocalDateTime now, Pageable page);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select n from NotificationEvent n where n.id=:id")
    Optional<NotificationEvent> lock(String id);
}
