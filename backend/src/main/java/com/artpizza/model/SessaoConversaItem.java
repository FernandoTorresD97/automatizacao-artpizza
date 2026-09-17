package com.artpizza.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Um item já fechado pelo cliente durante a conversa (ex: "Grande, calabresa
 * + frango" ou "2x Coca-Cola"), antes do pedido final ser criado. A
 * SessaoConversa acumula vários desses enquanto pergunta "quer mais alguma
 * coisa?" — é isso que permite várias pizzas e produtos no mesmo pedido.
 */
@Entity
@Table(name = "sessao_conversa_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessaoConversaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sessao_id", nullable = false)
    private SessaoConversa sessao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoItem tipo;

    // Preenchido quando tipo = PIZZA
    private Long tamanhoId;

    @ElementCollection
    @CollectionTable(name = "sessao_conversa_item_sabor", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "sabor_id")
    @Builder.Default
    private List<Long> saboresIds = new ArrayList<>();

    // Preenchido quando tipo = PRODUTO
    private Long produtoId;

    @Builder.Default
    @Column(nullable = false)
    private Integer quantidade = 1;

    private String observacao;
}
