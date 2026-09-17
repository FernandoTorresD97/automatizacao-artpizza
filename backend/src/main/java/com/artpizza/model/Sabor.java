package com.artpizza.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "sabor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sabor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nome;

    @Column(length = 500)
    private String descricao;

    // Adicional cobrado quando esse sabor é escolhido (ex: sabores especiais/premium).
    // Zero para sabores inclusos no preço base.
    @Builder.Default
    @Column(name = "preco_adicional", nullable = false)
    private BigDecimal precoAdicional = BigDecimal.ZERO;

    @Builder.Default
    private Boolean disponivel = true;
}
