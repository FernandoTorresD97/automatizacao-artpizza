package com.artpizza.pagamento.sandbox;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name="sandbox_payment_event") @Getter @Setter @NoArgsConstructor
public class SandboxPaymentEvent {
    @Id @Column(length=80) private String id;
    @Column(name="payment_id",nullable=false,length=36) private String paymentId;
    @Column(name="payload_hash",nullable=false,length=64) private String payloadHash;
    @Column(name="received_at",nullable=false) private LocalDateTime receivedAt;
}
