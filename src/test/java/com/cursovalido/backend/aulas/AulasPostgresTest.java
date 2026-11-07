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
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.service.ContaUsuarioService;
import com.cursovalido.backend.authentication.service.AdministradorInicialService;
import com.cursovalido.backend.authentication.repository.InicializacaoAdministradorRepository;
import jakarta.persistence.EntityManager;
import org.springframework.test.util.ReflectionTestUtils;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
@SpringBootTest(properties = { "spring.jpa.hibernate.ddl-auto=validate", "aplicacao.admin.email=", "aplicacao.admin.senha=" })
@ExtendWith(PostgresTeste.CicloDeVida.class)
@Import(AulasPostgresTest.MigracaoTeste.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AulasPostgresTest extends AulasIntegracaoTest {
    @Autowired private AdministradorInicialService administradorInicial;
    @Autowired private InicializacaoAdministradorRepository inicializacao;
    @Autowired private EntityManager entidades;
    @Autowired private DataSource fonte;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private CursoService cursos;
    @Autowired private ContaUsuarioService contas;
    @Autowired private com.cursovalido.backend.authentication.service.TokenEmailService tokensEmail;
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
        contas.excluirConta(aluno.getId(), tokensEmail.criarExclusao(aluno));
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
        contas.excluirConta(professor.getId(), tokensEmail.criarExclusao(professor));
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

    @ParameterizedTest
    @ValueSource(strings = {
            "insert into tb_modulos (titulo, descricao, curso_id, ordem, criado_em) values ('Teste', 'Teste', -1, 1, CURRENT_TIMESTAMP)",
            "insert into tb_aulas (titulo, descricao, modulo_id, ordem, youtube_video_id, criado_em) values ('Teste', 'Teste', -1, 1, 'M7lc1UVf-VE', CURRENT_TIMESTAMP)",
            "insert into tb_matriculas (aluno_id, curso_id, matriculado_em) values (-1, -1, CURRENT_TIMESTAMP)",
            "insert into tb_progresso_aulas (matricula_id, aula_id, posicao_segundos, atualizado_em) values (-1, -1, 0, CURRENT_TIMESTAMP)" })
    void bancoRejeitaVinculosInexistentes(String sql) {
        assertThatThrownBy(() -> jdbc.update(sql)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void bancoRejeitaOrdemZeroMesmoSemValidacaoHttp() {
        var professor = usuarios.findByEmail("professor@aulas.example.com").orElseThrow();
        var curso = cursos.criar(new CursoRequest("Java", "Curso"), professor.getId());
        assertThatThrownBy(() -> jdbc.update(
                "insert into tb_modulos (titulo, descricao, curso_id, ordem, criado_em) values ('Teste', 'Teste', ?, 0, CURRENT_TIMESTAMP)",
                curso.getId())).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void bancoRejeitaProgressoDuplicado() {
        var professor = usuarios.findByEmail("professor@aulas.example.com").orElseThrow();
        var curso = cursos.criar(new CursoRequest("Java", "Curso"), professor.getId());
        var modulo = modulos.criar(curso.getId(), new ModuloRequest("Inicio", "Basico", 1), professor.getId());
        var aula = aulas.criar(modulo.getId(), new AulaRequest("Java", "Basico", 1,
                "https://youtu.be/M7lc1UVf-VE"), professor.getId());
        var aluno = criarProfessor("aluno-progresso");
        aluno.setPerfil(Perfil.ALUNO);
        usuarios.saveAndFlush(aluno);
        var matricula = alunoAulas.matricular(curso.getId(), aluno.getId());
        progressos.concluir(aula.getId(), aluno.getId());
        assertThatThrownBy(() -> jdbc.update(
                "insert into tb_progresso_aulas (matricula_id, aula_id, atualizado_em) values (?, ?, CURRENT_TIMESTAMP)",
                matricula.getId(), aula.getId())).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void primeiroAdministradorNaoRecriaDepoisDeExclusaoFisica() {
        ReflectionTestUtils.setField(administradorInicial, "email", "primeiro@example.com");
        ReflectionTestUtils.setField(administradorInicial, "senha", "SenhaAdminInicial123!");
        try {
            administradorInicial.criar();
            var administrador = usuarios.findByEmail("primeiro@example.com").orElseThrow();
            var registro = inicializacao.findById(1L).orElseThrow();
            assertThat(registro.getIdUsuario()).isEqualTo(administrador.getId());
            usuarios.delete(administrador);
            usuarios.flush();
            entidades.clear();
            var aposExcluir = inicializacao.findById(1L).orElseThrow();
            assertThat(aposExcluir.isConcluida()).isTrue();
            assertThat(aposExcluir.getIdUsuario()).isNull();
            ReflectionTestUtils.setField(administradorInicial, "email", "novo@example.com");
            administradorInicial.criar();
            assertThat(usuarios.findByEmail("primeiro@example.com")).isEmpty();
            assertThat(usuarios.findByEmail("novo@example.com")).isEmpty();
        } finally {
            ReflectionTestUtils.setField(administradorInicial, "email", "");
            ReflectionTestUtils.setField(administradorInicial, "senha", "");
        }
    }

    @Test
    void v1CriaSchemaCompletoEDocumentosSeparados() {
        String schema = "v1_" + java.util.UUID.randomUUID().toString().replace("-", "");
        var flyway = Flyway.configure().dataSource(fonte).schemas(schema).defaultSchema(schema).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(flyway.info().current().getVersion().toString()).isEqualTo("1");
        assertThat(jdbc.queryForObject("select count(*) from " + schema + ".tb_politicas", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from " + schema + ".tb_politicas_privacidade", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select nome from " + schema + ".tb_politicas_privacidade", String.class)).isEqualTo("Politica de Privacidade");
        assertThat(jdbc.queryForObject("select concluida from " + schema + ".tb_inicializacao_administrador", Boolean.class)).isFalse();
        assertThat(jdbc.queryForObject("select count(*) from " + schema + ".tb_contas_excluidas", Long.class)).isZero();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
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
