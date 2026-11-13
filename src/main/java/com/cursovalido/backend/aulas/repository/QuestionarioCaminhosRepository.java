package com.cursovalido.backend.aulas.repository;

import com.cursovalido.backend.aulas.entity.QuestionarioCaminhos;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface QuestionarioCaminhosRepository extends JpaRepository<QuestionarioCaminhos, Long> {
    List<QuestionarioCaminhos> findByAtivoTrueOrderByIdDesc();
    List<QuestionarioCaminhos> findByAtivoTrueAndPublicadoTrueOrderByIdDesc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from QuestionarioCaminhos q where q.id = :id")
    Optional<QuestionarioCaminhos> buscarParaAlterar(@Param("id") Long id);
}
