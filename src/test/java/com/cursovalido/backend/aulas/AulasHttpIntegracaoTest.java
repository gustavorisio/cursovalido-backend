package com.cursovalido.backend.aulas;

import com.cursovalido.backend.aulas.dto.*;
import com.cursovalido.backend.aulas.repository.*;
import com.cursovalido.backend.aulas.service.YoutubeClient;
import com.cursovalido.backend.authentication.entity.*;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.service.TokenJwtService;
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
    @Autowired private UsuarioRepository usuarios;
    @Autowired private TokenJwtService tokens;
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
    void administradorEditaArquivaEReabreSemPerderProgresso() {
        String tokenAdmin = tokens.gerar(criarUsuario(Perfil.ADMINISTRADOR));
        matricular();
        enviar(HttpMethod.PUT, rotaProgresso(), tokenAluno, new ProgressoAulaRequest(120L), ProgressoAulaResponse.class);
        String rota = "/api/administracao/aulas/" + idAula;
        var dados = new AulaRequest("Editada pelo administrador", "Descricao atualizada", 2, "https://youtu.be/M7lc1UVf-VE");
        assertThat(enviar(HttpMethod.PUT, rota, tokenAluno, dados, String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(enviar(HttpMethod.PUT, rota + "/arquivar", tokenProfessor, null, String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(enviar(HttpMethod.PUT, rota + "/reabrir", null, null, String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(enviar(HttpMethod.PUT, rota, tokenAdmin, dados, String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(enviar(HttpMethod.PUT, rota + "/arquivar", tokenAdmin, null, String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(enviar(HttpMethod.GET, "/api/aluno/aulas/" + idAula, tokenAluno, null, String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(enviar(HttpMethod.GET, "/api/administracao/modulos/" + idModulo + "/aulas", tokenAdmin, null, String.class).getBody()).contains("Editada pelo administrador").contains("false");
        assertThat(enviar(HttpMethod.PUT, rota + "/reabrir", tokenAdmin, null, String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(enviar(HttpMethod.GET, rotaProgresso(), tokenAluno, null, ProgressoAulaResponse.class).getBody().posicaoSegundos()).isEqualTo(120);
    }

    @Test
    void telefoneNaoPodeSerEditadoEDadosPessoaisSaoCompletos() {
        var resposta = enviar(HttpMethod.GET, "/api/autenticacao/meus-dados", tokenAluno, null, com.cursovalido.backend.authentication.dto.DadosPessoaisResponse.class);
        assertThat(resposta.getBody().nomeCompleto()).isEqualTo(aluno.getNomeCompleto());
        assertThat(resposta.getBody().email()).isEqualTo(aluno.getEmail());
        assertThat(resposta.getBody().telefone()).isEqualTo("11999999999");
        assertThat(resposta.getBody().dataNascimento()).isEqualTo(aluno.getDataNascimento());
        var contato = java.util.Map.of("telefone", "21988888888");
        assertThat(enviar(HttpMethod.PATCH, "/api/autenticacao/meus-dados", null, contato, String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(enviar(HttpMethod.PATCH, "/api/autenticacao/meus-dados", tokenAluno, contato, String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        var atualizado = usuarios.findById(aluno.getId()).orElseThrow();
        assertThat(atualizado.getTelefone()).isEqualTo("11999999999");
        assertThat(atualizado.getEmail()).isEqualTo(aluno.getEmail());
        assertThat(atualizado.getNomeCompleto()).isEqualTo(aluno.getNomeCompleto());
        var exportacao = enviar(HttpMethod.GET, "/api/autenticacao/exportar-dados", tokenAluno, null, String.class);
        assertThat(exportacao.getHeaders().getCacheControl()).contains("no-store");
        assertThat(exportacao.getBody()).contains("11999999999").doesNotContain("senha", "password", "token", "registrosAuditoria");
        assertThat(jdbc.queryForObject("select count(*) from tb_logs_auditoria where usuario_id = ? and tipo_acao = ?", Long.class, aluno.getId(), "EXPORTACAO_DADOS")).isGreaterThan(0);
    }

    @Test
    void segurancaRejeitaIdentidadeInventadaMesmoComTokenAssinado() {
        var identidade = new Usuario();
        org.springframework.test.util.ReflectionTestUtils.setField(identidade, "id", aluno.getId());
        identidade.setEmail("inexistente-" + System.nanoTime() + "@example.invalid");
        identidade.setPerfil(Perfil.ADMINISTRADOR);
        String token = tokens.gerar(identidade);
        assertThat(enviar(HttpMethod.GET, "/api/autenticacao/meus-dados", token, null, String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(enviar(HttpMethod.GET, "/api/administracao/usuarios", token, null, String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void segurancaRejeitaEmailDeOutraContaComIdValidoDiferente() {
        Usuario outra = criarUsuario(Perfil.ALUNO);
        var identidade = new Usuario();
        org.springframework.test.util.ReflectionTestUtils.setField(identidade, "id", aluno.getId());
        identidade.setEmail(outra.getEmail());
        identidade.setPerfil(Perfil.ALUNO);
        assertThat(enviar(HttpMethod.GET, "/api/autenticacao/meus-dados", tokens.gerar(identidade), null, String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void segurancaIgnoraPerfilAdministrativoInventadoNoTokenAssinado() {
        var identidade = new Usuario();
        org.springframework.test.util.ReflectionTestUtils.setField(identidade, "id", aluno.getId());
        identidade.setEmail(aluno.getEmail());
        identidade.setPerfil(Perfil.ADMINISTRADOR);
        assertThat(enviar(HttpMethod.GET, "/api/administracao/usuarios", tokens.gerar(identidade), null, String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void segurancaRejeitaTokenAdulteradoSemAssinaturaValida() {
        String[] partes = tokenAluno.split("[.]");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(partes[1]), java.nio.charset.StandardCharsets.UTF_8);
        payload = payload.replace("ALUNO", "ADMINISTRADOR");
        String adulterado = partes[0] + "." + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8)) + "." + partes[2];
        for (String token : List.of(adulterado, "token-inventado", "eyJhbGciOiJub25lIn0.e30.")) {
            assertThat(enviar(HttpMethod.GET, "/api/autenticacao/meus-dados", token, null, String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(enviar(HttpMethod.GET, "/api/administracao/usuarios", token, null, String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    void segurancaNaoAceitaIdForcadoPorCabecalhoOuParametro() {
        Usuario outra = criarUsuario(Perfil.ADMINISTRADOR);
        var semToken = cliente.get().uri("/api/autenticacao/meus-dados?idUsuario=" + outra.getId())
                .header("X-User-Id", outra.getId().toString()).retrieve().toEntity(String.class);
        assertThat(semToken.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        var comToken = cliente.get().uri("/api/autenticacao/meus-dados?idUsuario=" + outra.getId())
                .header("X-User-Id", outra.getId().toString()).headers(h -> h.setBearerAuth(tokenAluno))
                .retrieve().toEntity(com.cursovalido.backend.authentication.dto.DadosPessoaisResponse.class);
        assertThat(comToken.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(comToken.getBody().id()).isEqualTo(aluno.getId());
    }

    @Test
    void segurancaRejeitaSessaoRevogadaETokenLimitadoAosTermos() {
        String limitado = tokens.gerarTermos(aluno);
        assertThat(enviar(HttpMethod.GET, "/api/autenticacao/meus-dados", limitado, null, String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        tokens.encerrar(tokenAluno);
        assertThat(enviar(HttpMethod.GET, "/api/autenticacao/meus-dados", tokenAluno, null, String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void segurancaBloqueiaRotasPrivadasPorMetodoSemLogin() {
        for (String rota : List.of("/api/autenticacao/meus-dados", "/api/autenticacao/exportar-dados",
                "/api/administracao/usuarios", "/api/administracao/cursos", "/api/cursos", "/api/modulos/" + idModulo,
                "/api/aulas/" + idAula, "/api/aluno/matriculas", "/api/aluno/aulas/" + idAula + "/progresso")) {
            assertThat(enviar(HttpMethod.GET, rota, null, null, String.class).getStatusCode()).as(rota).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        for (HttpMethod metodo : List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)) {
            assertThat(enviar(metodo, "/api/cursos/" + idCurso, null, null, String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    void visitanteVeApenasCatalogoSemAcessoAoConteudoOuMatricula() {
        var catalogo = enviar(HttpMethod.GET, "/api/catalogo/cursos", null, null, String.class);
        assertThat(catalogo.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(catalogo.getBody()).contains("Java").doesNotContain("youtube", "modulos", "idProfessor");
        for (String caminho : List.of("/api/aluno/cursos/" + idCurso,
                "/api/aluno/cursos/" + idCurso + "/modulos", "/api/aluno/modulos/" + idModulo + "/aulas",
                "/api/aluno/aulas/" + idAula, "/api/aluno/matriculas")) {
            assertThat(enviar(HttpMethod.GET, caminho, null, null, String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        assertThat(enviar(HttpMethod.POST, "/api/aluno/cursos/" + idCurso + "/matriculas", null, null, String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
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
