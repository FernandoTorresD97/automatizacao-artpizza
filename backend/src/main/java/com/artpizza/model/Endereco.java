package com.artpizza.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "endereco")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false)
    private String bairro;

    @Column(nullable = false)
    private String rua;

    @Column(nullable = false)
    private String numero;

    private String complemento;

    // Preenchidos futuramente quando a Fase 3 integrar uma API de geocoding.
    // Ficam nulos enquanto o frete for calculado por bairro (RegiaoFrete).
    private Double latitude;
    private Double longitude;
}
