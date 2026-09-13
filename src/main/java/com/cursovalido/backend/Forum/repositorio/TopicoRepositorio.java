package com.cursovalido.backend.Forum.repositorio;

import com.cursovalido.backend.Forum.entidade.Topico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface TopicoRepositorio extends JpaRepository<Topico, Long> {
    @Query("SELECT t FROM Topico t JOIN FETCH t.usuarioAutor WHERE t.ativo = true ORDER BY t.criadoEm DESC")
    List<Topico> listarRecentes(Pageable pageable);
}