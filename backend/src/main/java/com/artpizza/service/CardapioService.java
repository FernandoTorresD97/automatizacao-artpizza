package com.artpizza.service;

import com.artpizza.model.Produto;
import com.artpizza.model.Sabor;
import com.artpizza.model.Tamanho;
import com.artpizza.repository.ProdutoRepository;
import com.artpizza.repository.SaborRepository;
import com.artpizza.repository.TamanhoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardapioService {

    private final TamanhoRepository tamanhoRepository;
    private final SaborRepository saborRepository;
    private final ProdutoRepository produtoRepository;

    public List<Tamanho> listarTamanhos() {
        return tamanhoRepository.findByAtivoTrue();
    }

    public List<Sabor> listarSabores() {
        return saborRepository.findByDisponivelTrue();
    }

    public List<Produto> listarProdutos() {
        return produtoRepository.findByDisponivelTrue();
    }
}
