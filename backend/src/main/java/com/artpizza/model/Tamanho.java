package com.artpizza.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tamanho")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tamanho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ex: "Pequena", "Média", "Grande", "Família"
    @Column(nullable = false, unique = true)
    private String nome;

    @Column(name = "preco_base", nullable = false)
    private BigDecimal precoBase;

    // Regra central do cardápio: quantos sabores esse tamanho aceita.
    @Column(name = "max_sabores", nullable = false)
    private Integer maxSabores;

    @Builder.Default
    private Boolean ativo = true;
}
