package com.artpizza.dto;

import com.artpizza.model.FormaPagamento;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
public record FaturamentoResponseDTO(
        LocalDate data,
        int quantidadePedidos,
        BigDecimal valorTotal,
        BigDecimal valorItens,
        BigDecimal valorFrete,
        BigDecimal ticketMedio,
        List<PorFormaPagamentoDTO> porFormaPagamento) {

    @Builder
    public record PorFormaPagamentoDTO(FormaPagamento formaPagamento, int quantidade, BigDecimal valorTotal) {}
}
