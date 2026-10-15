package com.artpizza.notification;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name="notification_outbox") @Getter @Setter @NoArgsConstructor
public class NotificationEvent {
    @Id @Column(length=36) private String id;
    @Column(name="pedido_id",nullable=false) private Long pedidoId;
    @Column(name="event_type",length=40,nullable=false) private String eventType;
    @Column(length=120,nullable=false) private String recipient;
    @Column(length=120,nullable=false) private String title;
    @Column(length=1000,nullable=false) private String message;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @Column(name="next_attempt_at",nullable=false) private LocalDateTime nextAttemptAt;
    @Column(nullable=false) private int attempts;
    @Column(name="delivered_at") private LocalDateTime deliveredAt;
    @Column(name="last_error",length=40) private String lastError;
}
