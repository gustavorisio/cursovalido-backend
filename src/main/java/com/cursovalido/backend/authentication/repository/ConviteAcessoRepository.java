package com.cursovalido.backend.authentication.repository;

import com.cursovalido.backend.authentication.entity.ConviteAcesso;
import com.cursovalido.backend.authentication.entity.StatusConvite;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ConviteAcessoRepository extends JpaRepository<ConviteAcesso, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ConviteAcesso> findByTokenHash(String tokenHash);

    Optional<ConviteAcesso> findByEmail(String email);
    void deleteByEmail(String email);

    List<ConviteAcesso> findByStatusOrderByCriadoEmAsc(StatusConvite status);
}
