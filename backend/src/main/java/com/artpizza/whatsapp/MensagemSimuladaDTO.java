package com.artpizza.whatsapp;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MensagemSimuladaDTO {

    @NotBlank
    private String telefone;

    private String nome;

    @NotBlank
    private String texto;
}
