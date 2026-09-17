package com.artpizza.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * MVP do frete: valor fixo por bairro, configurável no painel administrativo.
 * Evita depender de uma API de mapas/rotas já na Fase 1 (isso fica pra Fase 3+).
 */
@Entity
@Table(name = "regiao_frete")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegiaoFrete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String bairro;

    @Column(name = "valor_frete", nullable = false)
    private BigDecimal valorFrete;

    @Builder.Default
    private Boolean ativo = true;
}
