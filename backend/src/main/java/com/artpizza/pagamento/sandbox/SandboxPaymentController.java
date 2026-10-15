package com.artpizza.pagamento.sandbox;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.*;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
@RestController @RequiredArgsConstructor
@ConditionalOnProperty(name="pagamento.sandbox.enabled",havingValue="true")
public class SandboxPaymentController {
    private final SandboxPaymentService payments;
    private final SandboxSignature signature;
    private final ObjectMapper json;
    private final Validator validator;
    public record Create(@NotNull @Positive Long pedidoId) {}
    @PostMapping("/api/pagamentos/sandbox")
    public SandboxPayment create(@Valid @RequestBody Create input){return payments.create(input.pedidoId());}
    @PostMapping("/api/pagamentos/sandbox/simular")
    public SandboxPayment simulate(@Valid @RequestBody SandboxPaymentService.Event event){return payments.process(event);}
    @PostMapping("/webhook/pagamento-sandbox")
    public SandboxPayment webhook(@RequestHeader(value="X-Sandbox-Timestamp",required=false) String timestamp,
        @RequestHeader(value="X-Sandbox-Signature",required=false) String sig, @RequestBody String body) {
        if(body.length()>8192 || !signature.valid(body,timestamp,sig))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        try {
            var event=json.readValue(body,SandboxPaymentService.Event.class);
            if(!validator.validate(event).isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            return payments.process(event);
        }catch(com.fasterxml.jackson.core.JsonProcessingException ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST);}
    }
}
