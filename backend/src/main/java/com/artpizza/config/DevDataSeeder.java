package com.artpizza.config;

import com.artpizza.model.Produto;
import com.artpizza.model.RegiaoFrete;
import com.artpizza.model.Sabor;
import com.artpizza.model.Tamanho;
import com.artpizza.repository.ProdutoRepository;
import com.artpizza.repository.RegiaoFreteRepository;
import com.artpizza.repository.SaborRepository;
import com.artpizza.repository.TamanhoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Dados de exemplo só para ambiente de desenvolvimento (H2 em memória),
 * pra dar pra testar a API/painel sem precisar cadastrar cardápio na mão.
 * Em produção o cardápio é gerenciado pelo painel administrativo.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private final TamanhoRepository tamanhoRepository;
    private final SaborRepository saborRepository;
    private final RegiaoFreteRepository regiaoFreteRepository;
    private final ProdutoRepository produtoRepository;

    @Override
    public void run(String... args) {
        tamanhoRepository.saveAll(java.util.List.of(
                Tamanho.builder().nome("Pequena").precoBase(new BigDecimal("15.99")).maxSabores(1).build(),
                Tamanho.builder().nome("Média").precoBase(new BigDecimal("25.99")).maxSabores(2).build(),
                Tamanho.builder().nome("Grande").precoBase(new BigDecimal("45.99")).maxSabores(3).build(),
                Tamanho.builder().nome("Família").precoBase(new BigDecimal("69.99")).maxSabores(4).build()
        ));

        saborRepository.saveAll(java.util.List.of(
                sabor("Calabresa", "Calabresa, cebola e orégano"),
                sabor("Baiana", "Calabresa picante, pimenta e pimentão"),
                sabor("4 Queijos", "Mussarela, parmesão, provolone e gorgonzola"),
                sabor("Portuguesa", "Presunto, ovo, cebola, ervilha e azeitona"),
                sabor("Frango com Catupiry", "Frango desfiado, catupiry e milho"),
                sabor("Lombinho", "Lombinho, cream cheese e cebola caramelizada"),
                sabor("Moda da Casa", "Calabresa, bacon, catupiry e milho"),
                sabor("Bacon", "Bacon crocante, mussarela e cheddar"),
                sabor("Milho Verde", "Milho, mussarela e parmesão"),
                sabor("Italiana", "Salame italiano, rúcula, tomate seco e parmesão"),
                sabor("Paraguaia", "Carne moída, cheddar, cebola e pimenta"),
                sabor("Rua da Mata", "Nata, frango, bacon e cebola"),
                sabor("IncoPop", "Presunto, palmito e mussarela"),
                sabor("Areal", "Carne de sol, queijo coalho e cebola"),
                sabor("Brigadeiro", "Brigadeiro e granulado de chocolate"),
                sabor("Romeu e Julieta", "Queijo e goiabada"),
                sabor("Doce de Leite com Paçoca", "Doce de leite finalizado com farofa de paçoca")
        ));

        regiaoFreteRepository.saveAll(java.util.List.of(
                RegiaoFrete.builder().bairro("Centro").valorFrete(new BigDecimal("5.00")).build(),
                RegiaoFrete.builder().bairro("Itapicuru").valorFrete(new BigDecimal("7.00")).build(),
                RegiaoFrete.builder().bairro("Coplan").valorFrete(new BigDecimal("8.00")).build(),
                RegiaoFrete.builder().bairro("Suzana").valorFrete(new BigDecimal("10.00")).build()
        ));

        produtoRepository.saveAll(java.util.List.of(
                Produto.builder().nome("Batata Frita Tradicional").preco(new BigDecimal("21.90")).categoria("PETISCO").build(),
                Produto.builder().nome("Batata Frita Turbinada").preco(new BigDecimal("31.90")).categoria("PETISCO").build(),
                Produto.builder().nome("Isca de Frango + Batata").preco(new BigDecimal("30.00")).categoria("PETISCO").build(),
                Produto.builder().nome("Surpresa de Queijo").preco(new BigDecimal("30.00")).categoria("PETISCO").build(),
                Produto.builder().nome("Filé com Fritas").preco(new BigDecimal("40.00")).categoria("PETISCO").build(),
                Produto.builder().nome("Coxinha da Asa + Batata").preco(new BigDecimal("30.00")).categoria("PETISCO").build(),
                Produto.builder().nome("Espetinho de Carne").preco(new BigDecimal("15.00")).categoria("PETISCO").build(),
                Produto.builder().nome("Picanha na Chapa à Moda da Casa").preco(new BigDecimal("80.00")).categoria("PETISCO").build(),
                Produto.builder().nome("Contra Filé na Chapa").preco(new BigDecimal("70.00")).categoria("PETISCO").build(),
                Produto.builder().nome("Refrigerante 1L").preco(new BigDecimal("10.00")).categoria("BEBIDA").build(),
                Produto.builder().nome("Refrigerante Lata").preco(new BigDecimal("6.00")).categoria("BEBIDA").build(),
                Produto.builder().nome("Refrigerante Litrinho").preco(new BigDecimal("6.00")).categoria("BEBIDA").build(),
                Produto.builder().nome("Heineken Long Neck").preco(new BigDecimal("10.00")).categoria("BEBIDA").build(),
                Produto.builder().nome("Heineken 0 Álcool Long Neck").preco(new BigDecimal("12.00")).categoria("BEBIDA").build(),
                Produto.builder().nome("Cerveja Lata").preco(new BigDecimal("6.00")).categoria("BEBIDA").build(),
                Produto.builder().nome("Suco").preco(new BigDecimal("10.00")).categoria("BEBIDA").build(),
                Produto.builder().nome("Água sem Gás").preco(new BigDecimal("4.00")).categoria("BEBIDA").build(),
                Produto.builder().nome("Água com Gás").preco(new BigDecimal("5.00")).categoria("BEBIDA").build()
        ));
    }

    private Sabor sabor(String nome, String descricao) {
        return Sabor.builder().nome(nome).descricao(descricao).precoAdicional(BigDecimal.ZERO).build();
    }
}
