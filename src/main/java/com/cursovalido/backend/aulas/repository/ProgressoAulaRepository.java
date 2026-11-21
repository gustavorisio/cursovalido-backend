package com.cursovalido.backend.aulas.repository;

import com.cursovalido.backend.aulas.entity.ProgressoAula;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProgressoAulaRepository extends JpaRepository<ProgressoAula, Long> {
    Optional<ProgressoAula> findByIdMatriculaAndIdAula(Long idMatricula, Long idAula);

    void deleteByIdAula(Long idAula);

    @Query(value = """
            SELECT MAX(p.concluida_em)
            FROM tb_progresso_aulas p
            JOIN tb_matriculas m ON m.id = p.matricula_id
            WHERE m.aluno_id = :idAluno AND p.concluida = true AND p.concluida_em > :desde
            """, nativeQuery = true)
    Optional<LocalDateTime> buscarUltimaConclusaoDesde(@Param("idAluno") Long idAluno,
            @Param("desde") LocalDateTime desde);
}
