package com.cursovalido.backend.forum;

import com.cursovalido.backend.authentication.security.AnonimizacaoDados;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

import com.cursovalido.backend.authentication.dto.*;
import com.cursovalido.backend.authentication.entity.*;
import com.cursovalido.backend.authentication.repository.*;
import com.cursovalido.backend.authentication.service.EmailService;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import com.cursovalido.backend.authentication.validation.email.ValidacaoEmailService;
import com.cursovalido.backend.forum.dto.*;
import com.cursovalido.backend.forum.entity.UsuarioForum;
import com.cursovalido.backend.forum.repository.*;
import com.cursovalido.backend.forum.support.PostgresTeste;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.RestClient;

/**
 * HTTP real, seguranca e persistencia reais. Apenas o envio externo de email e
 * mockado.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
                "seguranca.limite-requisicoes=10000",
                "seguranca.limite-autenticacao=10000"
})
@ActiveProfiles("test")
@ExtendWith(PostgresTeste.CicloDeVida.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ForumHttpIntegracaoTest {
        private static final AtomicLong SEQUENCIA = new AtomicLong(100_000_000L);
        private static final String SENHA = "SenhaTeste123!";
        @LocalServerPort
        private int porta;
        @Autowired
        private JdbcTemplate jdbc;
        @Autowired
        private com.cursovalido.backend.authentication.service.CadastroUsuarioService cadastroUsuarios;
        @Autowired
        private TopicoRepository topicos;
        @Autowired
        private ComentarioRepository comentarios;
        @Autowired
        private UsuarioForumRepository usuariosForum;
        @Autowired
        private UsuarioRepository usuarios;
        @Autowired
        private PoliticaRepository politicas;
        @Autowired
        private InicializacaoAdministradorRepository inicializacao;
        @Autowired
        private ConviteAcessoRepository convites;
        @Autowired
        private com.cursovalido.backend.authentication.service.TokenEmailService tokensEmail;
        @MockitoBean
        private EmailService emails;
        @MockitoSpyBean
        private LogAuditoriaService auditoria;
        @MockitoBean
        private ValidacaoEmailService validacaoEmail;
        private final Map<String, String> codigos = new ConcurrentHashMap<>();
        private final Map<String, String> convitesEnviados = new ConcurrentHashMap<>();
        @Autowired
        private com.cursovalido.backend.authentication.service.ResponsabilidadeService responsabilidade;
        private RestClient cliente;
        private Sessao autor;

        @DynamicPropertySource
        static void banco(DynamicPropertyRegistry registry) {
                PostgresTeste.configurar(registry, "forum_api");
        }

        @BeforeEach
        void prepararClienteEDadosIsolados() {
                cliente = RestClient.builder().baseUrl("http://127.0.0.1:" + porta)
                                .defaultStatusHandler(status -> true, (request, response) -> {
                                }).build();
                // Limpa exclusivamente o schema de teste; nunca usa o banco de desenvolvimento.
                var tabelas = jdbc.queryForList("select tablename from pg_tables where schemaname = current_schema()",
                                String.class);
                if (!tabelas.isEmpty())
                        jdbc.execute("TRUNCATE TABLE " + String.join(",", tabelas.stream()
                                        .map(nome -> "\"" + nome.replace("\"", "\"\"") + "\"").toList())
                                        + " RESTART IDENTITY CASCADE");
                codigos.clear();
                convitesEnviados.clear();
                doAnswer(invocacao -> {
                        ConviteAcesso convite = invocacao.getArgument(0);
                        convitesEnviados.put(convite.getEmail(), invocacao.getArgument(1));
                        return null;
                }).when(emails).enviarConvite(any(), anyString());
                doAnswer(invocacao -> {
                        Usuario usuario = invocacao.getArgument(0);
                        codigos.put(usuario.getEmail(), invocacao.getArgument(1));
                        return null;
                }).when(emails).enviarCodigo2fa(any(), anyString());
                politicas.save(new Politica("Termos", "teste-v1", "Termos de teste"));
                jdbc.update("INSERT INTO tb_versoes_documentos (tipo, numero) VALUES ('TERMOS', 0), ('PRIVACIDADE', 0)");
                autor = cadastrarEAutenticar(Perfil.ALUNO);
        }

        @Test
        void publicarIgnoraVersaoDoCliente() {
                var admin = cadastrarEAutenticar(Perfil.ADMINISTRADOR, "principal-versoes@example.com");
                var resposta = enviar(HttpMethod.POST, "/api/politicas", admin.token(),
                                Map.of("nome", "Termos", "conteudo", "Texto novo", "versao", "999.0"),
                                PoliticaResponse.class);
                assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(resposta.getBody().versao()).isEqualTo("v1.0");
                assertThat(politicas.findByVersao("999.0")).isEmpty();
                assertThat(politicas.findByAtivaTrueOrderByDataPublicacaoDesc()).extracting(Politica::getVersao)
                                .containsExactly("v1.0");
        }

        @Test
        void publicarPrivacidadeGeraVersaoSemEntradaManual() {
                var admin = cadastrarEAutenticar(Perfil.ADMINISTRADOR, "principal-privacidade@example.com");
                for (int numero = 1; numero <= 2; numero++) {
                        var resposta = enviar(HttpMethod.POST, "/api/politicas/privacidade", admin.token(),
                                        new PoliticaRequest("Privacidade", "Texto " + numero), PoliticaResponse.class);
                        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                        assertThat(resposta.getBody().versao()).isEqualTo(numero + ".0");
                }
        }

        @Test
        void exclusaoAdministrativaRejeitaAutoexclusaoAlunoEAdministradoresComuns() {
                var principal = cadastrarEAutenticar(Perfil.ADMINISTRADOR, "principal-exclusao@example.com");
                var comum = cadastrarEAutenticar(Perfil.ADMINISTRADOR);
                var professor = cadastrarEAutenticar(Perfil.PROFESSOR);
                String rota = "/api/administracao/usuarios/";
                assertThat(enviar(HttpMethod.DELETE, rota + principal.id(), principal.token(), null, String.class)
                                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(enviar(HttpMethod.DELETE, rota + principal.id(), comum.token(), null, String.class)
                                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(enviar(HttpMethod.DELETE, rota + autor.id(), principal.token(), null, String.class)
                                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
                assertThat(enviar(HttpMethod.DELETE, rota + comum.id(), professor.token(), null, String.class)
                                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(enviar(HttpMethod.DELETE, rota + professor.id(), comum.token(), null, String.class)
                                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
                assertThat(enviar(HttpMethod.DELETE, rota + comum.id(), null, null, String.class)
                                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(usuarios.findById(principal.id()).orElseThrow().getExcluidoEm()).isNull();
                assertThat(usuarios.findById(professor.id()).orElseThrow().getExcluidoEm()).isNotNull();
                assertThat(enviar(HttpMethod.DELETE, rota + Long.MAX_VALUE, principal.token(), null, String.class)
                                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @ParameterizedTest
        @ValueSource(strings = { "PROFESSOR", "ADMINISTRADOR" })
        void exclusaoAdministrativaDesidentificaEquipeRevogaAcessoEPreservaHistorico(String perfil) {
                var principal = cadastrarEAutenticar(Perfil.ADMINISTRADOR, "principal-exclusao@example.com");
                var alvo = cadastrarEAutenticar(Perfil.valueOf(perfil));
                var original = usuarios.findById(alvo.id()).orElseThrow();
                String email = original.getEmail();
                var topico = enviar(HttpMethod.POST, "/api/forum/topicos", alvo.token(),
                                new TopicoRequest("Historico da equipe", "Conteudo educacional preservado"),
                                TopicoResponse.class).getBody();
                tokensEmail.criarExclusao(original);
                long aceitesAntes = jdbc.queryForObject(
                                "select count(*) from tb_aceites_responsabilidade where user_id=?",
                                Long.class, alvo.id());
                assertThat(aceitesAntes).isPositive();
                String rota = "/api/administracao/usuarios/" + alvo.id();
                assertThat(enviar(HttpMethod.DELETE, rota, principal.token(), null, String.class).getStatusCode())
                                .isEqualTo(HttpStatus.NO_CONTENT);
                var excluido = usuarios.findById(alvo.id()).orElseThrow();
                assertThat(excluido.getExcluidoEm()).isNotNull();
                assertThat(excluido.isAtivo()).isFalse();
                assertThat(excluido.getNomeCompleto()).isEqualTo("Usuario excluido");
                assertThat(excluido.getEmail()).isEqualTo("excluido-" + alvo.id() + "@conta.invalid");
                assertThat(excluido.getSenha()).isEmpty();
                assertThat(excluido.getMatricula()).isEqualTo("excluido-" + alvo.id());
                assertThat(usuarios.findByEmail(email)).isEmpty();
                assertThat(jdbc.queryForObject("select count(*) from tb_sessoes where usuario_id=?", Long.class,
                                alvo.id())).isZero();
                assertThat(jdbc.queryForObject("select count(*) from tb_tokens_email where usuario_id=?", Long.class,
                                alvo.id())).isZero();
                assertThat(jdbc.queryForObject("select count(*) from tb_aceites_responsabilidade where user_id=?",
                                Long.class, alvo.id()))
                                .isEqualTo(aceitesAntes);
                assertThat(jdbc.queryForObject(
                                "select count(*) from tb_aceites_termos_arquivados where usuario_id_original=?",
                                Long.class, alvo.id())).isPositive();
                assertThat(jdbc.queryForObject("select count(*) from tb_logs_auditoria where usuario_id=? "
                                + "and tipo_acao='EXCLUSAO_ADMINISTRATIVA_CONTA'", Long.class, principal.id()))
                                .isEqualTo(1);
                var leitura = enviar(HttpMethod.GET, "/api/forum/topicos", principal.token(), null,
                                TopicoResumoDTO[].class);
                assertThat(leitura.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(leitura.getBody()).filteredOn(t -> t.id().equals(topico.id())).singleElement()
                                .satisfies(t -> {
                                        assertThat(t.descricao()).isEqualTo("Conteudo educacional preservado");
                                        assertThat(t.nomeAutor()).isEqualTo("Usuario excluido");
                                        assertThat(t.idAutor()).isEqualTo(alvo.id());
                                });
                assertThat(enviar(HttpMethod.GET, "/api/autenticacao/meus-dados", alvo.token(), null, String.class)
                                .getStatusCode())
                                .isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(enviar(HttpMethod.GET, "/api/administracao/usuarios", principal.token(), null, String.class)
                                .getBody())
                                .doesNotContain(email, "excluido-" + alvo.id() + "@conta.invalid");
                // Repetir a requisicao nao cria novo registro de exclusao.
                assertThat(enviar(HttpMethod.DELETE, rota, principal.token(), null, String.class).getStatusCode())
                                .isEqualTo(HttpStatus.NO_CONTENT);
                assertThat(jdbc.queryForObject("select count(*) from tb_logs_auditoria where usuario_id=? "
                                + "and tipo_acao='EXCLUSAO_ADMINISTRATIVA_CONTA'", Long.class, principal.id()))
                                .isEqualTo(1);
        }

        @Test
        void administradorComumRestringeProfessorEConsultaPorSituacaoSemPoderExcluirEquipe() {
                var principal = cadastrarEAutenticar(Perfil.ADMINISTRADOR, "principal-restricoes@example.com");
                var comum = cadastrarEAutenticar(Perfil.ADMINISTRADOR);
                var outroAdmin = cadastrarEAutenticar(Perfil.ADMINISTRADOR);
                var professor = cadastrarEAutenticar(Perfil.PROFESSOR);
                String usuariosUrl = "/api/administracao/usuarios";
                String restricoesUrl = usuariosUrl + "/" + professor.id() + "/suspensoes";
                var dados = new SuspensaoRequest(EscopoSuspensao.FORUM, "Publicações repetidas", null);
                var lista = UsuarioAdministracaoResposta[].class;

                assertThat(enviar(HttpMethod.GET, usuariosUrl, comum.token(), null, lista).getBody())
                                .filteredOn(u -> u.id().equals(professor.id())).singleElement().satisfies(u -> {
                                        assertThat(u.podeModerar()).isTrue();
                                        assertThat(u.podeExcluir()).isTrue();
                                });
                for (var admin : new Sessao[] { principal, outroAdmin, comum }) {
                        assertThat(enviar(HttpMethod.POST, usuariosUrl + "/" + admin.id() + "/suspensoes",
                                        comum.token(), dados, String.class).getStatusCode())
                                        .isEqualTo(HttpStatus.FORBIDDEN);
                }
                assertThat(enviar(HttpMethod.DELETE, usuariosUrl + "/" + outroAdmin.id(), comum.token(), null,
                                String.class)
                                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

                var aplicacao = enviar(HttpMethod.POST, restricoesUrl, comum.token(), dados, SuspensaoResponse.class);
                assertThat(aplicacao.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(enviar(HttpMethod.GET, usuariosUrl + "?restrito=true", comum.token(), null, lista).getBody())
                                .extracting(u -> u.id()).containsExactly(professor.id());
                assertThat(enviar(HttpMethod.GET, usuariosUrl + "?restrito=false", comum.token(), null, lista)
                                .getBody())
                                .extracting(u -> u.id()).contains(outroAdmin.id()).doesNotContain(professor.id());

                assertThat(enviar(HttpMethod.DELETE, restricoesUrl + "/" + aplicacao.getBody().id(),
                                comum.token(), null, String.class).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
                assertThat(enviar(HttpMethod.GET, usuariosUrl + "?restrito=true", comum.token(), null, lista).getBody())
                                .isEmpty();
        }

        @Test
        void listagemDeModeracaoExibeNomeCompletoEGamertagSemEmail() {
                var principal = cadastrarEAutenticar(Perfil.ADMINISTRADOR, "principal-listagem@example.com");
                var comum = cadastrarEAutenticar(Perfil.ADMINISTRADOR);
                var professor = cadastrarEAutenticar(Perfil.PROFESSOR);
                String emailAluno = usuarios.findById(autor.id()).orElseThrow().getEmail();
                String emailProfessor = usuarios.findById(professor.id()).orElseThrow().getEmail();
                String emailComum = usuarios.findById(comum.id()).orElseThrow().getEmail();
                var resposta = enviar(HttpMethod.GET, "/api/administracao/usuarios", principal.token(), null,
                                UsuarioAdministracaoResposta[].class);
                assertThat(resposta.getBody()).filteredOn(u -> u.id().equals(autor.id())).singleElement()
                                .satisfies(u -> {
                                        var usuario = usuarios.findById(autor.id()).orElseThrow();
                                        assertThat(u.nome()).isEqualTo(usuario.getNomeCompleto());
                                        assertThat(u.gamertag()).isEqualTo(usuario.getGamertag());
                                        assertThat(u.podeExcluir()).isTrue();
                                });
                assertThat(resposta.getBody()).filteredOn(u -> u.id().equals(professor.id())).singleElement()
                                .satisfies(u -> {
                                        var usuario = usuarios.findById(professor.id()).orElseThrow();
                                        assertThat(u.nome()).isEqualTo(usuario.getNomeCompleto());
                                        assertThat(u.gamertag()).isEqualTo(usuario.getGamertag());
                                        assertThat(u.podeExcluir()).isTrue();
                                });
                assertThat(resposta.getBody()).filteredOn(u -> u.id().equals(comum.id())).singleElement()
                                .satisfies(u -> {
                                        var usuario = usuarios.findById(comum.id()).orElseThrow();
                                        assertThat(u.nome()).isEqualTo(usuario.getNomeCompleto());
                                        assertThat(u.gamertag()).isEqualTo(usuario.getGamertag());
                                        assertThat(u.podeExcluir()).isTrue();
                                });
                assertThat(resposta.getBody()).filteredOn(u -> u.id().equals(principal.id())).singleElement()
                                .satisfies(u -> assertThat(u.podeExcluir()).isFalse());
                assertThat(enviar(HttpMethod.GET, "/api/administracao/usuarios", comum.token(), null,
                                UsuarioAdministracaoResposta[].class).getBody())
                                .allSatisfy(u -> assertThat(u.podeExcluir())
                                                .isEqualTo(u.perfil() != Perfil.ADMINISTRADOR));
                for (String token : new String[] { principal.token(), comum.token() }) {
                        assertThat(enviar(HttpMethod.GET, "/api/forum/users", token, null, String.class).getBody())
                                        .doesNotContain(emailAluno, emailProfessor, emailComum);
                }
                for (String token : new String[] { autor.token(), professor.token() }) {
                        assertThat(enviar(HttpMethod.GET, "/api/forum/users", token, null, String.class).getBody())
                                        .doesNotContain(emailAluno, emailProfessor, emailComum,
                                                        "principal-listagem@example.com");
                }
                assertThat(enviar(HttpMethod.GET, "/api/autenticacao/meus-dados", autor.token(), null, String.class)
                                .getBody())
                                .contains(emailAluno);
        }

        @Test
        void errosDeAutenticacaoEPermissaoNaoExibemCaminhosOuTokens() {
                for (String token : new String[] { null, "token-invalido-privado" }) {
                        var resposta = enviar(HttpMethod.GET, "/api/administracao/usuarios", token, null, String.class);
                        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                        assertThat(resposta.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON))
                                        .isTrue();
                        assertThat(resposta.getHeaders().getCacheControl()).contains("no-store");
                        assertThat(resposta.getBody()).contains("AUTENTICACAO_NECESSARIA")
                                        .doesNotContain("/api/", "token-invalido", "timestamp", "exception", "trace");
                }
                var negado = enviar(HttpMethod.GET, "/api/administracao/usuarios", autor.token(), null, String.class);
                assertThat(negado.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(negado.getBody()).contains("ACESSO_NEGADO").doesNotContain("/api/", "ROLE_", autor.token());
        }

        @Test
        void corsPermiteSomenteOrigemECabecalhosConfigurados() {
                var permitido = cliente.options().uri("/api/administracao/usuarios")
                                .header("Origin", "http://localhost:3000")
                                .header("Access-Control-Request-Method", "GET")
                                .header("Access-Control-Request-Headers", "authorization,content-type")
                                .retrieve().toEntity(String.class);
                assertThat(permitido.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(permitido.getHeaders().getFirst("Access-Control-Allow-Origin"))
                                .isEqualTo("http://localhost:3000");
                assertThat(permitido.getHeaders().getFirst("Access-Control-Allow-Credentials")).isNull();
                var bloqueado = cliente.options().uri("/api/administracao/usuarios")
                                .header("Origin", "https://nao-autorizado.example")
                                .header("Access-Control-Request-Method", "GET")
                                .header("Access-Control-Request-Headers", "authorization")
                                .retrieve().toEntity(String.class);
                assertThat(bloqueado.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(bloqueado.getHeaders().getFirst("Access-Control-Allow-Origin")).isNull();
                var cabecalhoInvalido = cliente.options().uri("/api/administracao/usuarios")
                                .header("Origin", "http://localhost:3000")
                                .header("Access-Control-Request-Method", "GET")
                                .header("Access-Control-Request-Headers", "x-user-id")
                                .retrieve().toEntity(String.class);
                assertThat(cabecalhoInvalido.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(cabecalhoInvalido.getHeaders().getFirst("Access-Control-Allow-Headers")).isNull();
        }

        @Test
        void visitanteNaoPodeLerNemAlterarConteudo() {
                var criado = enviar(HttpMethod.POST, "/api/forum/topicos", autor.token(),
                                new TopicoRequest("Leitura aberta", "Descricao publica"), TopicoResponse.class)
                                .getBody();
                String caminho = "/api/forum/topicos/" + criado.id();
                assertThat(enviar(HttpMethod.GET, "/api/forum/topicos", null, null, String.class).getStatusCode())
                                .isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(enviar(HttpMethod.GET, "/api/forum/topicos/meus", null, null, String.class).getStatusCode())
                                .isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(enviar(HttpMethod.GET, caminho + "/comentarios", null, null, String.class).getStatusCode())
                                .isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(enviar(HttpMethod.GET, "/api/forum/users", null, null, String.class).getStatusCode())
                                .isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(enviar(HttpMethod.POST, "/api/forum/topicos", null, new TopicoRequest("Teste", "Descricao"),
                                String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(enviar(HttpMethod.POST, caminho + "/comentarios", null, Map.of("conteudo", "Resposta"),
                                String.class)
                                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(enviar(HttpMethod.PUT, caminho, null, new TopicoRequest("Teste", "Descricao"), String.class)
                                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(enviar(HttpMethod.DELETE, caminho, null, null, String.class).getStatusCode())
                                .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void criarTopico_comDadosValidos_deveRetornarAutorIdEPersistir() {
                var resposta = enviar(HttpMethod.POST, "/api/forum/topicos", autor.token(),
                                new TopicoRequest("Java", "Duvida"),
                                TopicoResponse.class);
                assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
                var corpo = resposta.getBody();
                assertThat(corpo).isNotNull();
                assertThat(corpo.id()).isPositive();
                assertThat(corpo.titulo()).isEqualTo("Java");
                assertThat(corpo.descricao()).isEqualTo("Duvida");
                assertThat(corpo.idAutor()).isEqualTo(autor.id());
                assertThat(corpo.nomeAutor()).isEqualTo(autor.nome());
                assertThat(corpo.papelAutor()).isEqualTo("ALUNO");
                assertThat(corpo.criadoEm()).isNotNull();
                assertThat(corpo.quantidadeRespostas()).isZero();
                assertThat(corpo.ativo()).isTrue();
                assertThat(corpo.fechado()).isFalse();
                assertThat(topicos.findById(corpo.id()).orElseThrow().getDescricao()).isEqualTo("Duvida");
        }

        @Test
        void criarTopico_comFalhaNaAuditoria_deveReverterCriacao() {
                doThrow(new IllegalStateException("Falha na auditoria")).when(auditoria)
                                .registrar(eq(autor.id()), eq("CADASTRO_TOPICO"), anyString(), isNull());

                var resposta = enviar(HttpMethod.POST, "/api/forum/topicos", autor.token(),
                                new TopicoRequest("Topico sem auditoria", "Descricao"), String.class);

                assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                assertThat(topicos.findAll()).noneMatch(topico -> topico.getTitulo().equals("Topico sem auditoria"));
        }

        @Test
        void listarTopicos_comBuscaEPaginacao_deveFiltrarOrdenarEContarRespostasAtivas() {
                var primeiro = criarTopico("Java inicial");
                criarComentario(primeiro.id(), "Resposta");
                var apagado = criarComentario(primeiro.id(), "Apagado");
                excluirComentario(apagado.id(), autor.token());
                var segundo = criarTopico("JAVA avancado");
                var arquivado = criarTopico("Java arquivado");
                alterarEstado(arquivado.id(), "arquivar", autor.token());
                criarTopico("Outro assunto");
                var resposta = enviar(HttpMethod.GET, "/api/forum/topicos?busca=java&page=0&size=1", autor.token(),
                                null,
                                TopicoResumoDTO[].class);
                assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(resposta.getHeaders().getFirst("X-Frame-Options")).isEqualTo("DENY");
                assertThat(resposta.getBody()).hasSize(1);
                assertThat(resposta.getBody()[0].id()).isEqualTo(segundo.id());
                var pagina = enviar(HttpMethod.GET, "/api/forum/topicos?busca=java&page=1&size=1", autor.token(), null,
                                TopicoResumoDTO[].class);
                assertThat(pagina.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(pagina.getBody()).hasSize(1);
                assertThat(pagina.getBody()[0].id()).isEqualTo(primeiro.id());
                assertThat(pagina.getBody()[0].quantidadeRespostas()).isEqualTo(1);
        }

        @Test
        void editarTopico_comAutor_deveRetornarEPersistirNovosDados() {
                var topico = criarTopico("Original");
                var resposta = enviar(HttpMethod.PUT, "/api/forum/topicos/" + topico.id(), autor.token(),
                                new TopicoRequest("Atualizado", "Nova descricao"), TopicoResponse.class);
                assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(resposta.getBody().titulo()).isEqualTo("Atualizado");
                assertThat(resposta.getBody().descricao()).isEqualTo("Nova descricao");
                var salvo = topicos.findById(topico.id()).orElseThrow();
                assertThat(salvo.getTitulo()).isEqualTo("Atualizado");
                assertThat(salvo.getIdAutor()).isEqualTo(autor.id());
        }

        @Test
        void arquivarTopico_comAutor_deveDesativarEOcultarNaListagem() {
                var topico = criarTopico("Arquivar");
                alterarEstado(topico.id(), "arquivar", autor.token());
                assertThat(topicos.findById(topico.id()).orElseThrow().isAtivo()).isFalse();
                var lista = enviar(HttpMethod.GET, "/api/forum/topicos", autor.token(), null, TopicoResumoDTO[].class);
                assertThat(lista.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(lista.getBody()).isEmpty();
                var meus = enviar(HttpMethod.GET, "/api/forum/topicos/meus", autor.token(), null, String.class);
                assertThat(meus.getBody()).doesNotContain("Arquivar");
                var reabertura = enviar(HttpMethod.PUT, "/api/forum/topicos/" + topico.id() + "/desarquivar",
                                autor.token(), null, String.class);
                assertThat(reabertura.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(topicos.findById(topico.id()).orElseThrow().isAtivo()).isFalse();
        }

        @Test
        void fecharTopico_comAutor_devePersistirFechamento() {
                var topico = criarTopico("Fechar");
                alterarEstado(topico.id(), "fechar", autor.token());
                assertThat(topicos.findById(topico.id()).orElseThrow().isFechado()).isTrue();
        }

        @Test
        void rotaDeExclusaoDeTopicoDeveSerNegadaAteParaAdministrador() {
                var topico = criarTopico("Excluir");
                var admin = cadastrarEAutenticar(Perfil.ADMINISTRADOR);
                var resposta = enviar(HttpMethod.DELETE, "/api/forum/topicos/" + topico.id(), admin.token(), null,
                                String.class);
                assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(topicos.findById(topico.id()).orElseThrow().isAtivo()).isTrue();
        }

        @Test
        void apagarTopico_semToken_deveRetornar401EPreservarRegistro() {
                var topico = criarTopico("Protegido");
                var resposta = enviar(HttpMethod.DELETE, "/api/forum/topicos/" + topico.id(), null, null,
                                ErroHttp.class);
                assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(topicos.findById(topico.id()).orElseThrow().isAtivo()).isTrue();
        }

        @Test
        void apagarTopico_comAluno_deveRetornar403EPreservarRegistro() {
                var topico = criarTopico("Protegido");
                verificarErro(
                                enviar(HttpMethod.DELETE, "/api/forum/topicos/" + topico.id(), autor.token(), null,
                                                ErroHttp.class),
                                403);
                assertThat(topicos.findById(topico.id()).orElseThrow().isAtivo()).isTrue();
        }

        @Test
        void criarComentario_comTopicoAberto_devePersistirRelacionamento() {
                var topico = criarTopico("Relacionamento");
                var resposta = enviar(HttpMethod.POST, comentariosUrl(topico.id()), autor.token(),
                                new ComentarioRequest("Minha resposta"), ComentarioResponse.class);
                assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
                var corpo = resposta.getBody();
                assertThat(corpo.id()).isPositive();
                assertThat(corpo.conteudo()).isEqualTo("Minha resposta");
                assertThat(corpo.idAutor()).isEqualTo(autor.id());
                assertThat(corpo.nomeAutor()).isEqualTo(autor.nome());
                assertThat(corpo.papelAutor()).isEqualTo("ALUNO");
                assertThat(corpo.criadoEm()).isNotNull();
                assertThat(corpo.ativo()).isTrue();
                assertThat(comentarios.findById(corpo.id()).orElseThrow().getTopico().getId()).isEqualTo(topico.id());
        }

        @Test
        void listarComentarios_comDoisTopicos_deveManterExcluidosSemExporConteudo() {
                var topico = criarTopico("Principal");
                var outro = criarTopico("Outro");
                var primeiro = criarComentario(topico.id(), "Primeiro");
                var segundo = criarComentario(topico.id(), "Segundo");
                var apagado = criarComentario(topico.id(), "Apagado");
                criarComentario(outro.id(), "Fora do topico");
                excluirComentario(apagado.id(), autor.token());
                var resposta = enviar(HttpMethod.GET, comentariosUrl(topico.id()), autor.token(), null,
                                ComentarioResponse[].class);
                assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(resposta.getBody()).extracting(ComentarioResponse::id).containsExactly(primeiro.id(),
                                segundo.id(), apagado.id());
                assertThat(resposta.getBody()).extracting(ComentarioResponse::conteudo).containsExactly("Primeiro",
                                "Segundo", "Esta mensagem foi excluída.");
                assertThat(resposta.getBody()[2].ativo()).isFalse();
        }

        @Test
        void editarComentario_comAutor_deveRetornarEPersistirConteudo() {
                var comentario = criarComentario(criarTopico("Editar coment.").id(), "Original");
                var resposta = enviar(HttpMethod.PUT, "/api/forum/comentarios/" + comentario.id(), autor.token(),
                                new ComentarioRequest("Atualizado"), ComentarioResponse.class);
                assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(resposta.getBody().conteudo()).isEqualTo("Atualizado");
                assertThat(comentarios.findById(comentario.id()).orElseThrow().getConteudo()).isEqualTo("Atualizado");
        }

        @Test
        void apagarComentario_comAutor_deveExcluirLogicamente() {
                var comentario = criarComentario(criarTopico("Excluir coment").id(), "Resposta");
                excluirComentario(comentario.id(), autor.token());
                assertThat(comentarios.findById(comentario.id()).orElseThrow().isAtivo()).isFalse();
        }

        @Test
        void apagarComentarioDoTopico_comAutor_deveDesativarComentarioRelacionado() {
                var topico = criarTopico("Excluir rel.");
                var comentario = criarComentario(topico.id(), "Resposta");
                var resposta = enviar(HttpMethod.DELETE, comentariosUrl(topico.id()) + "/" + comentario.id(),
                                autor.token(),
                                null, String.class);
                assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(resposta.getBody()).isNullOrEmpty();
                assertThat(comentarios.findById(comentario.id()).orElseThrow().isAtivo()).isFalse();
        }

        @Test
        void listarUsuarios_comAtivosEInativos_deveRetornarSomenteAtivosComContagem() {
                var inativo = new UsuarioForum(999999L, "Inativo", "inativo@example.com", "ALUNO");
                inativo.setAtivo(false);
                usuariosForum.save(inativo);
                var resposta = enviar(HttpMethod.GET, "/api/forum/users", autor.token(), null,
                                UsuariosForumResponse.class);
                assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(resposta.getBody().count()).isEqualTo(1);
                assertThat(resposta.getBody().value()).singleElement().satisfies(usuario -> {
                        assertThat(usuario.id()).isEqualTo(autor.id());
                        assertThat(usuario.nome()).isEqualTo(autor.nome());
                        assertThat(usuario.perfil()).isEqualTo("ALUNO");
                });
        }

        @Test
        void criarTopico_comCamposVazios_deveRetornar400SemPersistir() {
                verificarErro(
                                enviar(HttpMethod.POST, "/api/forum/topicos", autor.token(), new TopicoRequest("", ""),
                                                ErroHttp.class),
                                400);
                assertThat(topicos.count()).isZero();
        }

        @Test
        void paginacaoPadraoDoForumEMeusTopicosExibeDezPorPagina() {
                for (int i = 0; i < 11; i++)
                        criarTopico("Topico " + i);
                assertThat(enviar(HttpMethod.GET, "/api/forum/topicos", autor.token(), null, TopicoResumoDTO[].class)
                                .getBody())
                                .hasSize(10);
                assertThat(enviar(HttpMethod.GET, "/api/forum/topicos?page=1", autor.token(), null,
                                TopicoResumoDTO[].class).getBody())
                                .hasSize(1);
                var respostaMeus = enviar(HttpMethod.GET, "/api/forum/topicos/meus", autor.token(), null, Map.class);
                assertThat(respostaMeus.getStatusCode()).as("Resposta: %s", respostaMeus.getBody())
                                .isEqualTo(HttpStatus.OK);
                var meus = respostaMeus.getBody();
                assertThat(meus).containsKey("content");
                assertThat((java.util.List<?>) meus.get("content")).hasSize(10);
                assertThat(meus.get("totalPages")).isEqualTo(2);
        }

        @Test
        void buscasDoForumNaoRetornam500ComPaginacao() {
                for (int i = 0; i < 7; i++) {
                        criarTopico("Java " + i);
                        criarTopico("aaaaaaaaaaaaaaaa " + i);
                }
                for (String busca : new String[] { "java", "aaaaaaaaaaaaaaaa", "sem-resultados" }) {
                        int encontrados = busca.equals("sem-resultados") ? 0 : 5;
                        String parametros = "?page=0&size=5&busca=" + busca;
                        var publicos = enviar(HttpMethod.GET, "/api/forum/topicos" + parametros, autor.token(), null,
                                        TopicoResumoDTO[].class);
                        assertThat(publicos.getStatusCode()).isEqualTo(HttpStatus.OK);
                        assertThat(publicos.getBody()).hasSize(encontrados);
                        var meus = enviar(HttpMethod.GET, "/api/forum/topicos/meus" + parametros, autor.token(), null,
                                        Map.class);
                        assertThat(meus.getStatusCode()).as("Busca: %s; resposta: %s", busca, meus.getBody())
                                        .isEqualTo(HttpStatus.OK);
                        assertThat((java.util.List<?>) meus.getBody().get("content")).hasSize(encontrados);
                }
                // A consulta autenticada pode registrar auditoria no PostgreSQL.
                assertThat(jdbc.queryForObject("select count(*) from tb_logs_auditoria where usuario_id=? "
                                + "and tipo_acao='CONSULTA_MEUS_TOPICOS'", Long.class, autor.id())).isEqualTo(3);
        }

        @Test
        void tituloTopico_deveAceitar80ERecusar81NaCriacaoEEdicao() {
                var topico = criarTopico("t".repeat(80));
                verificarErro(enviar(HttpMethod.POST, "/api/forum/topicos", autor.token(),
                                new TopicoRequest("t".repeat(81), "Descricao"), ErroHttp.class), 400);
                var atualizado = enviar(HttpMethod.PUT, "/api/forum/topicos/" + topico.id(), autor.token(),
                                new TopicoRequest("e".repeat(80), "Descricao"), TopicoResponse.class);
                assertThat(atualizado.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(atualizado.getBody().titulo()).isEqualTo("e".repeat(80));
                verificarErro(enviar(HttpMethod.PUT, "/api/forum/topicos/" + topico.id(), autor.token(),
                                new TopicoRequest("e".repeat(81), "Descricao"), ErroHttp.class), 400);
                assertThat(topicos.findById(topico.id()).orElseThrow().getTitulo()).isEqualTo("e".repeat(80));
        }

        @Test
        void criarTopico_comDescricaoLonga_deveRetornar400SemPersistir() {
                verificarErro(enviar(HttpMethod.POST, "/api/forum/topicos", autor.token(),
                                new TopicoRequest("Java", "a".repeat(5001)), ErroHttp.class), 400);
                assertThat(topicos.count()).isZero();
        }

        @Test
        void criarComentario_comConteudoVazio_deveRetornar400SemPersistir() {
                var topico = criarTopico("Validacao");
                verificarErro(enviar(HttpMethod.POST, comentariosUrl(topico.id()), autor.token(),
                                new ComentarioRequest(" "),
                                ErroHttp.class), 400);
                assertThat(comentarios.count()).isZero();
        }

        @Test
        void criarComentario_comConteudoLongo_deveRetornar400SemPersistir() {
                var topico = criarTopico("Validacao");
                verificarErro(enviar(HttpMethod.POST, comentariosUrl(topico.id()), autor.token(),
                                new ComentarioRequest("a".repeat(5001)), ErroHttp.class), 400);
                assertThat(comentarios.count()).isZero();
        }

        @Test
        void criarComentario_comTopicoFechado_deveRetornar409SemPersistir() {
                var topico = criarTopico("Fechado");
                alterarEstado(topico.id(), "fechar", autor.token());
                verificarErro(enviar(HttpMethod.POST, comentariosUrl(topico.id()), autor.token(),
                                new ComentarioRequest("Resposta"), ErroHttp.class), 409);
                assertThat(comentarios.count()).isZero();
        }

        @Test
        void listarComentarios_comTopicoInexistente_deveRetornar404() {
                verificarErro(enviar(HttpMethod.GET, comentariosUrl(999999L), autor.token(), null, ErroHttp.class),
                                404);
        }

        @Test
        void listarComentarios_comTopicoArquivado_deveRetornar404() {
                var topico = criarTopico("Arquivado");
                alterarEstado(topico.id(), "arquivar", autor.token());
                verificarErro(enviar(HttpMethod.GET, comentariosUrl(topico.id()), autor.token(), null, ErroHttp.class),
                                404);
        }

        @Test
        void editarTopico_comOutroUsuario_deveRetornar403SemAlterar() {
                var topico = criarTopico("Original");
                var outro = cadastrarEAutenticar(Perfil.ALUNO);
                verificarErro(enviar(HttpMethod.PUT, "/api/forum/topicos/" + topico.id(), outro.token(),
                                new TopicoRequest("Indevido", "Descricao"), ErroHttp.class), 403);
                assertThat(topicos.findById(topico.id()).orElseThrow().getTitulo()).isEqualTo("Original");
        }

        @ParameterizedTest
        @ValueSource(strings = { "arquivar", "fechar" })
        void alterarEstado_comOutroUsuario_deveRetornar403SemAlterar(String acao) {
                var topico = criarTopico("Original");
                var outro = cadastrarEAutenticar(Perfil.ALUNO);
                verificarErro(enviar(HttpMethod.PUT, "/api/forum/topicos/" + topico.id() + "/" + acao, outro.token(),
                                null,
                                ErroHttp.class), 403);
                var salvo = topicos.findById(topico.id()).orElseThrow();
                assertThat(salvo.isAtivo()).isTrue();
                assertThat(salvo.isFechado()).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = { "arquivar", "fechar" })
        void alterarEstado_comAdministrador_devePermitirOutroAutor(String acao) {
                var topico = criarTopico("Administracao");
                var admin = cadastrarEAutenticar(Perfil.ADMINISTRADOR);
                alterarEstado(topico.id(), acao, admin.token());
                var salvo = topicos.findById(topico.id()).orElseThrow();
                if (acao.equals("arquivar"))
                        assertThat(salvo.isAtivo()).isFalse();
                else
                        assertThat(salvo.isFechado()).isTrue();
        }

        @Test
        void editarComentario_comOutroUsuario_deveRetornar403SemAlterar() {
                var comentario = criarComentario(criarTopico("Autoria").id(), "Original");
                var outro = cadastrarEAutenticar(Perfil.ALUNO);
                verificarErro(enviar(HttpMethod.PUT, "/api/forum/comentarios/" + comentario.id(), outro.token(),
                                new ComentarioRequest("Indevido"), ErroHttp.class), 403);
                assertThat(comentarios.findById(comentario.id()).orElseThrow().getConteudo()).isEqualTo("Original");
        }

        @Test
        void editarComentario_comComentarioApagado_deveRetornar400() {
                var comentario = criarComentario(criarTopico("Apagado").id(), "Original");
                excluirComentario(comentario.id(), autor.token());
                verificarErro(enviar(HttpMethod.PUT, "/api/forum/comentarios/" + comentario.id(), autor.token(),
                                new ComentarioRequest("Indevido"), ErroHttp.class), 400);
                assertThat(comentarios.findById(comentario.id()).orElseThrow().getConteudo()).isEqualTo("Original");
        }

        @Test
        void apagarComentario_comOutroUsuario_deveRetornar403SemDesativar() {
                var comentario = criarComentario(criarTopico("Autoria").id(), "Original");
                var outro = cadastrarEAutenticar(Perfil.ALUNO);
                verificarErro(enviar(HttpMethod.DELETE, "/api/forum/comentarios/" + comentario.id(), outro.token(),
                                null,
                                ErroHttp.class), 403);
                assertThat(comentarios.findById(comentario.id()).orElseThrow().isAtivo()).isTrue();
        }

        @Test
        void apagarComentario_comAdministrador_devePermitirOutroAutor() {
                var comentario = criarComentario(criarTopico("Administracao").id(), "Original");
                var admin = cadastrarEAutenticar(Perfil.ADMINISTRADOR);
                excluirComentario(comentario.id(), admin.token());
                assertThat(comentarios.findById(comentario.id()).orElseThrow().isAtivo()).isFalse();
        }

        @Test
        void apagarComentarioDoTopico_comOutroTopico_deveRetornar404SemDesativar() {
                var comentario = criarComentario(criarTopico("Principal").id(), "Resposta");
                var outro = criarTopico("Outro");
                verificarErro(enviar(HttpMethod.DELETE, comentariosUrl(outro.id()) + "/" + comentario.id(),
                                autor.token(), null,
                                ErroHttp.class), 404);
                assertThat(comentarios.findById(comentario.id()).orElseThrow().isAtivo()).isTrue();
        }

        @Test
        void editarComentario_comIdInexistente_deveRetornar404() {
                verificarErro(enviar(HttpMethod.PUT, "/api/forum/comentarios/999999", autor.token(),
                                new ComentarioRequest("Resposta"), ErroHttp.class), 404);
        }

        @Test
        void editarTopico_comIdInexistente_deveRetornar404() {
                verificarErro(enviar(HttpMethod.PUT, "/api/forum/topicos/999999", autor.token(),
                                new TopicoRequest("Java", "Descricao"), ErroHttp.class), 404);
        }

        @Test
        void editarTopico_comAdministradorQueNaoEAutor_deveRetornar403() {
                var topico = criarTopico("Original");
                var admin = cadastrarEAutenticar(Perfil.ADMINISTRADOR);
                verificarErro(enviar(HttpMethod.PUT, "/api/forum/topicos/" + topico.id(), admin.token(),
                                new TopicoRequest("Indevido", "Descricao"), ErroHttp.class), 403);
                assertThat(topicos.findById(topico.id()).orElseThrow().getTitulo()).isEqualTo("Original");
        }

        @Test
        void criarTopico_comUsuarioForumInativo_deveRetornar404SemPersistir() {
                var usuario = usuariosForum.findById(autor.id()).orElseThrow();
                usuario.setAtivo(false);
                usuariosForum.saveAndFlush(usuario);
                verificarErro(enviar(HttpMethod.POST, "/api/forum/topicos", autor.token(),
                                new TopicoRequest("Java", "Descricao"), ErroHttp.class), 404);
                assertThat(topicos.count()).isZero();
        }

        @Test
        void criarComentario_comLimiteDeConteudo_deveAceitar5000Caracteres() {
                var topico = criarTopico("Limite");
                var resposta = criarComentario(topico.id(), "a".repeat(5000));
                assertThat(resposta.conteudo()).hasSize(5000);
                assertThat(comentarios.findById(resposta.id()).orElseThrow().getConteudo()).hasSize(5000);
        }

        @Test
        void criarTopico_comLimiteDeDescricao_deveAceitar5000Caracteres() {
                var resposta = enviar(HttpMethod.POST, "/api/forum/topicos", autor.token(),
                                new TopicoRequest("Limite", "a".repeat(5000)), TopicoResponse.class);
                assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(resposta.getBody().descricao()).hasSize(5000);
                assertThat(topicos.findById(resposta.getBody().id()).orElseThrow().getDescricao()).hasSize(5000);
        }

        @Test
        void editarComentario_comAdministradorQueNaoEAutor_deveRetornar403() {
                var comentario = criarComentario(criarTopico("Autoria").id(), "Original");
                var admin = cadastrarEAutenticar(Perfil.ADMINISTRADOR);
                verificarErro(enviar(HttpMethod.PUT, "/api/forum/comentarios/" + comentario.id(), admin.token(),
                                new ComentarioRequest("Indevido"), ErroHttp.class), 403);
                assertThat(comentarios.findById(comentario.id()).orElseThrow().getConteudo()).isEqualTo("Original");
        }

        @Test
        void apagarComentario_comIdInexistente_deveRetornar404() {
                verificarErro(enviar(HttpMethod.DELETE, "/api/forum/comentarios/999999", autor.token(), null,
                                ErroHttp.class),
                                404);
        }

        @ParameterizedTest
        @ValueSource(strings = { "arquivar", "fechar" })
        void alterarEstado_comIdInexistente_deveRetornar404(String acao) {
                verificarErro(enviar(HttpMethod.PUT, "/api/forum/topicos/999999/" + acao, autor.token(), null,
                                ErroHttp.class),
                                404);
        }

        @Test
        void criarTopico_comProfessor_devePermitirPublicacao() {
                var professor = cadastrarEAutenticar(Perfil.PROFESSOR);
                var resposta = enviar(HttpMethod.POST, "/api/forum/topicos", professor.token(),
                                new TopicoRequest("Aula", "Discussao"), TopicoResponse.class);
                assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(resposta.getBody().papelAutor()).isEqualTo("PROFESSOR");
                assertThat(topicos.findById(resposta.getBody().id()).orElseThrow().getIdAutor())
                                .isEqualTo(professor.id());
        }

        // Segundo controller da atividade, usado no cadastro de participantes do forum.
        @Test
        void listarPoliticas_comVersaoAtiva_deveRetornarCorpoPublicamente() {
                var resposta = enviar(HttpMethod.GET, "/api/politicas/ativas", null, null, PoliticaResponse[].class);
                assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(resposta.getBody()).singleElement().satisfies(politica -> {
                        assertThat(politica.versao()).isEqualTo("teste-v1");
                        assertThat(politica.conteudo()).isEqualTo("Termos de teste");
                        assertThat(politica.dataPublicacao()).isNotNull();
                });
        }

        @Test
        void publicarPolitica_comAdministradorPrincipal_deveRetornar201EPersistirNovaVersao() {
                var admin = cadastrarEAutenticar(Perfil.ADMINISTRADOR, "principal@example.com");
                var resposta = enviar(HttpMethod.POST, "/api/politicas", admin.token(),
                                new PoliticaRequest("Novos termos", "Conteudo novo"), PoliticaResponse.class);
                assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(resposta.getBody().versao()).isEqualTo("v1.0");
                assertThat(resposta.getBody().nome()).isEqualTo("Novos termos");
                assertThat(resposta.getBody().conteudo()).isEqualTo("Conteudo novo");
                assertThat(politicas.findByVersao("v1.0")).isPresent();
                assertThat(politicas.findByAtivaTrueOrderByDataPublicacaoDesc()).extracting(Politica::getVersao)
                                .containsExactly("v1.0");
                assertThat(usuarios.findById(autor.id()).orElseThrow().getAceitouTermosLgpd()).isTrue();
                assertThat(usuarios.findById(autor.id()).orElseThrow().getVersaoTermos()).isEqualTo("teste-v1");
        }

        @Test
        void publicarPolitica_comAluno_deveRetornar403SemPersistir() {
                verificarErro(enviar(HttpMethod.POST, "/api/politicas", autor.token(),
                                new PoliticaRequest("Termos", "Conteudo"), ErroHttp.class), 403);
                assertThat(politicas.findByVersao("v1.0")).isEmpty();
        }

        @Test
        void excluirProfessorAutenticadoPermiteNovoConviteSemReutilizarSessaoAntiga() {
                var principal = cadastrarEAutenticar(Perfil.ADMINISTRADOR, "principal-convite@example.com");
                String email = "professor-reconvite@example.com";
                long idAnterior = cadastrarConvidado(email, principal.token());
                String linkAnterior = convitesEnviados.get(email);
                String sessaoAnterior = autenticarConvidado(email);
                String codigoExclusao = tokensEmail.criarExclusao(usuarios.findById(idAnterior).orElseThrow());
                assertThat(enviar(HttpMethod.DELETE, "/api/autenticacao/conta", sessaoAnterior,
                                Map.of("codigo", codigoExclusao), String.class).getStatusCode())
                                .isEqualTo(HttpStatus.NO_CONTENT);
                assertThat(usuarios.findByEmail(email)).isEmpty();
                assertThat(convites.findByEmail(email)).isPresent();
                assertThat(enviar(HttpMethod.GET, "/api/autenticacao/meus-dados", sessaoAnterior, null, String.class)
                                .getStatusCode())
                                .isEqualTo(HttpStatus.UNAUTHORIZED);
                long idNovo = cadastrarConvidado(email, principal.token());
                assertThat(idNovo).isNotEqualTo(idAnterior);
                assertThat(enviar(HttpMethod.POST, "/api/convites/consultar", null, Map.of("token", linkAnterior),
                                String.class).getStatusCode())
                                .isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(enviar(HttpMethod.GET, "/api/autenticacao/meus-dados", sessaoAnterior, null, String.class)
                                .getStatusCode())
                                .isEqualTo(HttpStatus.UNAUTHORIZED);
                String sessaoNova = autenticarConvidado(email);
                assertThat(
                                enviar(HttpMethod.GET, "/api/autenticacao/meus-dados", sessaoNova, null, String.class)
                                                .getStatusCode())
                                .isEqualTo(HttpStatus.OK);
        }

        @Test
        void segundoConviteParaEmailComConvitePendenteRetornaConflitoSemNovoEmail() {
                var principal = cadastrarEAutenticar(Perfil.ADMINISTRADOR, "principal-convite@example.com");
                String email = "sem-conta@example.com";
                assertThat(enviar(HttpMethod.POST, "/api/administracao/convites", principal.token(),
                                new ConviteAcessoRequest(email, Perfil.PROFESSOR), String.class).getStatusCode())
                                .isEqualTo(HttpStatus.NO_CONTENT);
                String tokenAnterior = convitesEnviados.get(email);
                var resposta = enviar(HttpMethod.POST, "/api/administracao/convites", principal.token(),
                                new ConviteAcessoRequest(email, Perfil.ADMINISTRADOR), Map.class);
                assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                assertThat(resposta.getBody()).containsEntry("codigo", "CONVITE_JA_PENDENTE");
                assertThat(convites.count()).isEqualTo(1);
                assertThat(convitesEnviados.get(email)).isEqualTo(tokenAnterior);
                verify(emails, times(1)).enviarConvite(any(), anyString());
        }

        @Test
        void novoConviteAposConviteConcluidoSemContaGeraNovoLinkEInvalidaOAnterior() {
                var principal = cadastrarEAutenticar(Perfil.ADMINISTRADOR, "principal-convite@example.com");
                String email = "sem-conta@example.com";
                assertThat(enviar(HttpMethod.POST, "/api/administracao/convites", principal.token(),
                                new ConviteAcessoRequest(email, Perfil.PROFESSOR), String.class).getStatusCode())
                                .isEqualTo(HttpStatus.NO_CONTENT);
                String tokenAnterior = convitesEnviados.get(email);
                var conviteAnterior = convites.findByEmail(email).orElseThrow();
                Long idConvite = conviteAnterior.getId();
                conviteAnterior.concluir();
                convites.saveAndFlush(conviteAnterior);
                assertThat(enviar(HttpMethod.POST, "/api/administracao/convites", principal.token(),
                                new ConviteAcessoRequest(email, Perfil.ADMINISTRADOR), String.class).getStatusCode())
                                .isEqualTo(HttpStatus.NO_CONTENT);
                var novo = convites.findByEmail(email).orElseThrow();
                assertThat(novo.getId()).isNotEqualTo(idConvite);
                assertThat(novo.getPerfil()).isEqualTo(Perfil.ADMINISTRADOR);
                assertThat(novo.getStatus()).isEqualTo(StatusConvite.PENDENTE);
                assertThat(novo.isEmailConfirmado()).isFalse();
                assertThat(convites.count()).isEqualTo(2);
                assertThat(convitesEnviados.get(email)).isNotEqualTo(tokenAnterior);
                assertThat(enviar(HttpMethod.POST, "/api/convites/consultar", null,
                                Map.of("token", tokenAnterior),
                                String.class).getStatusCode())
                                .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void contaAindaExistenteRetornaConflitoExplicitoSemEnviarConvite() {
                var principal = cadastrarEAutenticar(Perfil.ADMINISTRADOR, "principal-convite@example.com");
                var professor = cadastrarEAutenticar(Perfil.PROFESSOR);
                var existente = usuarios.findById(professor.id()).orElseThrow();
                for (boolean ativo : new boolean[] { true, false }) {
                        existente.setAtivo(ativo);
                        usuarios.saveAndFlush(existente);
                        var resposta = enviar(HttpMethod.POST, "/api/administracao/convites", principal.token(),
                                        new ConviteAcessoRequest(existente.getEmail(), Perfil.PROFESSOR), Map.class);
                        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(resposta.getBody()).containsEntry("codigo", "EMAIL_JA_CADASTRADO");
                        assertThat(resposta.getBody().get("erro").toString()).contains("conta cadastrada");
                }
                assertThat(convitesEnviados).doesNotContainKey(existente.getEmail());
        }

        private long cadastrarConvidado(String email, String tokenPrincipal) {
                assertThat(enviar(HttpMethod.POST, "/api/administracao/convites", tokenPrincipal,
                                new ConviteAcessoRequest(email, Perfil.PROFESSOR), String.class).getStatusCode())
                                .isEqualTo(HttpStatus.NO_CONTENT);
                String token = convitesEnviados.get(email);
                var dadosConvite = enviar(HttpMethod.POST, "/api/convites/consultar", null,
                                Map.of("token", token), ConviteDadosResponse.class);
                assertThat(dadosConvite.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(dadosConvite.getBody().email()).isEqualTo(email);
                assertThat(dadosConvite.getBody().perfil()).isEqualTo(Perfil.PROFESSOR);
                var cadastro = new CadastroUsuarioRequest("Professor Convidado", email, SENHA, SENHA,
                                true, true, "teste-v1", true, "1.0");
                assertThat(enviar(HttpMethod.POST, "/api/convites/completar-cadastro", null,
                                Map.of("token", token, "cadastro", cadastro), String.class).getStatusCode())
                                .isEqualTo(HttpStatus.NO_CONTENT);
                // O cadastro pelo link ja libera o acesso, sem aprovacao manual.
                var usuario = usuarios.findByEmail(email).orElseThrow();
                assertThat(usuario.isAtivo()).isTrue();
                return usuario.getId();
        }

        private String autenticarConvidado(String email) {
                var login = enviar(HttpMethod.POST, "/api/autenticacao/login", null,
                                Map.of("email", email, "senha", SENHA),
                                AutenticacaoResponse.class);
                assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
                var confirmado = enviar(HttpMethod.POST, "/api/autenticacao/confirmar-2fa", null,
                                Map.of("email", email, "codigo", codigos.get(email)), AutenticacaoResponse.class);
                assertThat(confirmado.getStatusCode()).isEqualTo(HttpStatus.OK);
                return confirmado.getBody().token();
        }

        @Test
        void cadastroNaoEnviaCodigoELoginNaoDuplicaCodigoPendente() {
                for (int i = 1; i <= 3; i++) {
                        String email = "cadastro-" + i + "@example.com";
                        var cadastro = new CadastroUsuarioRequest("  João 12! da Silva ", email, SENHA, SENHA,
                                        true, true, "teste-v1", null, null);
                        var resposta = enviar(HttpMethod.POST, "/api/autenticacao/cadastro", null, cadastro,
                                        String.class);
                        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                }
                verify(emails, never()).enviarCodigo2fa(argThat(u -> u.getEmail().startsWith("cadastro-")),
                                anyString());
                String email = "cadastro-1@example.com";
                var usuario = usuarios.findByEmail(email).orElseThrow();
                assertThat(usuario.getNomeCompleto()).isEqualTo("João da Silva");
                var login = Map.of("email", email, "senha", SENHA);
                for (int i = 0; i < 2; i++)
                        assertThat(enviar(HttpMethod.POST, "/api/autenticacao/login", null, login,
                                        AutenticacaoResponse.class)
                                        .getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(enviar(HttpMethod.POST, "/api/autenticacao/reenviar-2fa", null, Map.of("email", email),
                                String.class)
                                .getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                verify(emails, times(1)).enviarCodigo2fa(argThat(u -> email.equals(u.getEmail())), anyString());
                assertThat(usuarios.findByEmail("cadastro-3@example.com")).isPresent();
        }

        private Sessao cadastrarEAutenticar(Perfil perfil) {
                return cadastrarEAutenticar(perfil, null);
        }

        private Sessao cadastrarEAutenticar(Perfil perfil, String emailPrincipal) {
                long numero = SEQUENCIA.incrementAndGet();
                String email = emailPrincipal == null ? "forum-" + numero + "@example.com" : emailPrincipal;
                String nome = "Participante " + Character.toString((int) ('A' + numero % 26));
                var requisicao = new CadastroUsuarioRequest(nome, email, SENHA, SENHA,
                                true, true, "teste-v1", null, null);
                cadastroUsuarios.cadastrar(requisicao, "2001:db8::" + Long.toHexString(numero));
                // Bootstrap do administrador: o cadastro publico cria somente ALUNO.
                if (perfil != Perfil.ALUNO) {
                        var usuario = usuarios.findByEmail(email).orElseThrow();
                        usuario.setPerfil(perfil);
                        usuarios.saveAndFlush(usuario);
                        responsabilidade.registrar(usuario, true, "1.0", "127.0.0.1");
                        if (emailPrincipal != null) {
                                var registro = new InicializacaoAdministrador();
                                registro.concluir(usuario.getId());
                                inicializacao.saveAndFlush(registro);
                        }
                }
                var login = enviar(HttpMethod.POST, "/api/autenticacao/login", null,
                                Map.of("email", email, "senha", SENHA), AutenticacaoResponse.class);
                assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(codigos.get(email)).isNotBlank();
                var confirmacao = enviar(HttpMethod.POST, "/api/autenticacao/confirmar-2fa", null,
                                Map.of("email", email, "codigo", codigos.get(email)), AutenticacaoResponse.class);
                assertThat(confirmacao.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(confirmacao.getBody().token()).isNotBlank();
                // No forum, alunos aparecem com o sobrenome abreviado.
                String nomeExibido = perfil == Perfil.ALUNO ? AnonimizacaoDados.abreviarNome(nome) : nome;
                return new Sessao(confirmacao.getBody().idUsuario(), nomeExibido, confirmacao.getBody().token());
        }

        @Test
        void topicosExigemTresMinutosInclusiveAposArquivar() {
                var dados = new TopicoRequest("Primeiro", "Descricao");
                var primeiro = enviar(HttpMethod.POST, "/api/forum/topicos", autor.token(), dados, TopicoResponse.class);
                assertThat(primeiro.getStatusCode()).isEqualTo(HttpStatus.OK);
                var bloqueado = enviar(HttpMethod.POST, "/api/forum/topicos", autor.token(), dados, String.class);
                assertThat(bloqueado.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                assertThat(bloqueado.getBody()).contains("3 minutos");
                alterarEstado(primeiro.getBody().id(), "arquivar", autor.token());
                assertThat(enviar(HttpMethod.POST, "/api/forum/topicos", autor.token(), dados, String.class).getStatusCode())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                jdbc.update("UPDATE tb_topics SET created_at = CURRENT_TIMESTAMP - INTERVAL '3 minutes'");
                assertThat(enviar(HttpMethod.POST, "/api/forum/topicos", autor.token(), dados, TopicoResponse.class).getStatusCode())
                        .isEqualTo(HttpStatus.OK);
        }

        @Test
        void comentariosExigemVinteSegundosEntreTopicosDiferentes() {
                var primeiro = criarTopico("Primeiro");
                var segundo = criarTopico("Segundo");
                var dados = new ComentarioRequest("Resposta");
                assertThat(enviar(HttpMethod.POST, comentariosUrl(primeiro.id()), autor.token(), dados, ComentarioResponse.class).getStatusCode())
                        .isEqualTo(HttpStatus.OK);
                var bloqueado = enviar(HttpMethod.POST, comentariosUrl(segundo.id()), autor.token(), dados, String.class);
                assertThat(bloqueado.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                assertThat(bloqueado.getBody()).contains("20 segundos");
                jdbc.update("UPDATE tb_comments SET created_at = CURRENT_TIMESTAMP - INTERVAL '20 seconds'");
                assertThat(enviar(HttpMethod.POST, comentariosUrl(segundo.id()), autor.token(), dados, ComentarioResponse.class).getStatusCode())
                        .isEqualTo(HttpStatus.OK);
        }

        @Test
        void apiSalvaEDevolveOfensasOcultas() {
                var resposta = enviar(HttpMethod.POST, "/api/forum/topicos", autor.token(),
                        new TopicoRequest("idiota", "lixo, fodace, foda-se, pau no cu, otario"), TopicoResponse.class);
                assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(resposta.getBody().titulo()).isEqualTo("***");
                assertThat(topicos.findById(resposta.getBody().id()).orElseThrow().getDescricao())
                        .isEqualTo("***, ***, ***, ***, ***");
                var comentario = enviar(HttpMethod.POST, comentariosUrl(resposta.getBody().id()), autor.token(),
                        new ComentarioRequest("OTÁRIO!"), ComentarioResponse.class);
                assertThat(comentario.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(comentario.getBody().conteudo()).isEqualTo("***!");
        }

        private TopicoResponse criarTopico(String titulo) {
                var resposta = enviar(HttpMethod.POST, "/api/forum/topicos", autor.token(),
                                new TopicoRequest(titulo, "Descricao " + titulo), TopicoResponse.class);
                assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(resposta.getBody().id()).isPositive();
                // Libera o limite de criação entre tópicos dentro dos cenários de paginação.
                jdbc.update("UPDATE tb_topics SET created_at = CURRENT_TIMESTAMP - INTERVAL '3 minutes' WHERE id = ?",
                                resposta.getBody().id());
                return resposta.getBody();
        }

        private ComentarioResponse criarComentario(Long id, String conteudo) {
                var resposta = enviar(HttpMethod.POST, comentariosUrl(id), autor.token(),
                                new ComentarioRequest(conteudo),
                                ComentarioResponse.class);
                assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(resposta.getBody().conteudo()).isEqualTo(conteudo);
                // Fixtures de listagem não exercitam o intervalo entre publicações.
                jdbc.update("UPDATE tb_comments SET created_at = CURRENT_TIMESTAMP - INTERVAL '20 seconds' WHERE id = ?",
                                resposta.getBody().id());
                return resposta.getBody();
        }

        private void alterarEstado(Long id, String acao, String token) {
                var resposta = enviar(HttpMethod.PUT, "/api/forum/topicos/" + id + "/" + acao, token, null,
                                String.class);
                assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(resposta.getBody()).isNullOrEmpty();
        }

        private void excluirComentario(Long id, String token) {
                var resposta = enviar(HttpMethod.DELETE, "/api/forum/comentarios/" + id, token, null, String.class);
                assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(resposta.getBody()).isNullOrEmpty();
        }

        private String comentariosUrl(Long id) {
                return "/api/forum/topicos/" + id + "/comentarios";
        }

        private <T> ResponseEntity<T> enviar(HttpMethod metodo, String url, String token, Object corpo, Class<T> tipo) {
                var request = cliente.method(metodo).uri(url).accept(MediaType.APPLICATION_JSON);
                if (token != null)
                        request.headers(headers -> headers.setBearerAuth(token));
                if (corpo != null)
                        request.contentType(MediaType.APPLICATION_JSON).body(corpo);
                return request.retrieve().toEntity(tipo);
        }

        private void verificarErro(ResponseEntity<ErroHttp> resposta, int status) {
                assertThat(resposta.getStatusCode().value()).isEqualTo(status);
                assertThat(resposta.getBody()).isNotNull();
                if (resposta.getBody().codigo() != null) {
                        assertThat(resposta.getBody().erro()).isNotBlank();
                        return;
                }
                assertThat(resposta.getBody().status()).isEqualTo(status);
                assertThat(resposta.getBody().error()).isEqualTo(HttpStatus.valueOf(status).getReasonPhrase());
                assertThat(resposta.getBody().path()).startsWith("/api/");
        }

        @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
        private record ErroHttp(Integer status, String error, String path, String erro, String codigo) {
        }

        private record Sessao(Long id, String nome, String token) {
        }
}
