package com.artpizza.repository;

import com.artpizza.model.RegiaoFrete;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegiaoFreteRepository extends JpaRepository<RegiaoFrete, Long> {
    Optional<RegiaoFrete> findByBairroIgnoreCaseAndAtivoTrue(String bairro);
}
