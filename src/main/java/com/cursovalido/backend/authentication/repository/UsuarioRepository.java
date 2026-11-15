package com.cursovalido.backend.authentication.repository;

import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.entity.Perfil;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> buscarParaAtualizar(@org.springframework.data.repository.query.Param("id") Long id);

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPerfil(Perfil perfil);

    boolean existsByMatricula(String matricula);

    void deleteById(Long id);
}
