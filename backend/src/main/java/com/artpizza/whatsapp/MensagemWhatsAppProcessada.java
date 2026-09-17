package com.artpizza.whatsapp;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensagem_whatsapp_processada")
@Getter
@NoArgsConstructor
public class MensagemWhatsAppProcessada {
    @Id
    private String id;

    private LocalDateTime processadaEm;

    public MensagemWhatsAppProcessada(String id) {
        this.id = id;
        this.processadaEm = LocalDateTime.now();
    }
}
