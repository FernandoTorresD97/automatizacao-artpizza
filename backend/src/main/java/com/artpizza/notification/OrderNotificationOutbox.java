package com.artpizza.notification;
import com.artpizza.model.Pedido;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.LocalDateTime;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class OrderNotificationOutbox {
    private final NotificationEventRepository events;
    @Transactional(propagation=Propagation.MANDATORY)
    public void record(Pedido pedido, String eventType) {
        if(pedido.getEmailNotificacao()==null || pedido.getEmailNotificacao().isBlank()) return;
        var event=new NotificationEvent();
        event.setId(UUID.randomUUID().toString()); event.setPedidoId(pedido.getId());event.setEventType(eventType);
        event.setRecipient(pedido.getEmailNotificacao());event.setTitle("Art Pizza · Pedido #"+pedido.getId());
        event.setMessage("Pedido #"+pedido.getId()+" · "+eventType+". Mensagem do ambiente de demonstração.");
        event.setCreatedAt(LocalDateTime.now());event.setNextAttemptAt(LocalDateTime.now());events.save(event);
    }
}
