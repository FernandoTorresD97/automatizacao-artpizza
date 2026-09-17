package com.artpizza.repository;

import com.artpizza.model.SessaoConversa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface SessaoConversaRepository extends JpaRepository<SessaoConversa, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SessaoConversa> findByTelefoneWhatsapp(String telefoneWhatsapp);
}
