package com.cursovalido.backend.forum.repository;

import com.cursovalido.backend.forum.entity.UsuarioForum;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UsuarioForumRepositorio extends JpaRepository<UsuarioForum, Long> {
    List<UsuarioForum> findByAtivoTrueOrderByIdAsc();

    @Transactional
    default void sincronizar(Long id, String nome, String email, String perfil, boolean ativo) {
        UsuarioForum usuario = new UsuarioForum(id, nome, email, perfil);
        usuario.setAtivo(ativo);
        saveAndFlush(usuario);
    }
}
