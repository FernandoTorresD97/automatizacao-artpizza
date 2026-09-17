package com.artpizza.repository;

import com.artpizza.model.Pedido;
import com.artpizza.model.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByStatusOrderByCriadoEmAsc(StatusPedido status);
    List<Pedido> findByStatusInOrderByCriadoEmAsc(List<StatusPedido> status);
    Optional<Pedido> findByPixTxId(String pixTxId);
    List<Pedido> findByCriadoEmGreaterThanEqualAndCriadoEmLessThanAndStatusIn(
            LocalDateTime inicio, LocalDateTime fim, List<StatusPedido> status);
}
