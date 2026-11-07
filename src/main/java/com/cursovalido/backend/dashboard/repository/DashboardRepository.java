package com.cursovalido.backend.dashboard.repository;

import com.cursovalido.backend.dashboard.dto.DashboardResponse.*;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DashboardRepository {
    private final JdbcTemplate jdbc;

    public Resumo resumo() {
        return jdbc.queryForObject("""
                SELECT
                  (SELECT count(*) FROM tb_usuarios WHERE ativo AND perfil='ALUNO') AS alunos,
                  (SELECT count(*) FROM tb_usuarios WHERE ativo AND perfil='PROFESSOR') AS professores,
                  (SELECT count(*) FROM tb_cursos WHERE ativo) AS cursos,
                  (SELECT count(*) FROM tb_aulas a JOIN tb_modulos m ON m.id=a.modulo_id
                     JOIN tb_cursos c ON c.id=m.curso_id WHERE a.ativo AND m.ativo AND c.ativo) AS aulas,
                  (SELECT count(*) FROM tb_matriculas m JOIN tb_cursos c ON c.id=m.curso_id WHERE c.ativo) AS matriculas,
                  (SELECT count(*) FROM tb_topics WHERE active) AS topicos,
                  (SELECT count(*) FROM tb_noticias WHERE ativo) AS noticias
                """, (rs, row) -> new Resumo(rs.getLong("alunos"), rs.getLong("professores"),
                rs.getLong("cursos"), rs.getLong("aulas"), rs.getLong("matriculas"),
                rs.getLong("topicos"), rs.getLong("noticias")));
    }

    public List<MatriculasMes> matriculasPorMes(LocalDate inicio, LocalDate fimExclusivo) {
        return jdbc.query("""
                SELECT extract(year FROM m.matriculado_em) AS ano,
                       extract(month FROM m.matriculado_em) AS mes, count(*) AS quantidade
                FROM tb_matriculas m JOIN tb_cursos c ON c.id=m.curso_id
                WHERE c.ativo AND m.matriculado_em >= ? AND m.matriculado_em < ?
                GROUP BY extract(year FROM m.matriculado_em), extract(month FROM m.matriculado_em)
                ORDER BY ano, mes
                """, (rs, row) -> new MatriculasMes("%04d-%02d".formatted(rs.getInt("ano"), rs.getInt("mes")),
                rs.getLong("quantidade")), Timestamp.valueOf(inicio.atStartOfDay()),
                Timestamp.valueOf(fimExclusivo.atStartOfDay()));
    }

    public List<CursoPopular> cursosMaisProcurados() {
        return jdbc.query("""
                SELECT c.id, c.titulo, count(m.id) AS quantidade
                FROM tb_cursos c LEFT JOIN tb_matriculas m ON m.curso_id=c.id
                WHERE c.ativo GROUP BY c.id, c.titulo
                ORDER BY quantidade DESC, c.id ASC LIMIT 10
                """, (rs, row) -> new CursoPopular(rs.getLong("id"), rs.getString("titulo"), rs.getLong("quantidade")));
    }

    public List<UsuariosPerfil> usuariosPorPerfil() {
        return jdbc.query("SELECT perfil, count(*) AS quantidade FROM tb_usuarios WHERE ativo GROUP BY perfil ORDER BY perfil",
                (rs, row) -> new UsuariosPerfil(rs.getString("perfil"), rs.getLong("quantidade")));
    }
}
