package com.cursovalido.backend.dashboard.repository;

import com.cursovalido.backend.dashboard.dto.DashboardResponse.*;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DashboardRepository {
        private static final String ALUNOS_ATIVOS = "u.perfil = 'ALUNO' AND u.ativo AND u.excluido_em IS NULL";

        private final JdbcTemplate jdbc;

        public Resumo resumo() {
                return jdbc.queryForObject(
                                """
                                                SELECT
                                                  (SELECT count(*) FROM tb_usuarios u WHERE %1$s) AS alunos,
                                                  (SELECT count(*) FROM tb_usuarios WHERE ativo AND perfil='PROFESSOR') AS professores,
                                                  (SELECT count(*) FROM tb_cursos WHERE ativo) AS cursos,
                                                  (SELECT count(*) FROM tb_aulas a JOIN tb_modulos m ON m.id=a.modulo_id
                                                     JOIN tb_cursos c ON c.id=m.curso_id WHERE a.ativo AND m.ativo AND c.ativo) AS aulas,
                                                  (SELECT count(*) FROM tb_matriculas m JOIN tb_cursos c ON c.id=m.curso_id WHERE c.ativo) AS matriculas,
                                                  (SELECT count(*) FROM tb_topics WHERE active) AS topicos,
                                                  (SELECT count(*) FROM tb_comments WHERE active) AS comentarios,
                                                  (SELECT count(*) FROM tb_progresso_aulas WHERE concluida) AS aulas_concluidas,
                                                  (SELECT count(*) FROM tb_conclusoes_cursos cc JOIN tb_cursos c ON c.id=cc.curso_id WHERE c.ativo) AS cursos_concluidos,
                                                  (SELECT count(DISTINCT m.aluno_id) FROM tb_progresso_aulas p
                                                     JOIN tb_matriculas m ON m.id=p.matricula_id WHERE p.concluida) AS alunos_engajados,
                                                  (SELECT avg(g.nivel) FROM tb_gamificacao_alunos g
                                                     JOIN tb_usuarios u ON u.id=g.aluno_id WHERE %1$s) AS nivel_medio
                                                """
                                                .formatted(ALUNOS_ATIVOS),
                                (rs, row) -> {
                                        long matriculas = rs.getLong("matriculas");
                                        long cursosConcluidos = rs.getLong("cursos_concluidos");
                                        long aulasConcluidas = rs.getLong("aulas_concluidas");
                                        long engajados = rs.getLong("alunos_engajados");
                                        return new Resumo(rs.getLong("alunos"), rs.getLong("professores"),
                                                        rs.getLong("cursos"), rs.getLong("aulas"), matriculas,
                                                        rs.getLong("topicos"), rs.getLong("comentarios"), aulasConcluidas, cursosConcluidos,
                                                        engajados, percentual(cursosConcluidos, matriculas),
                                                        decimal(rs.getObject("nivel_medio")));
                                });
        }

        public AtividadeDia atividadeDia(LocalDate data, LocalDateTime inicio, LocalDateTime fim) {
                var inicioSql = Timestamp.valueOf(inicio);
                var fimSql = Timestamp.valueOf(fim);
                return jdbc.queryForObject(
                                """
                                                SELECT
                                                  (SELECT count(*) FROM tb_matriculas WHERE matriculado_em >= ? AND matriculado_em < ?) AS matriculas,
                                                  (SELECT count(*) FROM tb_progresso_aulas WHERE concluida AND concluida_em >= ? AND concluida_em < ?) AS aulas_concluidas,
                                                  (SELECT count(*) FROM tb_conclusoes_cursos WHERE concluido_em >= ? AND concluido_em < ?) AS cursos_concluidos,
                                                  (SELECT count(*) FROM tb_cursos WHERE criado_em >= ? AND criado_em < ?) AS cursos,
                                                  (SELECT count(*) FROM tb_aulas WHERE criado_em >= ? AND criado_em < ?) AS aulas,
                                                  (SELECT count(*) FROM tb_topics WHERE created_at >= ? AND created_at < ?) AS topicos
                                                """,
                                (rs, row) -> new AtividadeDia(data, rs.getLong("matriculas"),
                                                rs.getLong("aulas_concluidas"), rs.getLong("cursos_concluidos"),
                                                rs.getLong("cursos"), rs.getLong("aulas"), rs.getLong("topicos")),
                                inicioSql, fimSql, inicioSql, fimSql, inicioSql, fimSql, inicioSql, fimSql,
                                inicioSql, fimSql, inicioSql, fimSql);
        }

        public List<MatriculasMes> matriculasPorMes(LocalDate inicio, LocalDate fimExclusivo) {
                var inicioSql = Timestamp.valueOf(inicio.atStartOfDay());
                var fimSql = Timestamp.valueOf(fimExclusivo.atStartOfDay());
                Map<String, Long> aulas = jdbc.query("""
                                SELECT extract(year FROM p.concluida_em) AS ano,
                                       extract(month FROM p.concluida_em) AS mes, count(*) AS quantidade
                                FROM tb_progresso_aulas p
                                WHERE p.concluida AND p.concluida_em >= ? AND p.concluida_em < ?
                                GROUP BY extract(year FROM p.concluida_em), extract(month FROM p.concluida_em)
                                """,
                                (rs, row) -> Map.entry("%04d-%02d".formatted(rs.getInt("ano"), rs.getInt("mes")),
                                                rs.getLong("quantidade")),
                                inicioSql, fimSql).stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                return jdbc.query("""
                                SELECT extract(year FROM m.matriculado_em) AS ano,
                                       extract(month FROM m.matriculado_em) AS mes, count(*) AS quantidade
                                FROM tb_matriculas m JOIN tb_cursos c ON c.id=m.curso_id
                                WHERE c.ativo AND m.matriculado_em >= ? AND m.matriculado_em < ?
                                GROUP BY extract(year FROM m.matriculado_em), extract(month FROM m.matriculado_em)
                                ORDER BY ano, mes
                                """,
                                (rs, row) -> {
                                        String mes = "%04d-%02d".formatted(rs.getInt("ano"), rs.getInt("mes"));
                                        return new MatriculasMes(mes, rs.getLong("quantidade"),
                                                        aulas.getOrDefault(mes, 0L));
                                },
                                inicioSql, fimSql);
        }

        public List<CursoPopular> cursosMaisProcurados() {
                return jdbc.query("""
                                SELECT c.id, c.titulo,
                                  (SELECT count(*) FROM tb_matriculas m WHERE m.curso_id=c.id) AS matriculas,
                                  (SELECT count(*) FROM tb_progresso_aulas p JOIN tb_matriculas m ON m.id=p.matricula_id
                                     WHERE m.curso_id=c.id AND p.concluida) AS aulas_concluidas,
                                  (SELECT count(*) FROM tb_conclusoes_cursos cc WHERE cc.curso_id=c.id) AS conclusoes
                                FROM tb_cursos c
                                WHERE c.ativo
                                ORDER BY matriculas DESC, aulas_concluidas DESC, c.id ASC LIMIT 10
                                """, (rs, row) -> {
                        long matriculas = rs.getLong("matriculas");
                        long conclusoes = rs.getLong("conclusoes");
                        return new CursoPopular(rs.getLong("id"), rs.getString("titulo"), matriculas,
                                        rs.getLong("aulas_concluidas"), conclusoes, percentual(conclusoes, matriculas));
                });
        }

        public List<UsuariosPerfil> usuariosPorPerfil() {
                return jdbc.query(
                                "SELECT perfil, count(*) AS quantidade FROM tb_usuarios WHERE ativo GROUP BY perfil ORDER BY perfil",
                                (rs, row) -> new UsuariosPerfil(rs.getString("perfil"), rs.getLong("quantidade")));
        }

        private static double percentual(long parte, long total) {
                return total == 0 ? 0 : arredondar(parte * 100.0 / total);
        }

        private static double arredondar(double valor) {
                return Math.round(valor * 10.0) / 10.0;
        }

        private static Double decimal(Object valor) {
                return valor instanceof Number numero ? arredondar(numero.doubleValue()) : null;
        }
}
