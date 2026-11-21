package com.cursovalido.backend.authentication.repository;

import com.cursovalido.backend.authentication.entity.ConviteAcesso;
import com.cursovalido.backend.authentication.entity.StatusConvite;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConviteAcessoRepository extends JpaRepository<ConviteAcesso, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ConviteAcesso> findByTokenHash(String tokenHash);

    Optional<ConviteAcesso> findFirstByEmailOrderByIdDesc(String email);

    default Optional<ConviteAcesso> findByEmail(String email) {
        return findFirstByEmailOrderByIdDesc(email);
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConviteAcesso c where c.id = :id")
    Optional<ConviteAcesso> bloquearPorId(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ConviteAcesso> findFirstByEmailAndStatusOrderByIdDesc(String email, StatusConvite status);

    long countByCriadoPorAndCriadoEmAfter(Long criadoPor, LocalDateTime criadoEm);

    void deleteByEmail(String email);

    List<ConviteAcesso> findByStatusOrderByCriadoEmAsc(StatusConvite status);
}
