package com.artpizza.notification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.net.URI;
import java.util.Map;
@Component
public class NotificationHttpClient implements NotificationClient {
    private final RestClient client;
    private final String username,password;
    private final long channelId;
    public NotificationHttpClient(@Value("${integration.notification.url:http://localhost:8080}") String url,
        @Value("${integration.notification.username:}") String username,
        @Value("${integration.notification.password:}") String password,
        @Value("${integration.notification.channel-id:1}") long channelId) {
        URI target=URI.create(url);
        boolean local="localhost".equals(target.getHost()) || "127.0.0.1".equals(target.getHost()) || "notification".equals(target.getHost());
        if(!"https".equals(target.getScheme()) && !(local && "http".equals(target.getScheme()))) throw new IllegalArgumentException("Notification URL requires HTTPS outside the local lab");
        if(target.getUserInfo()!=null || target.getQuery()!=null || target.getFragment()!=null) throw new IllegalArgumentException("Use a base URL without credentials or query parameters");
        var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(3000);factory.setReadTimeout(5000);
        client=RestClient.builder().baseUrl(url).requestFactory(factory).build();
        this.username=username;this.password=password;this.channelId=channelId;
    }
    public void send(NotificationEvent event) {
        if(username.isBlank() || password.isBlank()) throw new IllegalStateException("Notification integration credentials missing");
        client.post().uri("/api/v1/notifications").headers(h->h.setBasicAuth(username,password))
            .header("Idempotency-Key","artpizza-"+event.getId())
            .body(Map.of("title",event.getTitle(),"message",event.getMessage(),"recipient",event.getRecipient(),"channelId",channelId))
            .retrieve().toBodilessEntity();
    }
}
