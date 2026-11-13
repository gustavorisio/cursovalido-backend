package com.cursovalido.backend.aulas.repository;

import com.cursovalido.backend.aulas.entity.RespostaAlunoCaminhos;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RespostaAlunoCaminhosRepository extends JpaRepository<RespostaAlunoCaminhos, Long> {
    Optional<RespostaAlunoCaminhos> findByIdAlunoAndIdQuestionario(Long idAluno, Long idQuestionario);
}
