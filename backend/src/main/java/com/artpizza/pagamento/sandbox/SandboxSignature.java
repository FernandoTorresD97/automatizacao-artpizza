package com.artpizza.pagamento.sandbox;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
@Component
public class SandboxSignature {
    private final String secret;
    public SandboxSignature(@Value("${pagamento.sandbox.secret:}") String secret){this.secret=secret;}
    public boolean valid(String body,String timestamp,String signature) {
        if(secret.length()<32 || timestamp==null || signature==null || !signature.matches("[a-fA-F0-9]{64}"))return false;
        try {
            long seconds=Long.parseLong(timestamp);long now=Instant.now().getEpochSecond();
            if(seconds<now-300 || seconds>now+300)return false;
            var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            return MessageDigest.isEqual(mac.doFinal((timestamp+"."+body).getBytes(StandardCharsets.UTF_8)),HexFormat.of().parseHex(signature));
        }catch(Exception ex){return false;}
    }
}
