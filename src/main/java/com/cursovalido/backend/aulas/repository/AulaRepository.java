package com.cursovalido.backend.aulas.repository;

import com.cursovalido.backend.aulas.entity.Aula;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AulaRepository extends JpaRepository<Aula, Long> {
    List<Aula> findByIdModuloAndAtivoTrueOrderByOrdemAscIdAsc(Long idModulo);

    List<Aula> findByIdModuloOrderByOrdemAscIdAsc(Long idModulo);
}
