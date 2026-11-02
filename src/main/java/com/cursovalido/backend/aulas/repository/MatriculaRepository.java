package com.cursovalido.backend.aulas.repository;

import com.cursovalido.backend.aulas.entity.Matricula;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {
    Optional<Matricula> findByIdAlunoAndIdCurso(Long idAluno, Long idCurso);
    List<Matricula> findByIdAlunoOrderByIdDesc(Long idAluno);
}
