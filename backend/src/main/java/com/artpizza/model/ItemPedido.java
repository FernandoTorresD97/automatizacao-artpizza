package com.artpizza.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "item_pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private TipoItem tipo = TipoItem.PIZZA;

    // Preenchido quando tipo = PIZZA. Nulo quando tipo = PRODUTO.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tamanho_id")
    private Tamanho tamanho;

    @ManyToMany
    @JoinTable(
            name = "item_pedido_sabor",
            joinColumns = @JoinColumn(name = "item_pedido_id"),
            inverseJoinColumns = @JoinColumn(name = "sabor_id")
    )
    @Builder.Default
    private List<Sabor> sabores = new ArrayList<>();

    // Preenchido quando tipo = PRODUTO. Nulo quando tipo = PIZZA.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @Builder.Default
    @Column(nullable = false)
    private Integer quantidade = 1;

    private String observacao;

    // Preço já calculado de UMA unidade deste item (tamanho + adicionais dos
    // sabores, ou preço do produto), congelado no momento do pedido. O total
    // da linha é precoUnitario * quantidade.
    @Column(name = "preco_unitario", nullable = false)
    private BigDecimal precoUnitario;

    public BigDecimal getPrecoTotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
