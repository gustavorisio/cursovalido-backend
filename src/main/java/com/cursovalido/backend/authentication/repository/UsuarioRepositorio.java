package com.cursovalido.backend.authentication.repository;

import com.cursovalido.backend.authentication.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);

    boolean existsByMatricula(String matricula);

    void deleteById(Long id);
}