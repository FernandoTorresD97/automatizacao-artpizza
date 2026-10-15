package com.artpizza.pagamento.sandbox;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name="sandbox_payment") @Getter @Setter @NoArgsConstructor
public class SandboxPayment {
    @Id @Column(length=36) private String id;
    @Column(name="pedido_id",nullable=false,unique=true) private Long pedidoId;
    @Column(nullable=false,precision=38,scale=2) private BigDecimal amount;
    @Column(nullable=false,length=3) private String currency;
    @Column(nullable=false,length=20) private String status;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
}
