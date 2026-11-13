package com.cursovalido.backend.aulas.repository;

import com.cursovalido.backend.aulas.entity.Questionario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionarioRepository extends JpaRepository<Questionario, Long> {
    Optional<Questionario> findByIdCursoAndIdModulo(Long idCurso, Long idModulo);
    List<Questionario> findByIdCursoAndPublicadoTrueOrderByIdAsc(Long idCurso);
}
