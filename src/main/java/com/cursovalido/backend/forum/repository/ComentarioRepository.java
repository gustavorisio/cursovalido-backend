package com.cursovalido.backend.forum.repository;

import com.cursovalido.backend.forum.entity.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
    @Override
    @EntityGraph(attributePaths = "usuarioAutor")
    Optional<Comentario> findById(Long id);

    boolean existsByIdAutorAndCriadoEmAfter(Long idAutor, java.time.LocalDateTime desde);

    void deleteByIdAutor(Long idAutor);

    void deleteByTopicoId(Long idTopico);

    @Query(value = "SELECT c FROM Comentario c JOIN FETCH c.usuarioAutor "
            + "WHERE c.topico.id = :idTopico AND c.topico.ativo = true ORDER BY c.criadoEm ASC, c.id ASC",
            countQuery = "SELECT COUNT(c) FROM Comentario c WHERE c.topico.id = :idTopico AND c.topico.ativo = true")
    org.springframework.data.domain.Page<Comentario> listarPorTopicoAtivo(Long idTopico,
            org.springframework.data.domain.Pageable pagina);

    default List<Comentario> listarPorTopicoAtivo(Long idTopico) {
        return listarPorTopicoAtivo(idTopico, org.springframework.data.domain.PageRequest.of(0, 20)).getContent();
    }

    @Query("SELECT COUNT(c) FROM Comentario c WHERE c.topico.id = :idTopico AND c.ativo = true")
    long contarPorTopico(Long idTopico);

}
