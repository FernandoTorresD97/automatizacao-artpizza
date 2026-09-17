package com.artpizza.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Uma linha por número de telefone. É isso que permite várias pessoas
 * conversando ao mesmo tempo com o mesmo WhatsApp da pizzaria sem misturar
 * pedidos: cada mensagem que chega é roteada pelo telefone do remetente até
 * a sessão correspondente.
 */
@Entity
@Table(name = "sessao_conversa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessaoConversa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telefone_whatsapp", nullable = false, unique = true)
    private String telefoneWhatsapp;

    private String nomeCliente;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private EstadoConversa estado = EstadoConversa.MENU;

    // Itens já fechados nesta conversa (pizzas e/ou produtos), acumulados
    // enquanto o bot pergunta "quer mais alguma coisa?".
    @OneToMany(mappedBy = "sessao", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SessaoConversaItem> itens = new ArrayList<>();

    // Item (pizza) sendo montado no momento — só é adicionado a `itens`
    // quando o cliente termina de escolher tamanho + sabores + observação.
    @Column(name = "tamanho_id")
    private Long tamanhoId;

    @ElementCollection
    @CollectionTable(name = "sessao_conversa_sabor", joinColumns = @JoinColumn(name = "sessao_id"))
    @Column(name = "sabor_id")
    @Builder.Default
    private List<Long> saboresIds = new ArrayList<>();

    private String observacao;

    // Produto sendo escolhido no momento, enquanto se aguarda a quantidade.
    @Column(name = "produto_id_atual")
    private Long produtoIdAtual;

    @Enumerated(EnumType.STRING)
    private TipoEntrega tipoEntrega;

    private String bairro;
    private String rua;
    private String numero;

    @Enumerated(EnumType.STRING)
    private FormaPagamento formaPagamento;

    @Builder.Default
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    public void reiniciar() {
        this.estado = EstadoConversa.MENU;
        this.itens.clear();
        this.tamanhoId = null;
        this.saboresIds.clear();
        this.observacao = null;
        this.produtoIdAtual = null;
        this.tipoEntrega = null;
        this.bairro = null;
        this.rua = null;
        this.numero = null;
        this.formaPagamento = null;
    }
}
