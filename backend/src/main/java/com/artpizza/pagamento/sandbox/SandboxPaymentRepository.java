package com.artpizza.pagamento.sandbox;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface SandboxPaymentRepository extends JpaRepository<SandboxPayment,String> {
    Optional<SandboxPayment> findByPedidoId(Long pedidoId);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select p from SandboxPayment p where p.id=:id")
    Optional<SandboxPayment> lock(String id);
}
