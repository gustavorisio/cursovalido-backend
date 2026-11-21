package com.cursovalido.backend.forum.repository;

import com.cursovalido.backend.forum.entity.UsuarioForum;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public interface UsuarioForumRepository extends JpaRepository<UsuarioForum, Long> {
    List<UsuarioForum> findByAtivoTrueOrderByIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UsuarioForum u where u.id = :id")
    java.util.Optional<UsuarioForum> bloquearPorId(@Param("id") Long id);

    @Transactional
    default void sincronizar(Long id, String nome, String email, String perfil, boolean ativo) {
        UsuarioForum usuario = findById(id).orElseGet(() -> new UsuarioForum(id, nome, email, perfil));
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo);
        saveAndFlush(usuario);
    }
}
