package com.artpizza;
import com.artpizza.notification.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
class NotificationHttpClientTest {
    @Test void transportUsesStableKeyAndDedicatedCredentials() throws Exception {
        var receivedKey=new AtomicReference<String>();var auth=new AtomicReference<String>();var body=new AtomicReference<String>();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/v1/notifications",exchange->{
            receivedKey.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));exchange.sendResponseHeaders(201,-1);exchange.close();
        });server.start();
        try {
            var client=new NotificationHttpClient("http://127.0.0.1:"+server.getAddress().getPort(),"artpizza","test-password-123456",1);
            var event=new NotificationEvent();event.setId("stable-id");event.setTitle("Pedido");event.setMessage("Confirmado");event.setRecipient("customer@example.invalid");
            client.send(event);client.send(event);
            assertThat(receivedKey.get()).isEqualTo("artpizza-stable-id");
            assertThat(auth.get()).isEqualTo("Basic "+java.util.Base64.getEncoder().encodeToString("artpizza:test-password-123456".getBytes(StandardCharsets.UTF_8)));
            assertThat(body.get()).contains("customer@example.invalid","channelId");
        }finally{server.stop(0);}
    }
    @Test void remotePlainHttpIsRejected(){assertThatThrownBy(()->new NotificationHttpClient("http://example.com","u","p",1)).isInstanceOf(IllegalArgumentException.class);}
    @Test void failuresAreNotReportedAsDelivered() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/v1/notifications",e->{e.sendResponseHeaders(503,-1);e.close();});server.start();
        try {
            var client=new NotificationHttpClient("http://127.0.0.1:"+server.getAddress().getPort(),"u","p",1);
            var event=new NotificationEvent();event.setId("failure");event.setTitle("Test");event.setMessage("Test");event.setRecipient("customer@example.invalid");
            assertThatThrownBy(()->client.send(event)).isInstanceOf(org.springframework.web.client.RestClientResponseException.class);
        }finally{server.stop(0);}
    }
}
