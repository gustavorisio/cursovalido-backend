package com.cursovalido.backend.forum.repository;

import com.cursovalido.backend.forum.entity.UsuarioForum;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UsuarioForumRepositorio extends JpaRepository<UsuarioForum, Long> {
    List<UsuarioForum> findByAtivoTrueOrderByIdAsc();

    @Modifying
    @Transactional
    @Query("""
            insert into UsuarioForum (id, nome, email, perfil, ativo)
            values (:id, :nome, :email, :perfil, :ativo)
            on conflict (id) do update set
                nome = excluded.nome,
                email = excluded.email,
                perfil = excluded.perfil,
                ativo = excluded.ativo
            """)
    void sincronizar(Long id, String nome, String email, String perfil, boolean ativo);
}
