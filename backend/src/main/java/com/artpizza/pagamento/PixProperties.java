package com.artpizza.pagamento;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "pix")
@Getter
@Setter
public class PixProperties {

    // Chave Pix da pizzaria (CPF/CNPJ, e-mail, telefone ou chave aleatória).
    // Sem isso configurado, o "copia e cola" gerado não aponta pra conta
    // nenhuma de verdade — é só pra desenvolvimento/demonstração.
    private String chave = "00000000000";

    // Nome do recebedor como aparece no app do banco de quem paga (máx. 25 caracteres).
    private String nomeRecebedor = "ART PIZZA";

    // Cidade do recebedor (máx. 15 caracteres).
    private String cidade = "CRUZ DAS ALMAS";
}
