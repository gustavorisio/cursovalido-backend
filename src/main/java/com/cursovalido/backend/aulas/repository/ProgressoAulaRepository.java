package com.cursovalido.backend.aulas.repository;

import com.cursovalido.backend.aulas.entity.ProgressoAula;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressoAulaRepository extends JpaRepository<ProgressoAula, Long> {
    Optional<ProgressoAula> findByIdMatriculaAndIdAula(Long idMatricula, Long idAula);

    void deleteByIdAula(Long idAula);
}
