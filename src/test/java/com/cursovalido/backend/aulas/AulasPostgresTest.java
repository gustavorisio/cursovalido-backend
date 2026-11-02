package com.cursovalido.backend.aulas;

import com.cursovalido.backend.forum.support.PostgresTeste;
import com.cursovalido.backend.aulas.dto.CursoRequest;
import com.cursovalido.backend.aulas.service.CursoService;
import com.cursovalido.backend.aulas.service.AlunoAulasService;
import com.cursovalido.backend.aulas.service.ModuloService;
import com.cursovalido.backend.aulas.service.AulaService;
import com.cursovalido.backend.aulas.service.ProgressoAulaService;
import com.cursovalido.backend.aulas.dto.ModuloRequest;
import com.cursovalido.backend.aulas.dto.AulaRequest;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.repository.UsuarioRepositorio;
import com.cursovalido.backend.authentication.service.ContaUsuarioServico;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;

// Repete o fluxo HTTP em PostgreSQL temporario, com as migracoes e validacao JPA reais.
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@ExtendWith(PostgresTeste.CicloDeVida.class)
@Import(AulasPostgresTest.MigracaoTeste.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AulasPostgresTest extends AulasIntegracaoTest {
    @Autowired private UsuarioRepositorio usuarios;
    @Autowired private CursoService cursos;
    @Autowired private ContaUsuarioServico contas;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AlunoAulasService alunoAulas;
    @Autowired private ModuloService modulos;
    @Autowired private AulaService aulas;
    @Autowired private ProgressoAulaService progressos;

    @Test
    void excluirContaDoAlunoRemoveMatriculaEPreservaCurso() {
        var professor = usuarios.findByEmail("professor@aulas.example.com").orElseThrow();
        var curso = cursos.criar(new CursoRequest("Java", "Curso gratuito"), professor.getId());
        var aluno = criarProfessor("aluno-matriculado");
        aluno.setPerfil(Perfil.ALUNO);
        usuarios.saveAndFlush(aluno);
        alunoAulas.matricular(curso.getId(), aluno.getId());
        var modulo = modulos.criar(curso.getId(), new ModuloRequest("Inicio", "Basico", 1), professor.getId());
        var aula = aulas.criar(modulo.getId(), new AulaRequest("Java", "Introducao", 1,
                "https://youtu.be/M7lc1UVf-VE"), professor.getId());
        progressos.concluir(aula.getId(), aluno.getId());
        contas.excluirConta(aluno.getId());
        usuarios.flush();
        assertThat(jdbc.queryForObject("select count(*) from tb_matriculas where aluno_id = ?", Long.class, aluno.getId()))
                .isZero();
        assertThat(jdbc.queryForObject("select count(*) from tb_cursos where id = ?", Long.class, curso.getId()))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from tb_progresso_aulas where aula_id = ?", Long.class, aula.getId()))
                .isZero();
    }

    @Test
    void bancoImpedeMatriculaDuplicada() {
        var professor = usuarios.findByEmail("professor@aulas.example.com").orElseThrow();
        var curso = cursos.criar(new CursoRequest("Java", "Curso gratuito"), professor.getId());
        var aluno = criarProfessor("aluno-matriculado");
        aluno.setPerfil(Perfil.ALUNO);
        usuarios.saveAndFlush(aluno);
        alunoAulas.matricular(curso.getId(), aluno.getId());
        assertThatThrownBy(() -> jdbc.update(
                "insert into tb_matriculas (aluno_id, curso_id, matriculado_em) values (?, ?, CURRENT_TIMESTAMP)",
                aluno.getId(), curso.getId()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void excluirContaPreservaCursoSemResponsavel() {
        var professor = usuarios.findByEmail("professor@aulas.example.com").orElseThrow();
        var curso = cursos.criar(new CursoRequest("Java", "Curso preservado"), professor.getId());
        contas.excluirConta(professor.getId());
        usuarios.flush();
        assertThat(usuarios.findById(professor.getId())).isEmpty();
        assertThat(jdbc.queryForObject("select professor_id from tb_cursos where id = ?", Long.class, curso.getId()))
                .isNull();
        assertThat(jdbc.queryForObject("select titulo from tb_cursos where id = ?", String.class, curso.getId()))
                .isEqualTo("Java");
    }

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registry) {
        PostgresTeste.configurar(registry, "aulas_api");
    }

    @TestConfiguration
    static class MigracaoTeste {
        @Bean(initMethod = "migrate")
        Flyway flyway(DataSource dataSource) {
            return Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load();
        }

        @Bean
        static BeanFactoryPostProcessor migrarAntesDoJpa() {
            return beanFactory -> beanFactory.getBeanDefinition("entityManagerFactory").setDependsOn("flyway");
        }
    }
}
