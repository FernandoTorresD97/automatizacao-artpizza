package com.artpizza.pagamento;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "pagamento.webhook")
@Getter
@Setter
public class PagamentoWebhookProperties {
    private boolean enabled;
    private String secret;
}
