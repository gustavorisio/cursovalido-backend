package com.cursovalido.backend.forum.repository;

import com.cursovalido.backend.forum.entity.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ComentarioRepositorio extends JpaRepository<Comentario, Long> {
    void deleteByIdAutor(Long idAutor);

    void deleteByTopicoId(Long idTopico);

    @Query("SELECT c FROM Comentario c JOIN FETCH c.usuarioAutor "
            + "WHERE c.topico.id = :idTopico AND c.topico.ativo = true AND c.ativo = true ORDER BY c.criadoEm ASC")
    List<Comentario> listarPorTopicoAtivo(Long idTopico);

    @Query("SELECT COUNT(c) FROM Comentario c WHERE c.topico.id = :idTopico AND c.ativo = true")
    long contarPorTopico(Long idTopico);

}