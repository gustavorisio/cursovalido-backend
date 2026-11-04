package com.cursovalido.backend.authentication.repository;

import com.cursovalido.backend.authentication.entity.Sessao;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SessaoRepositorio extends JpaRepository<Sessao, String> {
    @Modifying
    @Transactional
    @Query("update Sessao s set s.revogada = true where s.revogada = false")
    void revogarTodas();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update Sessao s set s.revogada = true where s.usuarioId = :id")
    void revogarPorUsuario(@Param("id") Long id);

    void deleteByUsuarioId(Long usuarioId);
}
