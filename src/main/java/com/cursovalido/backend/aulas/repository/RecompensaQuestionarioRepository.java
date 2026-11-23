package com.cursovalido.backend.aulas.repository;

import com.cursovalido.backend.aulas.entity.RecompensaQuestionario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecompensaQuestionarioRepository extends JpaRepository<RecompensaQuestionario, Long> {
    boolean existsByIdAlunoAndIdQuestionario(Long idAluno, Long idQuestionario);

    long countByIdAluno(Long idAluno);
}
