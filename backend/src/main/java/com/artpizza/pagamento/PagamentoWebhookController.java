package com.artpizza.pagamento;

import com.artpizza.service.PedidoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Endpoint genérico pra plugar um gateway de pagamento de verdade (Mercado
 * Pago, Pagar.me etc.) mais pra frente: quando o provedor confirmar que um
 * Pix caiu, ele chamaria essa rota, e o pedido correspondente (pelo txid)
 * seria liberado automaticamente pra cozinha — sem precisar do operador
 * confirmar manualmente no painel.
 *
 * Hoje não há gateway configurado, então nada chama essa rota de verdade;
 * ela existe como o lugar certo pra adaptar o payload específico do
 * provedor escolhido quando isso for integrado.
 */
@RestController
@RequestMapping("/webhook/pagamento")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "pagamento.webhook.enabled", havingValue = "true")
public class PagamentoWebhookController {

    private final PedidoService pedidoService;
    private final PagamentoWebhookProperties properties;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Void> receber(
            @RequestHeader(value = "X-Payment-Signature", required = false) String assinatura,
            @RequestBody String corpo) {
        if (!assinaturaValida(corpo, assinatura)) {
            log.warn("Webhook de pagamento rejeitado: assinatura inválida ou segredo ausente");
            return ResponseEntity.status(403).build();
        }
        JsonNode payload;
        try {
            payload = objectMapper.readTree(corpo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
        String txid = payload.path("txid").asText(null);
        String status = payload.path("status").asText(null);

        if (txid == null || !"PAGO".equalsIgnoreCase(status)) {
            log.info("Webhook de pagamento ignorado (txid/status ausente ou não pago): {}", payload);
            return ResponseEntity.ok().build();
        }

        try {
            pedidoService.confirmarPagamentoPorTxid(txid);
        } catch (Exception e) {
            log.error("Falha ao confirmar pagamento pelo webhook para txid {}: {}", txid, e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    private boolean assinaturaValida(String corpo, String assinatura) {
        if (properties.getSecret() == null || properties.getSecret().isBlank() || assinatura == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] esperada = mac.doFinal(corpo.getBytes(StandardCharsets.UTF_8));
            byte[] recebida = hexParaBytes(assinatura);
            return MessageDigest.isEqual(esperada, recebida);
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] hexParaBytes(String valor) {
        if (!valor.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("Assinatura inválida");
        }
        byte[] bytes = new byte[valor.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(valor.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }
}
