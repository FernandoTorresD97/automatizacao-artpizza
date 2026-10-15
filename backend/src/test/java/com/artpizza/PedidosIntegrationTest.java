package com.artpizza;

import com.artpizza.model.*;
import com.artpizza.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@Transactional
class PedidosIntegrationTest {
    static final EmbeddedPostgres PG = startPostgres();
    static EmbeddedPostgres startPostgres() {
        try { return EmbeddedPostgres.builder().setPort(0).start(); }
        catch (IOException ex) { throw new ExceptionInInitializerError(ex); }
    }
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> PG.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("admin.username", () -> "integration-admin");
        registry.add("admin.password", () -> "integration-password");
        registry.add("pix.chave", () -> "demo@example.invalid");
    }
    @AfterAll static void stopPostgres() throws IOException { PG.close(); }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PedidoRepository pedidos;
    @Autowired ClienteRepository clientes;
    @Autowired ProdutoRepository produtos;
    @Autowired TamanhoRepository tamanhos;
    @Autowired SaborRepository sabores;
    @Autowired RegiaoFreteRepository fretes;

    Map<String, Object> produtoPayload() {
        Long id = produtos.findAll().getFirst().getId();
        return new HashMap<>(Map.of("nomeCliente", "Cliente fictício", "telefoneWhatsapp", "5500000000000",
            "tipoEntrega", "RETIRADA", "formaPagamento", "DINHEIRO",
            "itens", List.of(new HashMap<>(Map.of("tipo", "PRODUTO", "produtoId", id, "quantidade", 2)))));
    }
    long criar(Map<String, Object> payload) throws Exception {
        String body = mvc.perform(post("/api/pedidos").contentType("application/json").content(json.writeValueAsString(payload)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }
    void transicao(long id, StatusPedido status, int expected) throws Exception {
        mvc.perform(patch("/api/pedidos/{id}/status", id).with(httpBasic("integration-admin", "integration-password"))
            .contentType("application/json").content(json.writeValueAsString(Map.of("status", status))))
            .andExpect(status().is(expected));
    }
    @Test void migracoesPostgresCriamCardapio() {
        assertThat(produtos.count()).isEqualTo(18);
        assertThat(tamanhos.count()).isEqualTo(4);
        assertThat(sabores.count()).isEqualTo(17);
    }
    @Test void retiradaCalculaPrecoNoServidor() throws Exception {
        var payload = produtoPayload(); payload.put("valorTotal", 0.01);
        long id = criar(payload);
        var pedido = pedidos.findById(id).orElseThrow();
        assertThat(pedido.getValorFrete()).isEqualByComparingTo("0");
        assertThat(pedido.getValorTotal()).isEqualByComparingTo(pedido.getItens().getFirst().getProduto().getPreco().multiply(BigDecimal.valueOf(2)));
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.NOVO);
    }
    @Test void entregaSomaFreteCadastrado() throws Exception {
        fretes.save(RegiaoFrete.builder().bairro("Bairro teste").valorFrete(new BigDecimal("7.50")).ativo(true).build());
        var payload = produtoPayload(); payload.put("tipoEntrega", "ENTREGA");
        payload.put("endereco", Map.of("bairro", "Bairro teste", "rua", "Rua fictícia", "numero", "1"));
        var pedido = pedidos.findById(criar(payload)).orElseThrow();
        assertThat(pedido.getValorFrete()).isEqualByComparingTo("7.50");
        assertThat(pedido.getValorTotal()).isEqualByComparingTo(pedido.getItens().getFirst().getPrecoTotal().add(new BigDecimal("7.50")));
    }
    @Test void entregaSemEnderecoNaoPersistePedido() throws Exception {
        long antes = pedidos.count(); var payload = produtoPayload(); payload.put("tipoEntrega", "ENTREGA");
        mvc.perform(post("/api/pedidos").contentType("application/json").content(json.writeValueAsString(payload)))
            .andExpect(status().isUnprocessableEntity());
        assertThat(pedidos.count()).isEqualTo(antes);
    }
    @Test void produtoIndisponivelNaoPodeSerVendido() throws Exception {
        var produto = produtos.findAll().getFirst(); produto.setDisponivel(false); produtos.saveAndFlush(produto);
        var payload = produtoPayload();
        payload.put("itens", List.of(Map.of("tipo", "PRODUTO", "produtoId", produto.getId(), "quantidade", 1)));
        mvc.perform(post("/api/pedidos").contentType("application/json").content(json.writeValueAsString(payload)))
            .andExpect(status().isUnprocessableEntity());
    }
    @Test void quantidadeZeroEhRejeitadaPelaApi() throws Exception {
        var payload = produtoPayload(); payload.put("itens", List.of(Map.of("tipo", "PRODUTO", "produtoId", 1, "quantidade", 0)));
        mvc.perform(post("/api/pedidos").contentType("application/json").content(json.writeValueAsString(payload)))
            .andExpect(status().isBadRequest());
    }
    @Test void observacaoDe500CaracteresPersisteNoPostgres() throws Exception {
        var payload = produtoPayload();
        payload.put("itens", List.of(Map.of("tipo", "PRODUTO", "produtoId", produtos.findAll().getFirst().getId(), "quantidade", 1, "observacao", "x".repeat(500))));
        long id = criar(payload); pedidos.flush();
        assertThat(pedidos.findById(id).orElseThrow().getItens().getFirst().getObservacao()).hasSize(500);
    }
    @Test void pizzaRespeitaLimiteDeSabores() throws Exception {
        var tamanho = tamanhos.findAll().stream().filter(t -> t.getMaxSabores() == 1).findFirst().orElseThrow();
        var payload = produtoPayload();
        payload.put("itens", List.of(Map.of("tipo", "PIZZA", "tamanhoId", tamanho.getId(), "quantidade", 1,
            "saboresIds", sabores.findAll().stream().limit(2).map(Sabor::getId).toList())));
        mvc.perform(post("/api/pedidos").contentType("application/json").content(json.writeValueAsString(payload)))
            .andExpect(status().isUnprocessableEntity());
    }
    @Test void tamanhoDesativadoNaoPodeSerVendido() throws Exception {
        var tamanho = tamanhos.findAll().getFirst(); tamanho.setAtivo(false); tamanhos.saveAndFlush(tamanho);
        var payload = produtoPayload(); payload.put("itens", List.of(Map.of("tipo", "PIZZA", "tamanhoId", tamanho.getId(), "quantidade", 1, "saboresIds", List.of(sabores.findAll().getFirst().getId()))));
        mvc.perform(post("/api/pedidos").contentType("application/json").content(json.writeValueAsString(payload)))
            .andExpect(status().isUnprocessableEntity());
    }
    @Test void fluxoCompletoAteEntrega() throws Exception {
        long id = criar(produtoPayload());
        for (var status : List.of(StatusPedido.CONFIRMADO, StatusPedido.EM_PREPARO, StatusPedido.PRONTO, StatusPedido.SAIU_PARA_ENTREGA, StatusPedido.ENTREGUE)) transicao(id, status, 200);
        assertThat(pedidos.findById(id).orElseThrow().getStatus()).isEqualTo(StatusPedido.ENTREGUE);
    }
    @Test void naoPermitePularEtapas() throws Exception { transicao(criar(produtoPayload()), StatusPedido.ENTREGUE, 422); }
    @ParameterizedTest @EnumSource(value = StatusPedido.class, names = {"AGUARDANDO_PAGAMENTO", "NOVO", "CONFIRMADO", "EM_PREPARO"})
    void cancelaAntesDaExpedicao(StatusPedido inicial) throws Exception {
        long id = criar(produtoPayload()); var pedido = pedidos.findById(id).orElseThrow(); pedido.setStatus(inicial); pedidos.saveAndFlush(pedido);
        transicao(id, StatusPedido.CANCELADO, 200);
    }
    @ParameterizedTest @EnumSource(value = StatusPedido.class, names = {"PRONTO", "SAIU_PARA_ENTREGA", "ENTREGUE", "CANCELADO"})
    void naoCancelaDepoisDoPreparo(StatusPedido inicial) throws Exception {
        long id = criar(produtoPayload()); var pedido = pedidos.findById(id).orElseThrow(); pedido.setStatus(inicial); pedidos.saveAndFlush(pedido);
        transicao(id, StatusPedido.CANCELADO, 422);
    }
    @Test void pixExigeConfirmacaoDedicada() throws Exception {
        var payload = produtoPayload(); payload.put("formaPagamento", "PIX"); long id = criar(payload);
        assertThat(pedidos.findById(id).orElseThrow().getStatus()).isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);
        transicao(id, StatusPedido.NOVO, 422);
    }
    @Test void confirmacaoPixLiberaPedidoMasNaoPodeSerRepetida() throws Exception {
        var payload = produtoPayload(); payload.put("formaPagamento", "PIX"); long id = criar(payload);
        mvc.perform(patch("/api/pedidos/{id}/confirmar-pagamento", id).with(httpBasic("integration-admin", "integration-password")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("NOVO"));
        mvc.perform(patch("/api/pedidos/{id}/confirmar-pagamento", id).with(httpBasic("integration-admin", "integration-password")))
            .andExpect(status().isUnprocessableEntity());
    }
    @Test void consultasAdministrativasExigemAutenticacao() throws Exception {
        mvc.perform(get("/api/pedidos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/faturamento").param("data", "2026-10-15")).andExpect(status().isUnauthorized());
    }
    @Test void senhaIncorretaNaoPermiteAlterarPedido() throws Exception {
        mvc.perform(patch("/api/pedidos/1/status").with(httpBasic("integration-admin", "senha-incorreta"))
            .contentType("application/json").content("{\"status\":\"CONFIRMADO\"}"))
            .andExpect(status().isUnauthorized());
    }
    @Test void filtroDeStatusInvalidoRetorna400() throws Exception {
        mvc.perform(get("/api/pedidos").param("status", "INEXISTENTE").with(httpBasic("integration-admin", "integration-password")))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void falhaNaCriacaoDesfazTambemCadastroDoCliente() throws Exception {
        var payload = produtoPayload(); payload.put("telefoneWhatsapp", "5500000099999"); payload.put("tipoEntrega", "ENTREGA");
        mvc.perform(post("/api/pedidos").contentType("application/json").content(json.writeValueAsString(payload)))
            .andExpect(status().isUnprocessableEntity());
        assertThat(clientes.findByTelefoneWhatsapp("5500000099999")).isEmpty();
    }
    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void versaoImpedeSobrescreverAtualizacaoDeOutroOperador() throws Exception {
        long id = criar(produtoPayload());
        try {
            var snapshot = pedidos.findById(id).orElseThrow();
            transicao(id, StatusPedido.CONFIRMADO, 200);
            snapshot.setStatus(StatusPedido.CANCELADO);
            assertThatThrownBy(() -> pedidos.saveAndFlush(snapshot))
                .isInstanceOf(org.springframework.orm.ObjectOptimisticLockingFailureException.class);
            assertThat(pedidos.findById(id).orElseThrow().getStatus()).isEqualTo(StatusPedido.CONFIRMADO);
        } finally { pedidos.deleteById(id); }
    }
}
