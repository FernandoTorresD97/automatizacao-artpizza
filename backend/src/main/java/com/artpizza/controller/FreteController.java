package com.artpizza.controller;

import com.artpizza.service.FreteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/frete")
@RequiredArgsConstructor
public class FreteController {

    private final FreteService freteService;

    @GetMapping
    public Map<String, Object> calcular(@RequestParam String bairro) {
        BigDecimal valor = freteService.calcularFrete(bairro);
        return Map.of("bairro", bairro, "valorFrete", valor);
    }
}
