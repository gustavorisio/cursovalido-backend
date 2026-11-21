package com.cursovalido.backend.aulas.repository;

import com.cursovalido.backend.aulas.entity.ConclusaoCurso;
import com.cursovalido.backend.aulas.entity.Matricula;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConclusaoCursoRepository extends JpaRepository<ConclusaoCurso, Long> {
    boolean existsByIdAlunoAndIdCurso(Long idAluno, Long idCurso);

    long countByIdAluno(Long idAluno);

    @Query("select count(a) from Aula a, Modulo m where a.idModulo = m.id and m.idCurso = :idCurso "
            + "and a.ativo = true and m.ativo = true")
    long contarAulasAtivas(@Param("idCurso") Long idCurso);

    @Query("select count(p) from ProgressoAula p, Aula a, Modulo m where p.idMatricula = :idMatricula "
            + "and p.idAula = a.id and a.idModulo = m.id and m.idCurso = :idCurso "
            + "and p.concluida = true and a.ativo = true and m.ativo = true")
    long contarAulasConcluidas(@Param("idMatricula") Long idMatricula, @Param("idCurso") Long idCurso);

    @Query("select m from Matricula m where exists (select c.id from Curso c where c.id = m.idCurso and c.ativo = true) "
            + "and not exists (select cc.id from ConclusaoCurso cc where cc.idAluno = m.idAluno and cc.idCurso = m.idCurso)")
    List<Matricula> matriculasSemConclusao();
}
