package com.artpizza.dto;

import com.artpizza.model.StatusPedido;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AtualizarStatusDTO {
    @NotNull
    private StatusPedido status;
}
