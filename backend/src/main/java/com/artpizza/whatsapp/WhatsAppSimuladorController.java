package com.artpizza.whatsapp;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Roda exatamente a mesma lógica de conversa do webhook real (ConversaService),
 * mas devolve a resposta direto no corpo HTTP em vez de mandar pela Graph API.
 * Serve pra testar o fluxo inteiro do bot (ou pra demonstrar no portfólio)
 * antes de ter uma conta comercial aprovada na Meta.
 *
 * Exemplo: enviar várias mensagens em sequência com o mesmo "telefone"
 * simula uma conversa contínua, porque a sessão fica salva por telefone.
 */
@RestController
@RequestMapping("/api/whatsapp-simulador")
@RequiredArgsConstructor
public class WhatsAppSimuladorController {

    private final ConversaService conversaService;

    @PostMapping(value = "/mensagem", produces = "application/json; charset=UTF-8")
    public Map<String, String> enviarMensagem(@Valid @RequestBody MensagemSimuladaDTO dto) {
        String resposta = conversaService.processarMensagem(
                dto.getTelefone(),
                dto.getNome(),
                dto.getTexto()
        );

        return Map.of("resposta", resposta);
    }
}
