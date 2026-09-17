package com.artpizza.pagamento;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

/**
 * Gera o "Pix Copia e Cola" no formato BR Code (EMV) do Banco Central —
 * o mesmo texto que vira QR code em qualquer app de banco — usando uma
 * chave Pix estática configurada em PixProperties. Não depende de nenhuma
 * conta em gateway de pagamento (Mercado Pago, Pagar.me etc.).
 *
 * Limitação importante: como não é uma cobrança via gateway, não existe
 * webhook automático de confirmação — o operador confirma manualmente
 * (ver PedidoService.confirmarPagamento) depois de ver o Pix cair na conta.
 * Pra confirmação automática, troque esta implementação por uma chamada à
 * API de um provedor de pagamento, mantendo a mesma assinatura.
 */
@Service
@RequiredArgsConstructor
public class PixService {

    private final PixProperties propriedades;

    public String gerarPayload(BigDecimal valor, String identificadorTransacao) {
        String infoConta = campo("00", "br.gov.bcb.pix") + campo("01", propriedades.getChave());
        String dadosAdicionais = campo("05", sanitizarTxid(identificadorTransacao));

        StringBuilder payloadSemCrc = new StringBuilder()
                .append(campo("00", "01"))
                .append(campo("26", infoConta))
                .append(campo("52", "0000"))
                .append(campo("53", "986"))
                .append(campo("54", formatarValor(valor)))
                .append(campo("58", "BR"))
                .append(campo("59", limitar(semAcento(propriedades.getNomeRecebedor()), 25)))
                .append(campo("60", limitar(semAcento(propriedades.getCidade()), 15)))
                .append(campo("62", dadosAdicionais))
                .append("6304");

        return payloadSemCrc + crc16(payloadSemCrc.toString());
    }

    private String campo(String id, String valor) {
        return id + String.format("%02d", valor.length()) + valor;
    }

    private String formatarValor(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String semAcento(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toUpperCase();
    }

    private String limitar(String texto, int max) {
        return texto.length() > max ? texto.substring(0, max) : texto;
    }

    private String sanitizarTxid(String txid) {
        String limpo = semAcento(txid).replaceAll("[^A-Z0-9]", "");
        limpo = limitar(limpo, 25);
        return limpo.isBlank() ? "***" : limpo;
    }

    // CRC16-CCITT (variante "FALSE"): polinômio 0x1021, valor inicial 0xFFFF,
    // sem reflexão de bits e sem XOR final — é o algoritmo exigido pelo
    // manual do BR Code para o campo 63.
    private String crc16(String payload) {
        int resultado = 0xFFFF;
        for (byte b : payload.getBytes(StandardCharsets.UTF_8)) {
            resultado ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                resultado = (resultado & 0x8000) != 0
                        ? ((resultado << 1) ^ 0x1021) & 0xFFFF
                        : (resultado << 1) & 0xFFFF;
            }
        }
        return String.format("%04X", resultado);
    }
}
