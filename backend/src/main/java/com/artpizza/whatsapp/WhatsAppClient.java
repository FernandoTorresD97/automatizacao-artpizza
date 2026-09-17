package com.artpizza.whatsapp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsAppClient {

    private final RestTemplate restTemplate;
    private final WhatsAppProperties propriedades;

    public void enviarMensagemTexto(String telefoneDestino, String texto) {
        if (!propriedades.isEnviarMensagensReais()) {
            log.info("[WHATSAPP SIMULADO] para {}:\n{}", telefoneDestino, texto);
            return;
        }

        String url = "https://graph.facebook.com/%s/%s/messages"
                .formatted(propriedades.getApiVersion(), propriedades.getPhoneNumberId());

        Map<String, Object> corpo = Map.of(
                "messaging_product", "whatsapp",
                "to", telefoneDestino,
                "type", "text",
                "text", Map.of("body", texto)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(propriedades.getAccessToken());

        try {
            restTemplate.postForEntity(url, new HttpEntity<>(corpo, headers), String.class);
        } catch (Exception e) {
            log.error("Falha ao enviar mensagem via WhatsApp Cloud API para {}: {}", telefoneDestino, e.getMessage());
        }
    }
}
