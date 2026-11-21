package com.cursovalido.backend.aulas.repository;

import com.cursovalido.backend.aulas.entity.GamificacaoAluno;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GamificacaoAlunoRepository extends JpaRepository<GamificacaoAluno, Long> {
    @Modifying
    @Query(value = """
            INSERT INTO tb_gamificacao_alunos (aluno_id, nivel, xp_nivel, xp_total, atualizado_em)
            SELECT :idAluno, 1, 0, 0, CURRENT_TIMESTAMP
            WHERE NOT EXISTS (
                SELECT 1 FROM tb_gamificacao_alunos WHERE aluno_id = :idAluno
            )
            """, nativeQuery = true)
    void criarSeAusente(@Param("idAluno") Long idAluno);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GamificacaoAluno g where g.idAluno = :idAluno")
    Optional<GamificacaoAluno> bloquear(@Param("idAluno") Long idAluno);
}
