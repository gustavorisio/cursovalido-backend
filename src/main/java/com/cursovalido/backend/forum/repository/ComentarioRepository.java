package com.cursovalido.backend.forum.repository;

import com.cursovalido.backend.forum.entity.Comentario;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
    @Override
    @EntityGraph(attributePaths = "usuarioAutor")
    Optional<Comentario> findById(Long id);

    boolean existsByIdAutorAndCriadoEmAfter(Long idAutor, LocalDateTime desde);

    void deleteByIdAutor(Long idAutor);

    void deleteByTopicoId(Long idTopico);

    @Query(value = "SELECT c FROM Comentario c JOIN FETCH c.usuarioAutor "
            + "WHERE c.topico.id = :idTopico AND c.topico.ativo = true ORDER BY c.criadoEm ASC, c.id ASC", countQuery = "SELECT COUNT(c) FROM Comentario c WHERE c.topico.id = :idTopico AND c.topico.ativo = true")
    Page<Comentario> listarPorTopicoAtivo(Long idTopico,
            Pageable pagina);

    default List<Comentario> listarPorTopicoAtivo(Long idTopico) {
        return listarPorTopicoAtivo(idTopico, PageRequest.of(0, 20)).getContent();
    }

    @Query("SELECT COUNT(c) FROM Comentario c WHERE c.topico.id = :idTopico AND c.ativo = true")
    long contarPorTopico(Long idTopico);

}
