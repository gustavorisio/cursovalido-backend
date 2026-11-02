package com.cursovalido.backend.aulas;

import com.cursovalido.backend.aulas.dto.*;
import com.cursovalido.backend.aulas.repository.*;
import com.cursovalido.backend.aulas.service.YoutubeClient;
import com.cursovalido.backend.authentication.entity.*;
import com.cursovalido.backend.authentication.repository.UsuarioRepositorio;
import com.cursovalido.backend.authentication.service.TokenJwtServico;
import com.cursovalido.backend.forum.support.PostgresTeste;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

// Servidor HTTP, filtro JWT, transacoes e PostgreSQL reais; somente YouTube e simulado.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.jpa.hibernate.ddl-auto=validate")
@ActiveProfiles("test")
@ExtendWith(PostgresTeste.CicloDeVida.class)
@Import(AulasPostgresTest.MigracaoTeste.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AulasHttpIntegracaoTest {
    private static final AtomicLong SEQUENCIA = new AtomicLong(20_000_000_000L);
    @LocalServerPort private int porta;
    @Autowired private UsuarioRepositorio usuarios;
    @Autowired private TokenJwtServico tokens;
    @Autowired private MatriculaRepository matriculas;
    @Autowired private ProgressoAulaRepository progressos;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private YoutubeClient youtube;
    private RestClient cliente;
    private Usuario aluno;
    private String tokenAluno;
    private String tokenProfessor;
    private long idCurso;
    private long idModulo;
    private long idAula;

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registry) {
        PostgresTeste.configurar(registry, "aulas_http");
    }

    @BeforeEach
    void preparar() {
        var fabrica = new JdkClientHttpRequestFactory();
        fabrica.setReadTimeout(Duration.ofSeconds(10));
        cliente = RestClient.builder().baseUrl("http://127.0.0.1:" + porta).requestFactory(fabrica)
                .defaultStatusHandler(status -> true, (request, response) -> { }).build();
        tokenProfessor = tokens.gerar(criarUsuario(Perfil.PROFESSOR));
        aluno = criarUsuario(Perfil.ALUNO);
        tokenAluno = tokens.gerar(aluno);
        var curso = enviar(HttpMethod.POST, "/api/cursos", tokenProfessor,
                new CursoRequest("Java", "Curso HTTP"), CursoResponse.class);
        assertThat(curso.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        idCurso = curso.getBody().id();
        var modulo = enviar(HttpMethod.POST, "/api/cursos/" + idCurso + "/modulos", tokenProfessor,
                new ModuloRequest("Inicio", "Basico", 1), ModuloResponse.class);
        assertThat(modulo.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        idModulo = modulo.getBody().id();
        var aula = enviar(HttpMethod.POST, "/api/modulos/" + idModulo + "/aulas", tokenProfessor,
                new AulaRequest("Java", "Introducao", 1, "https://youtu.be/M7lc1UVf-VE"), AulaResponse.class);
        assertThat(aula.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        idAula = aula.getBody().id();
    }

    @Test
    void jwtRealLiberaMatriculaERetomadaAposNovoToken() {
        assertThat(enviar(HttpMethod.GET, "/api/aluno/aulas/" + idAula, tokenAluno, null, String.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        matricular();
        var salvo = enviar(HttpMethod.PUT, rotaProgresso(), tokenAluno,
                new ProgressoAulaRequest(145L), ProgressoAulaResponse.class);
        assertThat(salvo.getStatusCode()).isEqualTo(HttpStatus.OK);
        var consulta = enviar(HttpMethod.GET, rotaProgresso(), tokens.gerar(aluno), null, ProgressoAulaResponse.class);
        assertThat(consulta.getBody().posicaoSegundos()).isEqualTo(145);
        var conclusao = enviar(HttpMethod.POST, "/api/aluno/aulas/" + idAula + "/conclusao",
                tokenAluno, null, ProgressoAulaResponse.class);
        assertThat(conclusao.getBody().concluida()).isTrue();
        var matricula = matriculas.findByIdAlunoAndIdCurso(aluno.getId(), idCurso).orElseThrow();
        assertThat(progressos.findByIdMatriculaAndIdAula(matricula.getId(), idAula).orElseThrow().isConcluida()).isTrue();
    }

    @Test
    void duasMatriculasSimultaneasRetornamMesmoRegistro() throws Exception {
        var respostas = simultaneas(() -> enviar(HttpMethod.POST, rotaMatricula(), tokenAluno, null, MatriculaResponse.class));
        assertThat(respostas).allSatisfy(resposta -> assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK));
        assertThat(respostas.get(0).getBody().id()).isEqualTo(respostas.get(1).getBody().id());
        assertThat(matriculas.findByIdAlunoOrderByIdDesc(aluno.getId())).hasSize(1);
        assertThat(jdbc.queryForObject("select count(*) from tb_logs_auditoria where usuario_id = ? and tipo_acao = 'MATRICULA_CURSO'",
                Long.class, aluno.getId())).isEqualTo(1);
    }

    @Test
    void atualizacaoSimultaneaNaoDuplicaProgresso() throws Exception {
        matricular();
        var respostas = simultaneas(() -> enviar(HttpMethod.PUT, rotaProgresso(), tokenAluno,
                new ProgressoAulaRequest(100L), ProgressoAulaResponse.class));
        assertThat(respostas).allSatisfy(resposta -> assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK));
        assertThat(jdbc.queryForObject("select count(*) from tb_progresso_aulas where aula_id = ?", Long.class, idAula))
                .isEqualTo(1);
    }

    @Test
    void conclusaoSimultaneaNaoDuplicaAuditoria() throws Exception {
        matricular();
        var respostas = simultaneas(() -> enviar(HttpMethod.POST, "/api/aluno/aulas/" + idAula + "/conclusao",
                tokenAluno, null, ProgressoAulaResponse.class));
        assertThat(respostas).allSatisfy(resposta -> assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK));
        // PostgreSQL armazena microssegundos; a resposta inicial pode conter nanossegundos.
        assertThat(respostas.get(0).getBody().concluidaEm()).isCloseTo(respostas.get(1).getBody().concluidaEm(),
                within(1, java.time.temporal.ChronoUnit.MICROS));
        assertThat(jdbc.queryForObject("select count(*) from tb_logs_auditoria where usuario_id = ? and tipo_acao = 'CONCLUSAO_AULA'",
                Long.class, aluno.getId())).isEqualTo(1);
    }

    @Test
    void outroAlunoNaoLeNemAlteraProgressoMesmoMatriculado() {
        matricular();
        enviar(HttpMethod.PUT, rotaProgresso(), tokenAluno, new ProgressoAulaRequest(120L), ProgressoAulaResponse.class);
        String outroToken = tokens.gerar(criarUsuario(Perfil.ALUNO));
        assertThat(enviar(HttpMethod.GET, rotaProgresso(), outroToken, null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        enviar(HttpMethod.POST, rotaMatricula(), outroToken, null, MatriculaResponse.class);
        assertThat(enviar(HttpMethod.GET, rotaProgresso(), outroToken, null, ProgressoAulaResponse.class)
                .getBody().posicaoSegundos()).isZero();
        enviar(HttpMethod.PUT, rotaProgresso(), outroToken, new ProgressoAulaRequest(15L), ProgressoAulaResponse.class);
        assertThat(enviar(HttpMethod.GET, rotaProgresso(), tokenAluno, null, ProgressoAulaResponse.class)
                .getBody().posicaoSegundos()).isEqualTo(120);
    }

    @Test
    void falhaDoYoutubePreservaEdicaoEProgressoEmOutraTransacao() {
        matricular();
        enviar(HttpMethod.PUT, rotaProgresso(), tokenAluno, new ProgressoAulaRequest(120L), ProgressoAulaResponse.class);
        doThrow(new org.springframework.web.server.ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE))
                .when(youtube).validarVideo("abcdefghijk");
        var falha = enviar(HttpMethod.PUT, "/api/aulas/" + idAula, tokenProfessor,
                new AulaRequest("Nao salvar", "Novo", 2, "https://youtu.be/abcdefghijk"), String.class);
        assertThat(falha.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        var aula = enviar(HttpMethod.GET, "/api/aulas/" + idAula, tokenProfessor, null, AulaResponse.class).getBody();
        assertThat(aula.titulo()).isEqualTo("Java");
        assertThat(aula.youtubeVideoId()).isEqualTo("M7lc1UVf-VE");
        assertThat(enviar(HttpMethod.GET, rotaProgresso(), tokenAluno, null, ProgressoAulaResponse.class)
                .getBody().posicaoSegundos()).isEqualTo(120);
    }

    @Test
    void jwtDeContaInativadaPerdeAcessoImediatamente() {
        matricular();
        aluno.setAtivo(false);
        usuarios.saveAndFlush(aluno);
        assertThat(enviar(HttpMethod.GET, rotaProgresso(), tokenAluno, null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(enviar(HttpMethod.GET, "/api/aluno/cursos", null, null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private void matricular() {
        assertThat(enviar(HttpMethod.POST, rotaMatricula(), tokenAluno, null, MatriculaResponse.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    private String rotaMatricula() { return "/api/aluno/cursos/" + idCurso + "/matriculas"; }
    private String rotaProgresso() { return "/api/aluno/aulas/" + idAula + "/progresso"; }

    private <T> List<T> simultaneas(Supplier<T> chamada) throws Exception {
        CountDownLatch inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<T> tarefa = () -> {
                if (!inicio.await(5, TimeUnit.SECONDS)) { throw new IllegalStateException("Inicio nao liberado"); }
                return chamada.get();
            };
            Future<T> primeira = executor.submit(tarefa);
            Future<T> segunda = executor.submit(tarefa);
            inicio.countDown();
            return List.of(primeira.get(20, TimeUnit.SECONDS), segunda.get(20, TimeUnit.SECONDS));
        }
    }

    private <T> ResponseEntity<T> enviar(HttpMethod metodo, String rota, String token, Object corpo, Class<T> tipo) {
        var requisicao = cliente.method(metodo).uri(rota);
        if (token != null) { requisicao.headers(headers -> headers.setBearerAuth(token)); }
        if (corpo != null) { requisicao.contentType(MediaType.APPLICATION_JSON).body(corpo); }
        return requisicao.retrieve().toEntity(tipo);
    }

    private Usuario criarUsuario(Perfil perfil) {
        long numero = SEQUENCIA.incrementAndGet();
        Usuario usuario = new Usuario();
        usuario.setNomeCompleto("Teste HTTP " + numero);
        usuario.setEmail(numero + "@aulas.example.com");
        usuario.setSenha("hash-de-teste");
        usuario.setCpf(String.valueOf(numero));
        usuario.setMatricula("http-" + numero);
        usuario.setTelefone("11999999999");
        usuario.setEndereco(new Endereco("01001000", "Rua teste", "Centro", "Sao Paulo", "SP"));
        usuario.setDataNascimento(LocalDate.of(2000, 1, 1));
        usuario.setDeclarouMaiorIdade(true);
        usuario.setAceitouTermosLgpd(true);
        usuario.setDataHoraAceite(LocalDateTime.now());
        usuario.setVersaoTermos("teste-v1");
        usuario.setPerfil(perfil);
        usuario.setEmailConfirmado(true);
        return usuarios.saveAndFlush(usuario);
    }
}
