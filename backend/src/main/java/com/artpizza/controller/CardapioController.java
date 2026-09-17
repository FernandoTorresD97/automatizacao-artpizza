package com.artpizza.controller;

import com.artpizza.model.Produto;
import com.artpizza.model.Sabor;
import com.artpizza.model.Tamanho;
import com.artpizza.service.CardapioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cardapio")
@RequiredArgsConstructor
public class CardapioController {

    private final CardapioService cardapioService;

    @GetMapping("/tamanhos")
    public List<Tamanho> listarTamanhos() {
        return cardapioService.listarTamanhos();
    }

    @GetMapping("/sabores")
    public List<Sabor> listarSabores() {
        return cardapioService.listarSabores();
    }

    @GetMapping("/produtos")
    public List<Produto> listarProdutos() {
        return cardapioService.listarProdutos();
    }
}
