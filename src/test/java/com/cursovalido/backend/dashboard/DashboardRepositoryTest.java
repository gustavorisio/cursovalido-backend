package com.cursovalido.backend.dashboard;

import com.cursovalido.backend.dashboard.dto.DashboardResponse.AtividadeDia;
import com.cursovalido.backend.dashboard.repository.DashboardRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardRepositoryTest {
    private JdbcTemplate jdbc;
    private DashboardRepository repository;

    @BeforeEach
    void preparar() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:dashboard;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE tb_usuarios (ativo BOOLEAN, perfil VARCHAR)");
        jdbc.execute("CREATE TABLE tb_cursos (id BIGINT, ativo BOOLEAN, criado_em TIMESTAMP)");
        jdbc.execute("CREATE TABLE tb_modulos (id BIGINT, curso_id BIGINT, ativo BOOLEAN)");
        jdbc.execute("CREATE TABLE tb_aulas (modulo_id BIGINT, ativo BOOLEAN, criado_em TIMESTAMP)");
        jdbc.execute("CREATE TABLE tb_matriculas (curso_id BIGINT, matriculado_em TIMESTAMP)");
        jdbc.execute("CREATE TABLE tb_topics (active BOOLEAN, created_at TIMESTAMP)");
        repository = new DashboardRepository(jdbc);
    }

    @Test
    void resumoContaUsuariosAtivosPorPerfil() {
        jdbc.execute("INSERT INTO tb_usuarios VALUES (TRUE, 'ALUNO'), (FALSE, 'ALUNO'), (TRUE, 'PROFESSOR')");
        var resumo = repository.resumo();
        assertEquals(1, resumo.alunosAtivos());
        assertEquals(1, resumo.professoresAtivos());
    }

    @Test
    void atividadeContaRegistrosEntreOInicioDoDiaEAMeiaNoiteSeguinte() {
        LocalDate data = LocalDate.of(2026, 5, 10);
        jdbc.execute("""
                INSERT INTO tb_matriculas VALUES (1, '2026-05-09 23:59:59'), (1, '2026-05-10 00:00:00'),
                  (1, '2026-05-10 23:59:59'), (1, '2026-05-11 00:00:00')
                """);
        jdbc.execute("INSERT INTO tb_cursos VALUES (1, FALSE, '2026-05-10 12:00:00')");
        jdbc.execute("INSERT INTO tb_aulas VALUES (1, TRUE, '2026-05-10 12:00:00')");
        jdbc.execute("INSERT INTO tb_topics VALUES (TRUE, '2026-05-10 12:00:00')");
        assertEquals(new AtividadeDia(data, 2, 1, 1, 1),
                repository.atividadeDia(data, data.atStartOfDay(), data.plusDays(1).atStartOfDay()));
        LocalDate diaSemAtividade = data.plusDays(2);
        assertEquals(new AtividadeDia(diaSemAtividade, 0, 0, 0, 0),
                repository.atividadeDia(diaSemAtividade, diaSemAtividade.atStartOfDay(),
                        diaSemAtividade.plusDays(1).atStartOfDay()));
    }
}
