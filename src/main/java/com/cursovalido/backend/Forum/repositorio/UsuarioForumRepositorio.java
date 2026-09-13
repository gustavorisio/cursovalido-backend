package com.cursovalido.backend.Forum.repositorio;

import com.cursovalido.backend.Forum.entidade.UsuarioForum;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UsuarioForumRepositorio extends JpaRepository<UsuarioForum, Long> {
    List<UsuarioForum> findByAtivoTrueOrderByIdAsc();
}