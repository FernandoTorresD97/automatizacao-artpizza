package com.artpizza.service;

import com.artpizza.dto.FaturamentoResponseDTO;
import com.artpizza.model.FormaPagamento;
import com.artpizza.model.Pedido;
import com.artpizza.model.StatusPedido;
import com.artpizza.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FaturamentoService {
    private static final List<StatusPedido> STATUS_FATURADOS = List.of(
            StatusPedido.CONFIRMADO, StatusPedido.EM_PREPARO, StatusPedido.PRONTO,
            StatusPedido.SAIU_PARA_ENTREGA, StatusPedido.ENTREGUE
    );

    private final PedidoRepository pedidoRepository;

    @Transactional(readOnly = true)
    public FaturamentoResponseDTO resumir(LocalDate data) {
        List<Pedido> pedidos = pedidoRepository.findByCriadoEmGreaterThanEqualAndCriadoEmLessThanAndStatusIn(
                data.atStartOfDay(), data.plusDays(1).atStartOfDay(), STATUS_FATURADOS);

        BigDecimal valorTotal = pedidos.stream().map(Pedido::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal valorFrete = pedidos.stream().map(Pedido::getValorFrete)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<FormaPagamento, List<Pedido>> porForma = new EnumMap<>(FormaPagamento.class);
        pedidos.forEach(pedido -> porForma.computeIfAbsent(pedido.getFormaPagamento(), ignored -> new java.util.ArrayList<>())
                .add(pedido));

        List<FaturamentoResponseDTO.PorFormaPagamentoDTO> detalhes = Arrays.stream(FormaPagamento.values())
                .map(forma -> {
                    List<Pedido> pedidosDaForma = porForma.getOrDefault(forma, List.of());
                    BigDecimal total = pedidosDaForma.stream().map(Pedido::getValorTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new FaturamentoResponseDTO.PorFormaPagamentoDTO(forma, pedidosDaForma.size(), total);
                }).toList();

        BigDecimal ticketMedio = pedidos.isEmpty() ? BigDecimal.ZERO
                : valorTotal.divide(BigDecimal.valueOf(pedidos.size()), 2, RoundingMode.HALF_UP);

        return FaturamentoResponseDTO.builder()
                .data(data)
                .quantidadePedidos(pedidos.size())
                .valorTotal(valorTotal)
                .valorItens(valorTotal.subtract(valorFrete))
                .valorFrete(valorFrete)
                .ticketMedio(ticketMedio)
                .porFormaPagamento(detalhes)
                .build();
    }
}
