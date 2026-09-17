package com.artpizza.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "produto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ex: "Coca-Cola Lata 350ml", "Pudim"
    @Column(nullable = false, unique = true)
    private String nome;

    @Column(nullable = false)
    private BigDecimal preco;

    // Livre por enquanto (BEBIDA, SOBREMESA...); vira enum se a lista crescer
    // e precisar de regras específicas por categoria.
    private String categoria;

    @Builder.Default
    private Boolean disponivel = true;
}
