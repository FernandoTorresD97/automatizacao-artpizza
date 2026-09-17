package com.artpizza.whatsapp;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "whatsapp")
@Getter
@Setter
public class WhatsAppProperties {

    // Token que você escolhe e cadastra no painel da Meta ao configurar o
    // webhook (é só uma senha combinada, não vem da Meta).
    private String verifyToken = "art-pizza-verify-token";

    // Segredo do app Meta usado para validar X-Hub-Signature-256 no POST.
    private String appSecret;

    // Token de acesso gerado no Meta for Developers (System User Access Token
    // de longa duração, em produção).
    private String accessToken;

    // ID do número de telefone comercial, também do Meta for Developers.
    private String phoneNumberId;

    private String apiVersion = "v20.0";

    // Enquanto false (padrão em dev), o WhatsAppClient só loga a mensagem em
    // vez de chamar a Graph API de verdade — assim dá pra testar o fluxo
    // inteiro pelo simulador sem precisar de conta aprovada na Meta.
    private boolean enviarMensagensReais = false;
}
