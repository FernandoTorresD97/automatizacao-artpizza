package com.artpizza.repository;

import com.artpizza.model.Tamanho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TamanhoRepository extends JpaRepository<Tamanho, Long> {
    List<Tamanho> findByAtivoTrue();
}
