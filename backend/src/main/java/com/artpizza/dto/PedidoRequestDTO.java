package com.artpizza.dto;

import com.artpizza.model.FormaPagamento;
import com.artpizza.model.TipoEntrega;
import com.artpizza.model.TipoItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PedidoRequestDTO {

    @NotBlank @Size(max = 120)
    private String nomeCliente;

    @NotBlank @Size(max = 20)
    private String telefoneWhatsapp;

    @NotEmpty @Size(max = 20)
    @Valid
    private List<ItemPedidoRequestDTO> itens;

    @NotNull
    private TipoEntrega tipoEntrega;

    // Obrigatório apenas quando tipoEntrega = ENTREGA
    @Valid
    private EnderecoRequestDTO endereco;

    @NotNull
    private FormaPagamento formaPagamento;

    @Getter
    @Setter
    public static class ItemPedidoRequestDTO {
        @NotNull
        private TipoItem tipo;

        // Obrigatórios quando tipo = PIZZA; ignorados quando tipo = PRODUTO.
        private Long tamanhoId;
        @Size(max = 4)
        private List<Long> saboresIds;

        // Obrigatório quando tipo = PRODUTO; ignorado quando tipo = PIZZA.
        private Long produtoId;

        @NotNull @Min(1) @Max(100)
        private Integer quantidade = 1;

        @Size(max = 500)
        private String observacao;
    }

    @Getter
    @Setter
    public static class EnderecoRequestDTO {
        @NotBlank @Size(max = 100)
        private String bairro;
        @NotBlank @Size(max = 160)
        private String rua;
        @NotBlank @Size(max = 30)
        private String numero;
        @Size(max = 160)
        private String complemento;
    }
}
