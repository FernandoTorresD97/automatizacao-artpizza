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

@SpringBootTest(properties={"pagamento.sandbox.enabled=true","pagamento.sandbox.secret=sandbox-test-signing-secret-at-least-32"})
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
    @Autowired com.artpizza.notification.NotificationEventRepository outbox;
    @Autowired com.artpizza.notification.NotificationOutboxWorker outboxWorker;
    @Autowired com.artpizza.service.PedidoService pedidoService;
    @Autowired com.artpizza.pagamento.sandbox.SandboxPaymentService sandbox;
    @Autowired com.artpizza.pagamento.sandbox.SandboxPaymentRepository payments;
    @Autowired com.artpizza.pagamento.sandbox.SandboxPaymentEventRepository paymentEvents;
    @org.springframework.boot.test.mock.mockito.MockBean com.artpizza.notification.NotificationClient notificationClient;

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
    com.artpizza.pagamento.sandbox.SandboxPayment charge() throws Exception {
        var payload=produtoPayload();payload.put("formaPagamento","PIX");payload.put("emailNotificacao","customer@example.invalid");
        return sandbox.create(criar(payload));
    }
    com.artpizza.pagamento.sandbox.SandboxPaymentService.Event event(com.artpizza.pagamento.sandbox.SandboxPayment p,String id,String status) {
        return new com.artpizza.pagamento.sandbox.SandboxPaymentService.Event(id,p.getId(),p.getPedidoId(),p.getAmount(),"BRL",status);
    }
    @Test void emailOpcionalGeraEventoNoMesmoPedido() throws Exception {
        var payload=produtoPayload();payload.put("emailNotificacao","customer@example.invalid");long id=criar(payload);
        assertThat(outbox.findAll()).filteredOn(e->e.getPedidoId().equals(id)).singleElement().extracting(e->e.getEventType()).isEqualTo("CRIADO");
    }
    @Test void semEmailNaoGeraEvento() throws Exception {long antes=outbox.count();criar(produtoPayload());assertThat(outbox.count()).isEqualTo(antes);}
    @Test void emailInvalidoEhRejeitado() throws Exception {
        var payload=produtoPayload();payload.put("emailNotificacao","invalido");
        mvc.perform(post("/api/pedidos").contentType("application/json").content(json.writeValueAsString(payload))).andExpect(status().isBadRequest());
    }
    @Test void indisponibilidadeNotificationPreservaEventoParaRetentativa() throws Exception {
        var p=charge();var e=outbox.findAll().stream().filter(n->n.getPedidoId().equals(p.getPedidoId())).findFirst().orElseThrow();
        org.mockito.Mockito.doThrow(new IllegalStateException("unavailable")).when(notificationClient).send(org.mockito.ArgumentMatchers.any());
        outboxWorker.deliver(e.getId());
        assertThat(e.getDeliveredAt()).isNull();assertThat(e.getAttempts()).isEqualTo(1);assertThat(e.getNextAttemptAt()).isAfter(java.time.LocalDateTime.now());
        org.mockito.Mockito.reset(notificationClient);e.setNextAttemptAt(java.time.LocalDateTime.now().minusSeconds(1));outbox.saveAndFlush(e);
        outboxWorker.deliver(e.getId());outboxWorker.deliver(e.getId());
        assertThat(e.getDeliveredAt()).isNotNull();org.mockito.Mockito.verify(notificationClient).send(e);
    }
    @Test void sandboxUsaValorDoServidorERetornaMesmaCobranca() throws Exception {
        var p=charge();assertThat(p.getAmount()).isEqualByComparingTo(pedidos.findById(p.getPedidoId()).orElseThrow().getValorTotal());
        assertThat(sandbox.create(p.getPedidoId()).getId()).isEqualTo(p.getId());
    }
    @Test void aprovacaoDuplicadaNaoDuplicaConfirmacaoNemOutbox() throws Exception {
        var p=charge();var e=event(p,"event-approved","APPROVED");sandbox.process(e);sandbox.process(e);sandbox.process(event(p,"another-event","APPROVED"));
        assertThat(pedidos.findById(p.getPedidoId()).orElseThrow().getStatus()).isEqualTo(StatusPedido.NOVO);
        assertThat(outbox.findAll()).filteredOn(n->n.getPedidoId().equals(p.getPedidoId()) && n.getEventType().equals("PAGAMENTO_CONFIRMADO")).hasSize(1);
        assertThat(paymentEvents.findAll()).filteredOn(n->n.getPaymentId().equals(p.getId())).hasSize(2);
    }
    @Test void recusaNaoLiberaPedido() throws Exception {var p=charge();sandbox.process(event(p,"declined","DECLINED"));assertThat(pedidos.findById(p.getPedidoId()).orElseThrow().getStatus()).isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);}
    @Test void valorIncorretoNaoConfirma() throws Exception {
        var p=charge();assertThatThrownBy(()->sandbox.process(new com.artpizza.pagamento.sandbox.SandboxPaymentService.Event("wrong-value",p.getId(),p.getPedidoId(),new BigDecimal("0.01"),"BRL","APPROVED"))).isInstanceOf(com.artpizza.exception.RegraNegocioException.class);
        assertThat(paymentEvents.existsById("wrong-value")).isFalse();
    }
    @Test void moedaIncorretaNaoConfirma() throws Exception {
        var p=charge();assertThatThrownBy(()->sandbox.process(new com.artpizza.pagamento.sandbox.SandboxPaymentService.Event("wrong-currency",p.getId(),p.getPedidoId(),p.getAmount(),"USD","APPROVED"))).isInstanceOf(com.artpizza.exception.RegraNegocioException.class);
    }
    @Test void pedidoCanceladoNaoEhLiberado() throws Exception {var p=charge();pedidoService.atualizarStatus(p.getPedidoId(),StatusPedido.CANCELADO);assertThatThrownBy(()->sandbox.process(event(p,"cancelled","APPROVED"))).isInstanceOf(com.artpizza.exception.RegraNegocioException.class);}
    @Test void eventoReutilizadoComOutroConteudoEhRejeitado() throws Exception {var p=charge();sandbox.process(event(p,"same-id","DECLINED"));assertThatThrownBy(()->sandbox.process(event(p,"same-id","APPROVED"))).isInstanceOf(com.artpizza.exception.RegraNegocioException.class);}
    @Test void cobrancaRecusadaNaoPodeSerAprovadaDepois() throws Exception {var p=charge();sandbox.process(event(p,"decline","DECLINED"));assertThatThrownBy(()->sandbox.process(event(p,"approve","APPROVED"))).isInstanceOf(com.artpizza.exception.RegraNegocioException.class);}
    @Test void simulacaoExigeAdmin() throws Exception {mvc.perform(post("/api/pagamentos/sandbox").contentType("application/json").content("{\"pedidoId\":1}")).andExpect(status().isUnauthorized());}
    @Test void webhookSemAssinaturaEhRejeitado() throws Exception {mvc.perform(post("/webhook/pagamento-sandbox").contentType("application/json").content("{}")).andExpect(status().isForbidden());}
    @Test void webhookAssinadoConfirmaPagamento() throws Exception {
        var p=charge();String body=json.writeValueAsString(event(p,"signed-event","APPROVED"));String ts=Long.toString(java.time.Instant.now().getEpochSecond());
        var mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec("sandbox-test-signing-secret-at-least-32".getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA256"));
        String signature=java.util.HexFormat.of().formatHex(mac.doFinal((ts+"."+body).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        mvc.perform(post("/webhook/pagamento-sandbox").header("X-Sandbox-Timestamp",ts).header("X-Sandbox-Signature",signature).contentType("application/json").content(body)).andExpect(status().isOk());
    }
    @Test void webhookComAssinaturaAdulteradaEhRejeitado() throws Exception {
        mvc.perform(post("/webhook/pagamento-sandbox").header("X-Sandbox-Timestamp",Long.toString(java.time.Instant.now().getEpochSecond()))
            .header("X-Sandbox-Signature","0".repeat(64)).contentType("application/json").content("{}")).andExpect(status().isForbidden());
    }
    @Test void webhookAntigoEhRejeitado() throws Exception {
        mvc.perform(post("/webhook/pagamento-sandbox").header("X-Sandbox-Timestamp","1")
            .header("X-Sandbox-Signature","0".repeat(64)).contentType("application/json").content("{}")).andExpect(status().isForbidden());
    }
    @Test @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void confirmacoesConcorrentesGeramUmaUnicaNotificacao() throws Exception {
        var p=charge();var e=event(p,"concurrent-sandbox-event","APPROVED");
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var first=pool.submit(()->sandbox.process(e));var second=pool.submit(()->sandbox.process(e));
            assertThat(first.get(10,java.util.concurrent.TimeUnit.SECONDS).getStatus()).isEqualTo("APPROVED");
            assertThat(second.get(10,java.util.concurrent.TimeUnit.SECONDS).getStatus()).isEqualTo("APPROVED");
            assertThat(outbox.findAll()).filteredOn(n->n.getPedidoId().equals(p.getPedidoId()) && n.getEventType().equals("PAGAMENTO_CONFIRMADO")).hasSize(1);
            assertThat(paymentEvents.findAll()).filteredOn(n->n.getPaymentId().equals(p.getId())).hasSize(1);
        } finally {
            pool.shutdownNow();
            paymentEvents.deleteAll(paymentEvents.findAll().stream().filter(n->n.getPaymentId().equals(p.getId())).toList());
            outbox.deleteAll(outbox.findAll().stream().filter(n->n.getPedidoId().equals(p.getPedidoId())).toList());
            payments.deleteById(p.getId());pedidos.deleteById(p.getPedidoId());
        }
    }
    @Test void pedidoInvalidoComEmailNaoGeraEvento() throws Exception {
        long before=outbox.count();var payload=produtoPayload();payload.put("emailNotificacao","customer@example.invalid");payload.put("tipoEntrega","ENTREGA");
        mvc.perform(post("/api/pedidos").contentType("application/json").content(json.writeValueAsString(payload))).andExpect(status().isUnprocessableEntity());
        assertThat(outbox.count()).isEqualTo(before);
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
