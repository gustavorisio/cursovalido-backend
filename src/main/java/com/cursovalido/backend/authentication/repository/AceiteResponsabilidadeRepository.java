package com.cursovalido.backend.authentication.repository;

import com.cursovalido.backend.authentication.entity.AceiteResponsabilidade;
import com.cursovalido.backend.authentication.entity.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AceiteResponsabilidadeRepository extends JpaRepository<AceiteResponsabilidade, Long> {
    boolean existsByUsuarioIdAndPerfilAndVersao(Long usuarioId, Perfil perfil, String versao);
}
