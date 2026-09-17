package com.artpizza.repository;

import com.artpizza.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByTelefoneWhatsapp(String telefoneWhatsapp);
}
