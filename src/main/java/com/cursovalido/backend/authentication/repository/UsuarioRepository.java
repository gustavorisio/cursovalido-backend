package com.cursovalido.backend.authentication.repository;

import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.security.ProtecaoDadosPessoais;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsByPerfil(Perfil perfil);

    boolean existsByCpfHash(String cpfHash);

    default boolean existsByCpf(String cpf) {
        return existsByCpfHash(ProtecaoDadosPessoais.hashCpf(cpf));
    }

    boolean existsByMatricula(String matricula);

    void deleteById(Long id);
}
