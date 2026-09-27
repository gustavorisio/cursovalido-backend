package com.cursovalido.backend.forum.repository;

import com.cursovalido.backend.forum.entity.Topico;
import com.cursovalido.backend.forum.dto.TopicoResumoDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface TopicoRepositorio extends JpaRepository<Topico, Long> {
    List<Topico> findByIdAutor(Long idAutor);

    @Query("SELECT new com.cursovalido.backend.forum.dto.TopicoResumoDTO(" +
            "t.id, t.titulo, t.descricao, u.nome, u.perfil, t.idAutor, t.criadoEm, " +
            "COUNT(c), t.ativo, t.fechado) " +
            "FROM Topico t JOIN t.usuarioAutor u LEFT JOIN Comentario c " +
            "ON c.topico = t AND c.ativo = true " +
            "WHERE t.ativo = true AND LOWER(t.titulo) LIKE LOWER(CONCAT('%', :busca, '%')) " +
            "GROUP BY t.id, t.titulo, t.descricao, u.nome, u.perfil, t.idAutor, t.criadoEm, t.ativo, t.fechado " +
            "ORDER BY t.criadoEm DESC")
    List<TopicoResumoDTO> listarRecentes(Pageable pageable, @Param("busca") String busca);
}