package com.cursovalido.backend.forum;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;

import com.cursovalido.backend.authentication.dto.*;
import com.cursovalido.backend.authentication.entity.*;
import com.cursovalido.backend.authentication.repository.*;
import com.cursovalido.backend.authentication.service.EmailService;
import com.cursovalido.backend.authentication.service.ValidacaoEmailService;
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
import org.springframework.web.client.RestClient;

/**
 * HTTP real, seguranca e persistencia reais. Apenas o envio externo de email e
 * mockado.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "seguranca.limite-requisicoes=10000")
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
    @MockitoBean
    private ValidacaoEmailService validacaoEmail;
    private final Map<String, String> confirmacoes = new ConcurrentHashMap<>();
    private final Map<String, String> codigos = new ConcurrentHashMap<>();
    private final Map<String, String> convitesEnviados = new ConcurrentHashMap<>();
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
                    .map(nome -> "\"" + nome.replace("\"", "\"\"") + "\"").toList()) + " RESTART IDENTITY CASCADE");
        confirmacoes.clear();
        codigos.clear();
        convitesEnviados.clear();
        doAnswer(invocacao -> {
            ConviteAcesso convite = invocacao.getArgument(0);
            convitesEnviados.put(convite.getEmail(), invocacao.getArgument(1));
            return null;
        }).when(emails).enviarConvite(any(), anyString());
        doAnswer(invocacao -> {
            Usuario usuario = invocacao.getArgument(0);
            confirmacoes.put(usuario.getEmail(), invocacao.getArgument(1));
            return null;
        }).when(emails).enviarConfirmacao(any(), anyString());
        doAnswer(invocacao -> {
            Usuario usuario = invocacao.getArgument(0);
            codigos.put(usuario.getEmail(), invocacao.getArgument(1));
            return null;
        }).when(emails).enviarCodigo2fa(any(), anyString());
        politicas.save(new Politica("Termos", "teste-v1", "Termos de teste"));
        autor = cadastrarEAutenticar(Perfil.ALUNO);
    }

    @Test
    void visitantePodeLerMasNaoPodeAlterarConteudo() {
        var criado = enviar(HttpMethod.POST, "/api/forum/topicos", autor.token(),
                new TopicoRequest("Leitura aberta", "Descricao publica"), TopicoResponse.class).getBody();
        String caminho = "/api/forum/topicos/" + criado.id();
        assertThat(enviar(HttpMethod.GET, "/api/forum/topicos", null, null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(enviar(HttpMethod.GET, caminho + "/comentarios", null, null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(enviar(HttpMethod.GET, "/api/noticias", null, null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(enviar(HttpMethod.GET, "/api/forum/users", null, null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(enviar(HttpMethod.POST, "/api/forum/topicos", null, new TopicoRequest("Teste", "Descricao"),
                String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(enviar(HttpMethod.POST, caminho + "/comentarios", null, Map.of("conteudo", "Resposta"), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(enviar(HttpMethod.PUT, caminho, null, new TopicoRequest("Teste", "Descricao"), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(enviar(HttpMethod.DELETE, caminho, null, null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(enviar(HttpMethod.POST, "/api/noticias", null, Map.of("titulo", "Teste", "conteudo", "Texto"),
                String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void criarTopico_comDadosValidos_deveRetornarAutorIdEPersistir() {
        var resposta = enviar(HttpMethod.POST, "/api/forum/topicos", autor.token(), new TopicoRequest("Java", "Duvida"),
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
    void listarTopicos_comBuscaEPaginacao_deveFiltrarOrdenarEContarRespostasAtivas() {
        var primeiro = criarTopico("Java inicial");
        criarComentario(primeiro.id(), "Resposta");
        var apagado = criarComentario(primeiro.id(), "Apagado");
        excluirComentario(apagado.id(), autor.token());
        var segundo = criarTopico("JAVA avancado");
        var arquivado = criarTopico("Java arquivado");
        alterarEstado(arquivado.id(), "arquivar", autor.token());
        criarTopico("Outro assunto");
        var resposta = enviar(HttpMethod.GET, "/api/forum/topicos?busca=java&page=0&size=1", autor.token(), null,
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
    }

    @Test
    void fecharTopico_comAutor_devePersistirFechamento() {
        var topico = criarTopico("Fechar");
        alterarEstado(topico.id(), "fechar", autor.token());
        assertThat(topicos.findById(topico.id()).orElseThrow().isFechado()).isTrue();
    }

    @Test
    void apagarTopico_comAdministrador_deveRetornar200EExcluirLogicamente() {
        var topico = criarTopico("Excluir");
        var admin = cadastrarEAutenticar(Perfil.ADMINISTRADOR);
        var resposta = enviar(HttpMethod.DELETE, "/api/forum/topicos/" + topico.id(), admin.token(), null,
                String.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).isNullOrEmpty();
        assertThat(topicos.findById(topico.id()).orElseThrow().isAtivo()).isFalse();
    }

    @Test
    void apagarTopico_semToken_deveRetornar401EPreservarRegistro() {
        var topico = criarTopico("Protegido");
        var resposta = enviar(HttpMethod.DELETE, "/api/forum/topicos/" + topico.id(), null, null, ErroHttp.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(topicos.findById(topico.id()).orElseThrow().isAtivo()).isTrue();
    }

    @Test
    void apagarTopico_comAluno_deveRetornar403EPreservarRegistro() {
        var topico = criarTopico("Protegido");
        verificarErro(
                enviar(HttpMethod.DELETE, "/api/forum/topicos/" + topico.id(), autor.token(), null, ErroHttp.class),
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
    void listarComentarios_comDoisTopicos_deveListarSomenteAtivosDoTopicoEmOrdem() {
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
        assertThat(resposta.getBody()).extracting(ComentarioResponse::id).containsExactly(primeiro.id(), segundo.id());
        assertThat(resposta.getBody()).extracting(ComentarioResponse::conteudo).containsExactly("Primeiro", "Segundo");
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
        var resposta = enviar(HttpMethod.DELETE, comentariosUrl(topico.id()) + "/" + comentario.id(), autor.token(),
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
        var resposta = enviar(HttpMethod.GET, "/api/forum/users", autor.token(), null, UsuariosForumResponse.class);
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
                enviar(HttpMethod.POST, "/api/forum/topicos", autor.token(), new TopicoRequest("", ""), ErroHttp.class),
                400);
        assertThat(topicos.count()).isZero();
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
        verificarErro(enviar(HttpMethod.POST, comentariosUrl(topico.id()), autor.token(), new ComentarioRequest(" "),
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
        verificarErro(enviar(HttpMethod.GET, comentariosUrl(999999L), autor.token(), null, ErroHttp.class), 404);
    }

    @Test
    void listarComentarios_comTopicoArquivado_deveRetornar404() {
        var topico = criarTopico("Arquivado");
        alterarEstado(topico.id(), "arquivar", autor.token());
        verificarErro(enviar(HttpMethod.GET, comentariosUrl(topico.id()), autor.token(), null, ErroHttp.class), 404);
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
        verificarErro(enviar(HttpMethod.PUT, "/api/forum/topicos/" + topico.id() + "/" + acao, outro.token(), null,
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
        verificarErro(enviar(HttpMethod.DELETE, "/api/forum/comentarios/" + comentario.id(), outro.token(), null,
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
        verificarErro(enviar(HttpMethod.DELETE, comentariosUrl(outro.id()) + "/" + comentario.id(), autor.token(), null,
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
        verificarErro(enviar(HttpMethod.DELETE, "/api/forum/comentarios/999999", autor.token(), null, ErroHttp.class),
                404);
    }

    @ParameterizedTest
    @ValueSource(strings = { "arquivar", "fechar" })
    void alterarEstado_comIdInexistente_deveRetornar404(String acao) {
        verificarErro(enviar(HttpMethod.PUT, "/api/forum/topicos/999999/" + acao, autor.token(), null, ErroHttp.class),
                404);
    }

    @Test
    void criarTopico_comProfessor_devePermitirPublicacao() {
        var professor = cadastrarEAutenticar(Perfil.PROFESSOR);
        var resposta = enviar(HttpMethod.POST, "/api/forum/topicos", professor.token(),
                new TopicoRequest("Aula", "Discussao"), TopicoResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody().papelAutor()).isEqualTo("PROFESSOR");
        assertThat(topicos.findById(resposta.getBody().id()).orElseThrow().getIdAutor()).isEqualTo(professor.id());
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
        var admin = cadastrarEAutenticar(Perfil.ADMINISTRADOR, "cursovalido@gmail.com");
        var resposta = enviar(HttpMethod.POST, "/api/politicas", admin.token(),
                new PoliticaRequest("Novos termos", "teste-v2", "Conteudo novo"), PoliticaResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody().versao()).isEqualTo("teste-v2");
        assertThat(resposta.getBody().nome()).isEqualTo("Novos termos");
        assertThat(resposta.getBody().conteudo()).isEqualTo("Conteudo novo");
        assertThat(politicas.findByVersao("teste-v2")).isPresent();
        assertThat(politicas.findByAtivaTrueOrderByDataPublicacaoDesc()).extracting(Politica::getVersao)
                .containsExactly("teste-v2");
        assertThat(usuarios.findById(autor.id()).orElseThrow().getAceitouTermosLgpd()).isTrue();
        assertThat(usuarios.findById(autor.id()).orElseThrow().getVersaoTermos()).isEqualTo("teste-v1");
    }

    @Test
    void publicarPolitica_comAluno_deveRetornar403SemPersistir() {
        verificarErro(enviar(HttpMethod.POST, "/api/politicas", autor.token(),
                new PoliticaRequest("Termos", "teste-v2", "Conteudo"), ErroHttp.class), 403);
        assertThat(politicas.findByVersao("teste-v2")).isEmpty();
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
        assertThat(convites.findByEmail(email)).isEmpty();
        assertThat(enviar(HttpMethod.GET, "/api/autenticacao/meus-dados", sessaoAnterior, null, String.class)
                .getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        jdbc.update("update tb_contas_excluidas set bloqueada_ate = ? where id = ?",
                java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(1)), idAnterior);
        long idNovo = cadastrarConvidado(email, principal.token());
        assertThat(idNovo).isNotEqualTo(idAnterior);
        assertThat(enviar(HttpMethod.POST, "/api/convites/confirmar-email", null, Map.of("token", linkAnterior),
                String.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(enviar(HttpMethod.GET, "/api/autenticacao/meus-dados", sessaoAnterior, null, String.class)
                .getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        String sessaoNova = autenticarConvidado(email);
        assertThat(
                enviar(HttpMethod.GET, "/api/autenticacao/meus-dados", sessaoNova, null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @ParameterizedTest
    @ValueSource(strings = { "PENDENTE", "CONCLUIDO" })
    void novoConviteReaproveitaRegistroAntigoSemContaEInvalidaLinkAnterior(String statusAnterior) {
        var principal = cadastrarEAutenticar(Perfil.ADMINISTRADOR, "principal-convite@example.com");
        String email = "sem-conta@example.com";
        assertThat(enviar(HttpMethod.POST, "/api/administracao/convites", principal.token(),
                new ConviteAcessoRequest(email, Perfil.PROFESSOR), String.class).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        String tokenAnterior = convitesEnviados.get(email);
        var conviteAnterior = convites.findByEmail(email).orElseThrow();
        Long idConvite = conviteAnterior.getId();
        conviteAnterior.setStatus(StatusConvite.valueOf(statusAnterior));
        conviteAnterior.setEmailConfirmado(true);
        convites.saveAndFlush(conviteAnterior);
        assertThat(enviar(HttpMethod.POST, "/api/administracao/convites", principal.token(),
                new ConviteAcessoRequest(email, Perfil.ADMINISTRADOR), String.class).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        var novo = convites.findByEmail(email).orElseThrow();
        assertThat(novo.getId()).isEqualTo(idConvite);
        assertThat(novo.getPerfil()).isEqualTo(Perfil.ADMINISTRADOR);
        assertThat(novo.getStatus()).isEqualTo(StatusConvite.PENDENTE);
        assertThat(novo.isEmailConfirmado()).isFalse();
        assertThat(convites.count()).isEqualTo(1);
        assertThat(convitesEnviados.get(email)).isNotEqualTo(tokenAnterior);
        assertThat(enviar(HttpMethod.POST, "/api/convites/confirmar-email", null, Map.of("token", tokenAnterior),
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
        assertThat(enviar(HttpMethod.POST, "/api/convites/confirmar-email", null, Map.of("token", token), String.class)
                .getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        var cadastro = new CadastroUsuarioRequest("Professor Convidado", email, SENHA,
                LocalDate.of(1990, 1, 1),
                true, true, "teste-v1");
        assertThat(enviar(HttpMethod.POST, "/api/convites/completar-cadastro", null,
                Map.of("token", token, "cadastro", cadastro), String.class).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        var usuario = usuarios.findByEmail(email).orElseThrow();
        assertThat(enviar(HttpMethod.POST, "/api/administracao/convites/" + usuario.getId() + "/aprovar",
                tokenPrincipal, null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        return usuario.getId();
    }

    private String autenticarConvidado(String email) {
        var login = enviar(HttpMethod.POST, "/api/autenticacao/login", null, Map.of("email", email, "senha", SENHA),
                AutenticacaoResponse.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        var confirmado = enviar(HttpMethod.POST, "/api/autenticacao/confirmar-2fa", null,
                Map.of("email", email, "codigo", codigos.get(email)), AutenticacaoResponse.class);
        assertThat(confirmado.getStatusCode()).isEqualTo(HttpStatus.OK);
        return confirmado.getBody().token();
    }

    private Sessao cadastrarEAutenticar(Perfil perfil) {
        return cadastrarEAutenticar(perfil, null);
    }

    private Sessao cadastrarEAutenticar(Perfil perfil, String emailPrincipal) {
        long numero = SEQUENCIA.incrementAndGet();
        String email = emailPrincipal == null ? "forum-" + numero + "@example.com" : emailPrincipal;
        String nome = "Participante " + numero;
        var requisicao = new CadastroUsuarioRequest(nome, email, SENHA,
                LocalDate.of(2000, 1, 1), true, true, "teste-v1");
        assertThat(
                enviar(HttpMethod.POST, "/api/autenticacao/cadastro", null, requisicao, String.class).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(confirmacoes.get(email)).isNotBlank();
        assertThat(enviar(HttpMethod.POST, "/api/autenticacao/confirmar-email", null,
                Map.of("token", confirmacoes.get(email)), String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        // Bootstrap do administrador: o cadastro publico cria somente ALUNO.
        if (perfil != Perfil.ALUNO) {
            var usuario = usuarios.findByEmail(email).orElseThrow();
            usuario.setPerfil(perfil);
            usuarios.saveAndFlush(usuario);
            if (emailPrincipal != null) {
                var registro = new InicializacaoAdministrador();
                registro.concluir(usuario.getId());
                inicializacao.saveAndFlush(registro);
            }
        }
        var login = enviar(HttpMethod.POST, "/api/autenticacao/login", null, Map.of("email", email, "senha", SENHA),
                AutenticacaoResponse.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody().requerCodigo2fa()).isTrue();
        assertThat(codigos.get(email)).isNotBlank();
        var confirmacao = enviar(HttpMethod.POST, "/api/autenticacao/confirmar-2fa", null,
                Map.of("email", email, "codigo", codigos.get(email)), AutenticacaoResponse.class);
        assertThat(confirmacao.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(confirmacao.getBody().token()).isNotBlank();
        return new Sessao(confirmacao.getBody().idUsuario(), nome, confirmacao.getBody().token());
    }

    private TopicoResponse criarTopico(String titulo) {
        var resposta = enviar(HttpMethod.POST, "/api/forum/topicos", autor.token(),
                new TopicoRequest(titulo, "Descricao " + titulo), TopicoResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody().id()).isPositive();
        return resposta.getBody();
    }

    private ComentarioResponse criarComentario(Long id, String conteudo) {
        var resposta = enviar(HttpMethod.POST, comentariosUrl(id), autor.token(), new ComentarioRequest(conteudo),
                ComentarioResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody().conteudo()).isEqualTo(conteudo);
        return resposta.getBody();
    }

    private void alterarEstado(Long id, String acao, String token) {
        var resposta = enviar(HttpMethod.PUT, "/api/forum/topicos/" + id + "/" + acao, token, null, String.class);
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
