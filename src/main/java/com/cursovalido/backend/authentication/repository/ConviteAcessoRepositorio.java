package com.cursovalido.backend.authentication.repository;

import com.cursovalido.backend.authentication.entity.ConviteAcesso;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConviteAcessoRepositorio extends JpaRepository<ConviteAcesso, Long> {
    Optional<ConviteAcesso> findByTokenHash(String tokenHash);

    Optional<ConviteAcesso> findByEmail(String email);

    List<ConviteAcesso> findByStatusOrderByCriadoEmAsc(String status);
}
