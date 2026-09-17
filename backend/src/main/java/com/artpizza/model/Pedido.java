package com.artpizza.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemPedido> itens = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_entrega", nullable = false)
    private TipoEntrega tipoEntrega;

    // Nulo quando tipoEntrega = RETIRADA
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endereco_id")
    private Endereco endereco;

    @Builder.Default
    @Column(name = "valor_frete", nullable = false)
    private BigDecimal valorFrete = BigDecimal.ZERO;

    @Column(name = "valor_total", nullable = false)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false)
    private FormaPagamento formaPagamento;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private StatusPedido status = StatusPedido.NOVO;

    @Builder.Default
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    // Preenchidos só quando formaPagamento = PIX (PixService.gerarCobranca).
    @Column(name = "pix_txid")
    private String pixTxId;

    @Column(name = "pix_copia_cola", columnDefinition = "TEXT")
    private String pixCopiaCola;
}
