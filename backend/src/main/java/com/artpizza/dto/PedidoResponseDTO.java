package com.artpizza.dto;

import com.artpizza.model.FormaPagamento;
import com.artpizza.model.Pedido;
import com.artpizza.model.StatusPedido;
import com.artpizza.model.TipoEntrega;
import com.artpizza.model.TipoItem;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class PedidoResponseDTO {

    private Long id;
    private String nomeCliente;
    private String telefoneWhatsapp;
    private List<ItemDTO> itens;
    private TipoEntrega tipoEntrega;
    private String bairroEntrega;
    private BigDecimal valorFrete;
    private BigDecimal valorTotal;
    private FormaPagamento formaPagamento;
    private StatusPedido status;
    private LocalDateTime criadoEm;
    private String pixCopiaCola;

    public static PedidoResponseDTO from(Pedido pedido) {
        return PedidoResponseDTO.builder()
                .id(pedido.getId())
                .nomeCliente(pedido.getCliente().getNome())
                .telefoneWhatsapp(pedido.getCliente().getTelefoneWhatsapp())
                .itens(pedido.getItens().stream().map(ItemDTO::from).collect(Collectors.toList()))
                .tipoEntrega(pedido.getTipoEntrega())
                .bairroEntrega(pedido.getEndereco() != null ? pedido.getEndereco().getBairro() : null)
                .valorFrete(pedido.getValorFrete())
                .valorTotal(pedido.getValorTotal())
                .formaPagamento(pedido.getFormaPagamento())
                .status(pedido.getStatus())
                .criadoEm(pedido.getCriadoEm())
                .pixCopiaCola(pedido.getPixCopiaCola())
                .build();
    }

    @Getter
    @Builder
    public static class ItemDTO {
        private TipoItem tipo;
        private String nome;
        private List<String> sabores;
        private Integer quantidade;
        private String observacao;
        private BigDecimal precoUnitario;
        private BigDecimal precoTotal;

        public static ItemDTO from(com.artpizza.model.ItemPedido item) {
            String nome = item.getTipo() == TipoItem.PIZZA
                    ? item.getTamanho().getNome()
                    : item.getProduto().getNome();

            List<String> sabores = item.getTipo() == TipoItem.PIZZA
                    ? item.getSabores().stream().map(com.artpizza.model.Sabor::getNome).collect(Collectors.toList())
                    : List.of();

            return ItemDTO.builder()
                    .tipo(item.getTipo())
                    .nome(nome)
                    .sabores(sabores)
                    .quantidade(item.getQuantidade())
                    .observacao(item.getObservacao())
                    .precoUnitario(item.getPrecoUnitario())
                    .precoTotal(item.getPrecoTotal())
                    .build();
        }
    }
}
