package com.cursovalido.backend.authentication.repository;

import com.cursovalido.backend.authentication.entity.SuspensaoUsuario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuspensaoUsuarioRepository extends JpaRepository<SuspensaoUsuario, Long> {
    List<SuspensaoUsuario> findByIdUsuarioAndRevogadaEmIsNullOrderByIdDesc(Long idUsuario);
    List<SuspensaoUsuario> findByIdUsuarioInAndRevogadaEmIsNullOrderByIdDesc(java.util.Collection<Long> ids);
}
