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
    @Query(value = """
            INSERT INTO tb_forum_users (id, name, email, role, active)
            VALUES (:id, :nome, :email, :perfil, :ativo)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                email = EXCLUDED.email,
                role = EXCLUDED.role,
                active = EXCLUDED.active
            """, nativeQuery = true)
    void sincronizar(Long id, String nome, String email, String perfil, boolean ativo);
}