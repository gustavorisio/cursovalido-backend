package com.cursovalido.backend.aulas.repository;

import com.cursovalido.backend.aulas.entity.TentativaQuestionario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TentativaQuestionarioRepository extends JpaRepository<TentativaQuestionario, Long> {
    Optional<TentativaQuestionario> findFirstByIdAlunoAndIdQuestionarioAndVersaoOrderByIdDesc(Long idAluno, Long idQuestionario, int versao);
}
