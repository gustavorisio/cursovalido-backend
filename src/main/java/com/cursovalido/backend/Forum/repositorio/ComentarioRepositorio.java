package com.cursovalido.backend.Forum.repositorio;

import com.cursovalido.backend.Forum.entidade.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Collection;
import java.util.List;

public interface ComentarioRepositorio extends JpaRepository<Comentario, Long> {
    @Query("SELECT c FROM Comentario c JOIN FETCH c.usuarioAutor " + "WHERE c.topico.id = :idTopico AND c.topico.ativo = true ORDER BY c.criadoEm ASC")
    List<Comentario> listarPorTopicoAtivo(Long idTopico);

    @Query("SELECT COUNT(c) FROM Comentario c WHERE c.topico.id = :idTopico AND c.ativo = true")
    long contarPorTopico(Long idTopico);

    @Query("SELECT c.topico.id AS idTopico, COUNT(c) AS quantidade " + "FROM Comentario c WHERE c.topico.id IN :idsTopicos AND c.ativo = true GROUP BY c.topico.id")
    List<ContagemComentarios> contarPorTopicos(Collection<Long> idsTopicos);
}