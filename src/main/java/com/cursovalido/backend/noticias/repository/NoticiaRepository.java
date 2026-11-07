package com.cursovalido.backend.noticias.repository;

import com.cursovalido.backend.noticias.entity.Noticia;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticiaRepository extends JpaRepository<Noticia, Long> {
    @Query("SELECT n FROM Noticia n WHERE n.ativo = true " +
            "AND LOWER(n.titulo) LIKE LOWER(CONCAT('%', :busca, '%')) " +
            "ORDER BY n.criadoEm DESC, n.id DESC")
    List<Noticia> listarRecentes(Pageable pagina, @Param("busca") String busca);
}
