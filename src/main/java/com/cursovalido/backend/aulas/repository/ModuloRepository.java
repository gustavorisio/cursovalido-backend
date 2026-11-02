package com.cursovalido.backend.aulas.repository;

import com.cursovalido.backend.aulas.entity.Modulo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuloRepository extends JpaRepository<Modulo, Long> {
    List<Modulo> findByIdCursoAndAtivoTrueOrderByOrdemAscIdAsc(Long idCurso);
}
