package com.cursovalido.backend.aulas.repository;

import com.cursovalido.backend.aulas.entity.Curso;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CursoRepository extends JpaRepository<Curso, Long> {
    List<Curso> findByIdProfessorAndAtivoTrueOrderByIdDesc(Long idProfessor);
    List<Curso> findByAtivoTrueOrderByIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Curso c where c.id = :id")
    Optional<Curso> buscarParaMatricula(@Param("id") Long id);
}
