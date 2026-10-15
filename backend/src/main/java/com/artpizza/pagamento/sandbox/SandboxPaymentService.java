package com.artpizza.pagamento.sandbox;

import com.artpizza.repository.PedidoRepository;
import com.artpizza.model.*;
import com.artpizza.service.PedidoService;
import com.artpizza.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import jakarta.validation.constraints.*;

@Service @RequiredArgsConstructor
public class SandboxPaymentService {
    public record Event(@NotBlank @Size(max=80) String eventId,
        @NotBlank @Size(max=36) String paymentId, @NotNull Long pedidoId,
        @NotNull @DecimalMin("0.01") @Digits(integer=36,fraction=2) BigDecimal amount,
        @Pattern(regexp="BRL") @NotNull String currency,
        @Pattern(regexp="APPROVED|DECLINED") @NotNull String status) {}
    private final PedidoRepository pedidos;
    private final PedidoService pedidoService;
    private final SandboxPaymentRepository payments;
    private final SandboxPaymentEventRepository events;

    @Transactional public SandboxPayment create(Long pedidoId) {
        var pedido=pedidos.lockById(pedidoId).orElseThrow(()->new RecursoNaoEncontradoException("Pedido não encontrado"));
        var existing=payments.findByPedidoId(pedidoId);
        if(existing.isPresent()) return existing.get();
        if(pedido.getStatus()!=StatusPedido.AGUARDANDO_PAGAMENTO || pedido.getFormaPagamento()!=FormaPagamento.PIX)
            throw new RegraNegocioException("Sandbox exige um pedido PIX aguardando pagamento");
        var payment=new SandboxPayment();payment.setId(UUID.randomUUID().toString());payment.setPedidoId(pedidoId);
        payment.setAmount(pedido.getValorTotal());payment.setCurrency("BRL");payment.setStatus("PENDING");payment.setCreatedAt(LocalDateTime.now());
        return payments.save(payment);
    }

    @Transactional public SandboxPayment process(Event event) {
        var pedido=pedidos.lockById(event.pedidoId()).orElseThrow(()->new RecursoNaoEncontradoException("Pedido não encontrado"));
        var payment=payments.lock(event.paymentId()).orElseThrow(()->new RecursoNaoEncontradoException("Cobrança sandbox não encontrada"));
        if(!payment.getPedidoId().equals(event.pedidoId()) || !"BRL".equals(event.currency())
            || payment.getAmount().compareTo(event.amount())!=0 || pedido.getValorTotal().compareTo(event.amount())!=0)
            throw new RegraNegocioException("Pedido, valor ou moeda não correspondem à cobrança");
        if(!Set.of("APPROVED","DECLINED").contains(event.status())) throw new RegraNegocioException("Resultado inválido");
        String hash=hash(event);
        var existing=events.findById(event.eventId());
        if(existing.isPresent()) {
            if(!existing.get().getPayloadHash().equals(hash)) throw new RegraNegocioException("Evento já usado com outro conteúdo");
            return payment;
        }
        if(!"PENDING".equals(payment.getStatus()) && !payment.getStatus().equals(event.status()))
            throw new RegraNegocioException("Uma cobrança encerrada não pode mudar de resultado");
        if("PENDING".equals(payment.getStatus())) {
            if(pedido.getStatus()!=StatusPedido.AGUARDANDO_PAGAMENTO) throw new RegraNegocioException("Pedido não está aguardando pagamento");
            if("APPROVED".equals(event.status())) pedidoService.confirmarPagamento(pedido.getId());
            payment.setStatus(event.status());
        }
        var received=new SandboxPaymentEvent();received.setId(event.eventId());received.setPaymentId(payment.getId());
        received.setPayloadHash(hash);received.setReceivedAt(LocalDateTime.now());events.save(received);
        return payment;
    }
    private String hash(Event e) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
            (e.paymentId()+"|"+e.pedidoId()+"|"+e.amount().stripTrailingZeros().toPlainString()+"|"+e.currency()+"|"+e.status()).getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
    }
}
