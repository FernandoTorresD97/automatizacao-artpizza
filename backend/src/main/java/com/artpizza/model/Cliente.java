package com.artpizza.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    // Número do WhatsApp no formato E.164, ex: 5575999999999. É o identificador
    // de conversa: cada telefone corresponde a uma sessão de pedido no bot.
    @Column(name = "telefone_whatsapp", nullable = false, unique = true)
    private String telefoneWhatsapp;

    @Builder.Default
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
