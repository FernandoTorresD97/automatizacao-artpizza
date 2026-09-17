package com.artpizza.repository;

import com.artpizza.model.Sabor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaborRepository extends JpaRepository<Sabor, Long> {
    List<Sabor> findByDisponivelTrue();
}
