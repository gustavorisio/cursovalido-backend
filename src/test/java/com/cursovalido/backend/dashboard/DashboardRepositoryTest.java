package com.cursovalido.backend.dashboard;

import com.cursovalido.backend.dashboard.dto.DashboardResponse.AtividadeDia;
import com.cursovalido.backend.dashboard.repository.DashboardRepository;
import com.cursovalido.backend.forum.support.PostgresTeste;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.*;

// As consultas usam funcoes de data do PostgreSQL (age, date_part), por isso o teste roda no PostgreSQL.
@ExtendWith(PostgresTeste.CicloDeVida.class)
class DashboardRepositoryTest {
    private JdbcTemplate jdbc;
    private DashboardRepository repository;

    @BeforeEach
    void preparar() {
        var propriedades = new HashMap<String, Supplier<?>>();
        PostgresTeste.configurar(propriedades::put, "dashboard_repositorio");
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                propriedades.get("spring.datasource.url").get().toString(),
                propriedades.get("spring.datasource.username").get().toString(),
                propriedades.get("spring.datasource.password").get().toString()));
        for (String tabela : new String[] { "tb_gamificacao_alunos", "tb_conclusoes_cursos", "tb_progresso_aulas",
                "tb_matriculas", "tb_aulas", "tb_modulos", "tb_cursos", "tb_comments", "tb_topics", "tb_usuarios" })
            jdbc.execute("DROP TABLE IF EXISTS " + tabela);
        jdbc.execute("""
                CREATE TABLE tb_usuarios (id BIGINT PRIMARY KEY, ativo BOOLEAN, perfil VARCHAR(30),
                  excluido_em TIMESTAMP)""");
        jdbc.execute("CREATE TABLE tb_cursos (id BIGINT PRIMARY KEY, titulo VARCHAR(255), ativo BOOLEAN, criado_em TIMESTAMP)");
        jdbc.execute("CREATE TABLE tb_modulos (id BIGINT PRIMARY KEY, curso_id BIGINT, ativo BOOLEAN)");
        jdbc.execute("CREATE TABLE tb_aulas (id BIGSERIAL PRIMARY KEY, modulo_id BIGINT, ativo BOOLEAN, criado_em TIMESTAMP)");
        jdbc.execute("CREATE TABLE tb_matriculas (id BIGINT PRIMARY KEY, aluno_id BIGINT, curso_id BIGINT, matriculado_em TIMESTAMP)");
        jdbc.execute("CREATE TABLE tb_topics (active BOOLEAN, created_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE tb_comments (active BOOLEAN, created_at TIMESTAMP)");
        jdbc.execute("""
                CREATE TABLE tb_progresso_aulas (matricula_id BIGINT, concluida BOOLEAN, concluida_em TIMESTAMP)""");
        jdbc.execute("""
                CREATE TABLE tb_conclusoes_cursos (aluno_id BIGINT, curso_id BIGINT, concluido_em TIMESTAMP)""");
        jdbc.execute("CREATE TABLE tb_gamificacao_alunos (aluno_id BIGINT, nivel INT)");
        repository = new DashboardRepository(jdbc);
    }

    @Test
    void resumoContaSomenteAlunosAtivosNaoExcluidosEIndicadores() {
        jdbc.execute("""
                INSERT INTO tb_usuarios VALUES
                  (1, TRUE, 'ALUNO', NULL), (2, TRUE, 'ALUNO', NULL),
                  (3, FALSE, 'ALUNO', NULL), (4, TRUE, 'ALUNO', now()),
                  (5, TRUE, 'PROFESSOR', NULL)""");
        jdbc.execute("INSERT INTO tb_cursos VALUES (10, 'Java', TRUE, now())");
        jdbc.execute("INSERT INTO tb_modulos VALUES (20, 10, TRUE)");
        jdbc.execute("INSERT INTO tb_aulas (modulo_id, ativo, criado_em) VALUES (20, TRUE, now()), (20, TRUE, now())");
        jdbc.execute("INSERT INTO tb_matriculas VALUES (40, 1, 10, now()), (41, 2, 10, now())");
        jdbc.execute("INSERT INTO tb_progresso_aulas VALUES (40, TRUE, now()), (40, TRUE, now()), (41, FALSE, NULL)");
        jdbc.execute("INSERT INTO tb_conclusoes_cursos VALUES (1, 10, now())");
        jdbc.execute("INSERT INTO tb_gamificacao_alunos VALUES (1, 3), (2, 1)");
        jdbc.execute("INSERT INTO tb_comments VALUES (TRUE, now()), (TRUE, now()), (FALSE, now())");

        var resumo = repository.resumo();

        assertEquals(2, resumo.alunosAtivos());
        assertEquals(1, resumo.professoresAtivos());
        assertEquals(1, resumo.cursosAtivos());
        assertEquals(2, resumo.aulasAtivas());
        assertEquals(2, resumo.matriculas());
        assertEquals(2, resumo.aulasConcluidas());
        assertEquals(2, resumo.comentariosAtivos());
        assertEquals(1, resumo.cursosConcluidos());
        assertEquals(1, resumo.alunosEngajados());
        assertEquals(50.0, resumo.taxaConclusaoMatriculas());
        assertEquals(2.0, resumo.nivelMedioAlunos());
    }

    @Test
    void atividadeContaRegistrosEntreOInicioDoDiaEAMeiaNoiteSeguinte() {
        LocalDate data = LocalDate.of(2026, 5, 10);
        jdbc.execute("""
                INSERT INTO tb_matriculas VALUES (1, 1, 1, '2026-05-09 23:59:59'), (2, 1, 1, '2026-05-10 00:00:00'),
                  (3, 1, 1, '2026-05-10 23:59:59'), (4, 1, 1, '2026-05-11 00:00:00')""");
        jdbc.execute("INSERT INTO tb_cursos VALUES (1, 'Java', FALSE, '2026-05-10 12:00:00')");
        jdbc.execute("INSERT INTO tb_aulas (modulo_id, ativo, criado_em) VALUES (1, TRUE, '2026-05-10 12:00:00')");
        jdbc.execute("INSERT INTO tb_topics VALUES (TRUE, '2026-05-10 12:00:00')");
        jdbc.execute("INSERT INTO tb_progresso_aulas VALUES (2, TRUE, '2026-05-10 08:00:00')");
        jdbc.execute("INSERT INTO tb_conclusoes_cursos VALUES (1, 1, '2026-05-10 09:00:00')");

        assertEquals(new AtividadeDia(data, 2, 1, 1, 1, 1, 1),
                repository.atividadeDia(data, data.atStartOfDay(), data.plusDays(1).atStartOfDay()));
        LocalDate diaSemAtividade = data.plusDays(2);
        assertEquals(new AtividadeDia(diaSemAtividade, 0, 0, 0, 0, 0, 0),
                repository.atividadeDia(diaSemAtividade, diaSemAtividade.atStartOfDay(),
                        diaSemAtividade.plusDays(1).atStartOfDay()));
    }

    @Test
    void rankingEPerfisSemIdentificarAlunos() {
        jdbc.execute("""
                INSERT INTO tb_usuarios VALUES (1, TRUE, 'ALUNO', NULL), (2, TRUE, 'PROFESSOR', NULL)""");
        jdbc.execute("INSERT INTO tb_cursos VALUES (10, 'Java', TRUE, now()), (11, 'Inativo', FALSE, now())");
        jdbc.execute("INSERT INTO tb_matriculas VALUES (40, 1, 10, now())");
        jdbc.execute("INSERT INTO tb_progresso_aulas VALUES (40, TRUE, now())");
        jdbc.execute("INSERT INTO tb_conclusoes_cursos VALUES (1, 10, now())");

        var ranking = repository.cursosMaisProcurados();
        assertEquals(1, ranking.size());
        assertEquals("Java", ranking.getFirst().titulo());
        assertEquals(100.0, ranking.getFirst().taxaConclusao());

        var perfis = repository.usuariosPorPerfil();
        assertEquals(2, perfis.size());

        var meses = repository.matriculasPorMes(LocalDate.now().withDayOfMonth(1),
                LocalDate.now().withDayOfMonth(1).plusMonths(1));
        assertEquals(1, meses.size());
        assertEquals(1, meses.getFirst().matriculas());
        assertEquals(1, meses.getFirst().aulasConcluidas());
    }
}
