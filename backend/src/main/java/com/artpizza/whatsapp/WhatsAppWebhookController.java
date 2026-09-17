package com.artpizza.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

@RestController
@RequestMapping("/webhook/whatsapp")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController {

    private final WhatsAppProperties propriedades;
    private final WhatsAppClient whatsAppClient;
    private final ConversaService conversaService;
    private final ObjectMapper objectMapper;

    // A Meta chama esse GET uma vez, ao salvar a configuração do webhook no
    // painel de desenvolvedores, só para confirmar que o dono do endpoint é
    // você (respondendo o challenge de volta).
    @GetMapping
    public ResponseEntity<String> verificar(
            @RequestParam("hub.mode") String modo,
            @RequestParam("hub.verify_token") String tokenRecebido,
            @RequestParam("hub.challenge") String challenge) {

        if ("subscribe".equals(modo) && propriedades.getVerifyToken() != null
                && !propriedades.getVerifyToken().isBlank()
                && propriedades.getVerifyToken().equals(tokenRecebido)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(403).build();
    }

    // A Meta chama esse POST a cada mensagem recebida pelo número comercial.
    @PostMapping
    public ResponseEntity<Void> receber(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String assinatura,
            @RequestBody String corpo) {
        if (!assinaturaValida(corpo, assinatura)) {
            log.warn("Webhook do WhatsApp rejeitado: assinatura inválida ou segredo ausente");
            return ResponseEntity.status(403).build();
        }
        try {
            JsonNode payload = objectMapper.readTree(corpo);
            JsonNode mudancas = payload.path("entry").path(0).path("changes").path(0).path("value");
            JsonNode mensagens = mudancas.path("messages");
            if (!mensagens.isArray() || mensagens.isEmpty()) {
                // Também chegam eventos de status (entregue/lido) aqui — ignoramos.
                return ResponseEntity.ok().build();
            }

            String nomePerfil = mudancas.path("contacts").path(0).path("profile").path("name").asText(null);
            for (JsonNode mensagem : mensagens) {
                String telefone = mensagem.path("from").asText();
                String texto = mensagem.path("text").path("body").asText("");
                String idMensagem = mensagem.path("id").asText(null);
                Optional<String> resposta = conversaService.processarMensagemWhatsApp(
                        idMensagem, telefone, nomePerfil, texto);
                resposta.ifPresent(valor -> whatsAppClient.enviarMensagemTexto(telefone, valor));
            }
        } catch (Exception e) {
            log.error("Erro ao processar mensagem recebida do WhatsApp", e);
        }
        // A Meta espera 200 rapidamente; erros de negócio já viram mensagem
        // de resposta pro cliente, então nunca retornamos erro aqui.
        return ResponseEntity.ok().build();
    }

    private boolean assinaturaValida(String corpo, String assinatura) {
        if (propriedades.getAppSecret() == null || propriedades.getAppSecret().isBlank()
                || assinatura == null || !assinatura.startsWith("sha256=")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(propriedades.getAppSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String esperada = "sha256=" + bytesParaHex(mac.doFinal(corpo.getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(esperada.getBytes(StandardCharsets.US_ASCII),
                    assinatura.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception e) {
            log.error("Não foi possível validar a assinatura do webhook do WhatsApp", e);
            return false;
        }
    }

    private String bytesParaHex(byte[] bytes) {
        StringBuilder resultado = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            resultado.append(String.format("%02x", b));
        }
        return resultado.toString();
    }
}
