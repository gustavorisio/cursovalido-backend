package com.cursovalido.backend.authentication.repository;

import com.cursovalido.backend.authentication.entity.TokenEmail;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenEmailRepository extends JpaRepository<TokenEmail, Long> {
    boolean existsByTokenHash(String tokenHash);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TokenEmail> findByTokenHashAndFinalidade(String tokenHash, String finalidade);

    boolean existsByUsuarioIdAndFinalidadeAndUtilizadoFalseAndExpiraEmAfter(
            Long usuarioId, String finalidade, LocalDateTime agora);

    long countByUsuarioIdAndFinalidadeAndCriadoEmAfter(Long usuarioId, String finalidade, LocalDateTime criadoEm);

    @Modifying
    @Query("update TokenEmail token set token.utilizado = true "
            + "where token.usuario.id = :usuarioId and token.finalidade = :finalidade "
            + "and token.utilizado = false")
    void invalidarTokensAtivos(@Param("usuarioId") Long usuarioId, @Param("finalidade") String finalidade);

    void deleteByUsuarioId(Long usuarioId);
}
