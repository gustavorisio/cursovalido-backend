package com.cursovalido.backend.aulas.repository;

import com.cursovalido.backend.aulas.entity.Curso;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface CursoRepository extends JpaRepository<Curso, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Curso c set c.tema = null where c.tema.id = :idTema")
    void removerTema(@Param("idTema") Long idTema);
    List<Curso> findByIdProfessorAndAtivoTrueOrderByIdDesc(Long idProfessor);

    List<Curso> findByAtivoTrueOrderByIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Curso c where c.id = :id")
    Optional<Curso> buscarParaMatricula(@Param("id") Long id);
}
