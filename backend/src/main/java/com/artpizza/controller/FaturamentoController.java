package com.artpizza.controller;

import com.artpizza.dto.FaturamentoResponseDTO;
import com.artpizza.service.FaturamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/faturamento")
@RequiredArgsConstructor
public class FaturamentoController {
    private final FaturamentoService faturamentoService;

    @GetMapping
    public FaturamentoResponseDTO resumir(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return faturamentoService.resumir(data != null ? data : LocalDate.now());
    }
}
