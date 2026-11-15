package com.cursovalido.backend.dashboard;

import com.cursovalido.backend.dashboard.repository.DashboardRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;

class DashboardRepositoryTest {
    private JdbcTemplate jdbc;
    private DashboardRepository repository;
    private LocalDate hoje;

    @BeforeEach
    void preparar() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:dashboard;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE tb_usuarios (ativo BOOLEAN, perfil VARCHAR, data_nascimento DATE)");
        jdbc.execute("CREATE TABLE tb_cursos (id BIGINT, ativo BOOLEAN)");
        jdbc.execute("CREATE TABLE tb_modulos (id BIGINT, curso_id BIGINT, ativo BOOLEAN)");
        jdbc.execute("CREATE TABLE tb_aulas (modulo_id BIGINT, ativo BOOLEAN)");
        jdbc.execute("CREATE TABLE tb_matriculas (curso_id BIGINT)");
        jdbc.execute("CREATE TABLE tb_topics (active BOOLEAN)");
        jdbc.execute("CREATE TABLE tb_noticias (ativo BOOLEAN)");
        repository = new DashboardRepository(jdbc);
        hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
    }

    private void usuario(boolean ativo, String perfil, LocalDate nascimento) {
        jdbc.update("INSERT INTO tb_usuarios VALUES (?, ?, ?)", ativo, perfil, nascimento);
    }

    @Test
    void mediaConsideraAniversarioEExcluiEquipeInativosEDatasAusentesOuFuturas() {
        usuario(true, "ALUNO", hoje.minusYears(20));
        usuario(true, "ALUNO", hoje.minusYears(31).plusDays(1));
        usuario(true, "ALUNO", null);
        usuario(true, "ALUNO", hoje.plusDays(1));
        usuario(false, "ALUNO", hoje.minusYears(60));
        usuario(true, "PROFESSOR", hoje.minusYears(50));
        usuario(true, "ADMINISTRADOR", hoje.minusYears(40));
        var resumo = repository.resumo();
        assertEquals(25.0, resumo.mediaIdadeAlunos());
        assertEquals(2, resumo.alunosComNascimentoInformado());
        assertEquals(4, resumo.alunosAtivos());
    }

    @Test
    void semDatasValidasRetornaMediaAusente() {
        usuario(true, "ALUNO", null);
        var resumo = repository.resumo();
        assertNull(resumo.mediaIdadeAlunos());
        assertEquals(0, resumo.alunosComNascimentoInformado());
    }
}