package com.cursovalido.backend.aulas.repository;

import com.cursovalido.backend.aulas.entity.Matricula;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {
    Optional<Matricula> findByIdAlunoAndIdCurso(Long idAluno, Long idCurso);
    List<Matricula> findByIdAlunoOrderByIdDesc(Long idAluno);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Matricula m where m.id = :id")
    Optional<Matricula> bloquearPorId(@Param("id") Long id);
}
