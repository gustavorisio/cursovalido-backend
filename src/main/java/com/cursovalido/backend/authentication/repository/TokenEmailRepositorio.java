package com.cursovalido.backend.authentication.repository;

import com.cursovalido.backend.authentication.entity.TokenEmail;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenEmailRepositorio extends JpaRepository<TokenEmail, Long> {
    Optional<TokenEmail> findByTokenHashAndFinalidade(String tokenHash, String finalidade);

    long countByUsuarioIdAndFinalidadeAndCriadoEmAfter(Long usuarioId, String finalidade, LocalDateTime criadoEm);

    @Modifying
    @Query("update TokenEmail token set token.utilizado = true "
            + "where token.usuario.id = :usuarioId and token.finalidade = :finalidade "
            + "and token.utilizado = false")
    void invalidarTokensAtivos(@Param("usuarioId") Long usuarioId, @Param("finalidade") String finalidade);

    void deleteByUsuarioId(Long usuarioId);
}