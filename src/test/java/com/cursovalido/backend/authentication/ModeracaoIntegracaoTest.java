package com.cursovalido.backend.authentication;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cursovalido.backend.authentication.dto.SuspensaoRequest;
import com.cursovalido.backend.authentication.entity.*;
import com.cursovalido.backend.authentication.repository.*;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import com.cursovalido.backend.authentication.service.*;
import com.cursovalido.backend.forum.repository.TopicoRepository;
import com.cursovalido.backend.forum.service.UsuarioForumService;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "security.bcrypt.strength=10",
        "seguranca.limite-requisicoes=10000",
        "aplicacao.admin.email=",
        "aplicacao.admin.senha="
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ModeracaoIntegracaoTest {
    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private InicializacaoAdministradorRepository inicializacao;
    @Autowired private SuspensaoUsuarioRepository suspensoes;
    @Autowired private ConviteAcessoRepository convites;
    @Autowired private PoliticaRepository politicas;
    @Autowired private ModeracaoUsuarioService moderacao;
    @Autowired private IdentidadeExcluidaService identidades;
    @Autowired private ContaExcluidaRepository contasExcluidas;
    @Autowired private ContaUsuarioService contas;
    @Autowired private TokenEmailService tokensEmail;

    @ParameterizedTest
    @EnumSource(Perfil.class)
    void exclusaoPreservaPunicoesEBloqueiaIdentificadores(Perfil perfil) {
        var antigo = criar("exclusao@example.com", perfil);
        antigo.setCpf("12345678901");
        usuarios.saveAndFlush(antigo);
        for (var escopo : EscopoSuspensao.values()) {
            moderacao.aplicar(antigo.getId(), new SuspensaoRequest(escopo, "Teste de persistencia", null), principal.getId());
        }
        String codigo = tokensEmail.criarExclusao(antigo);
        contas.excluirConta(antigo.getId(), codigo);
        usuarios.flush();
        assertThat(usuarios.findById(antigo.getId())).isEmpty();
        assertThatThrownBy(() -> identidades.verificarCadastro("EXCLUSAO@example.com", "99999999999"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(() -> identidades.verificarCadastro("outro@example.com", "123.456.789-01"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        var registro = contasExcluidas.findById(antigo.getId()).orElseThrow();
        registro.setBloqueadaAte(Instant.now().minusSeconds(1));
        contasExcluidas.saveAndFlush(registro);
        identidades.verificarCadastro("exclusao@example.com", "12345678901");
        var novo = criar("outro@example.com", Perfil.ALUNO);
        novo.setCpf("12345678901");
        usuarios.saveAndFlush(novo);
        assertThat(moderacao.restrito(novo.getId(), EscopoSuspensao.CONTA)).isTrue();
        assertThat(moderacao.restrito(novo.getId(), EscopoSuspensao.FORUM)).isTrue();
        var peloEmail = criar("exclusao@example.com", Perfil.ALUNO);
        assertThat(moderacao.restrito(peloEmail.getId(), EscopoSuspensao.CONTA)).isTrue();
    }

    @Test
    void solicitarCodigoPorHttpEExcluirSomenteComConfirmacao() throws Exception {
        mvc.perform(delete("/api/autenticacao/conta").with(como(aluno)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/autenticacao/conta/solicitar-exclusao").with(como(aluno)))
                .andExpect(status().isNoContent());
        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(emails).enviarExclusaoConta(eq(aluno), captor.capture());
        mvc.perform(delete("/api/autenticacao/conta").with(como(aluno))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"" + captor.getValue() + "\"}"))
                .andExpect(status().isNoContent());
        assertThat(usuarios.findById(aluno.getId())).isEmpty();
    }

    @Test
    void reenvioInvalidaCodigoAnteriorEExpiracaoImpedeExclusao() {
        String anterior = tokensEmail.criarExclusao(aluno);
        String atual = tokensEmail.criarExclusao(aluno);
        entidadesExclusao.flush();
        entidadesExclusao.clear();
        assertThatThrownBy(() -> tokensEmail.validarExclusao(aluno.getId(), anterior))
                .isInstanceOf(IllegalArgumentException.class);
        jdbcExclusao.update("update tb_tokens_email set expira_em = ? where usuario_id = ?",
                java.time.LocalDateTime.now().minusMinutes(1), aluno.getId());
        entidadesExclusao.clear();
        assertThatThrownBy(() -> tokensEmail.validarExclusao(aluno.getId(), atual))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcExclusao;
    @Autowired private jakarta.persistence.EntityManager entidadesExclusao;

    @Test
    void exclusaoExigeCodigoValidoDaPropriaConta() {
        String codigo = tokensEmail.criarExclusao(aluno);
        assertThatThrownBy(() -> tokensEmail.validarExclusao(professor.getId(), codigo))
                .isInstanceOf(IllegalArgumentException.class);
        tokensEmail.validarExclusao(aluno.getId(), codigo);
        assertThatThrownBy(() -> tokensEmail.validarExclusao(aluno.getId(), codigo))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(usuarios.findById(aluno.getId())).isPresent();
    }

    @Autowired private TokenJwtService tokens;
    @MockitoBean private UsuarioForumService forum;
    @Autowired private PasswordEncoder senhas;
    @MockitoBean private EmailService emails;
    @MockitoBean private ValidacaoEmailService validacaoEmail;
    private Usuario principal;
    private Usuario administrador;
    private Usuario aluno;
    private Usuario professor;

    @BeforeEach
    void preparar() {
        principal = criar("cursovalido@gmail.com", Perfil.ADMINISTRADOR);
        var registro = inicializacao.findById(1L).orElseThrow();
        registro.concluir(principal.getId());
        inicializacao.saveAndFlush(registro);
        administrador = criar("admin@example.com", Perfil.ADMINISTRADOR);
        aluno = criar("aluno@example.com", Perfil.ALUNO);
        professor = criar("professor@example.com", Perfil.PROFESSOR);
    }

    @Test
    void permissoesPorPerfilBloqueiamOutrasAreasERotasSemRegra() throws Exception {
        mvc.perform(post("/api/cursos").with(como(aluno)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/cursos").with(como(administrador)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/administracao/usuarios").with(como(professor)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/administracao/usuarios").with(como(aluno)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/aluno/matriculas").with(como(professor)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/forum/topicos/123").with(como(aluno)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/forum/topicos/123").with(como(professor)))
                .andExpect(status().isForbidden());
        for (var usuario : new Usuario[] { aluno, professor, administrador, principal }) {
            for (String caminho : new String[] { "/api/sem-regra", "/api/forum/sem-regra",
                    "/api/administracao/sem-regra", "/api/aluno/sem-regra", "/api/cursos/123/sem-regra" }) {
                mvc.perform(get(caminho).with(como(usuario))).andExpect(status().isForbidden());
            }
        }
    }

    @Test
    void somentePrincipalGerenciaConvitesEPublicaTermos() throws Exception {
        mvc.perform(get("/api/administracao/permissoes").with(como(administrador)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.principal").value(false));
        mvc.perform(get("/api/administracao/permissoes").with(como(principal)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.principal").value(true));
        mvc.perform(get("/api/administracao/convites").with(como(administrador))).andExpect(status().isForbidden());
        String convite = "{\"email\":\"novo@example.com\",\"perfil\":\"PROFESSOR\"}";
        mvc.perform(post("/api/administracao/convites").with(como(administrador))
                .contentType(MediaType.APPLICATION_JSON).content(convite)).andExpect(status().isForbidden());
        mvc.perform(post("/api/administracao/convites").with(como(principal))
                .contentType(MediaType.APPLICATION_JSON).content(convite)).andExpect(status().isNoContent());
        String termos = "{\"nome\":\"Termos\",\"versao\":\"moderacao-v1\",\"conteudo\":\"Novos termos\"}";
        mvc.perform(post("/api/politicas").with(como(administrador)).contentType(MediaType.APPLICATION_JSON).content(termos))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro").value("Esta operacao e exclusiva do administrador principal"));
        assertThat(politicas.findByVersao("moderacao-v1")).isEmpty();
        mvc.perform(post("/api/politicas").with(como(principal)).contentType(MediaType.APPLICATION_JSON).content(termos))
                .andExpect(status().isCreated());
    }

    @Test
    void reenviarMantemConviteUnicoEInvalidaLinkAnterior() throws Exception {
        AtomicReference<String> token = new AtomicReference<>();
        doAnswer(chamada -> { token.set(chamada.getArgument(1)); return null; }).when(emails).enviarConvite(any(), anyString());
        mvc.perform(post("/api/administracao/convites").with(como(principal)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"convite@example.com\",\"perfil\":\"PROFESSOR\"}")).andExpect(status().isNoContent());
        String anterior = token.get();
        var convite = convites.findByEmail("convite@example.com").orElseThrow();
        mvc.perform(post("/api/administracao/convites/" + convite.getId() + "/reenviar").with(como(administrador)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/administracao/convites/" + convite.getId() + "/reenviar").with(como(principal)))
                .andExpect(status().isNoContent());
        convites.flush();
        assertThat(convites.count()).isEqualTo(1);
        assertThat(token.get()).isNotEqualTo(anterior);
        mvc.perform(post("/api/convites/confirmar-email").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + anterior + "\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/convites/confirmar-email").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token.get() + "\"}")).andExpect(status().isNoContent());
        politicas.save(new Politica("Termos", "convite-v1", "Termos do convite"));
        String cadastro = """
                {"token":"%s","cadastro":{"nomeCompleto":"Professor Convidado","email":"convite@example.com",
                "senha":"SenhaTeste123!","cpf":"12345678901","dataNascimento":"1990-01-01","telefone":"11999999999",
                "endereco":{"cep":"01001000","logradouro":"Rua A","bairro":"Centro","cidade":"Sao Paulo","estado":"SP"},
                "declarouMaiorIdade":true,"aceitouTermosLgpd":true,"versaoTermos":"convite-v1"}}
                """.formatted(token.get());
        mvc.perform(post("/api/convites/completar-cadastro").contentType(MediaType.APPLICATION_JSON).content(cadastro))
                .andExpect(status().isNoContent());
        var convidado = usuarios.findByEmail("convite@example.com").orElseThrow();
        assertThat(convidado.getPerfil()).isEqualTo(Perfil.PROFESSOR);
        assertThat(convidado.isAtivo()).isFalse();
        mvc.perform(post("/api/administracao/convites/" + convidado.getId() + "/aprovar").with(como(administrador)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/administracao/convites/" + convidado.getId() + "/aprovar").with(como(principal)))
                .andExpect(status().isNoContent());
        assertThat(usuarios.findById(convidado.getId()).orElseThrow().isAtivo()).isTrue();
    }

    @Test
    void administradorModeraAlunosEProfessoresMasNaoAdministradores() throws Exception {
        Usuario outroAdministrador = criar("outro-admin@example.com", Perfil.ADMINISTRADOR);
        for (var alvo : new Usuario[] { aluno, professor }) {
            mvc.perform(post("/api/administracao/usuarios/" + alvo.getId() + "/suspensoes").with(como(administrador))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"escopo\":\"FORUM\",\"motivo\":\"Moderacao\"}"))
                    .andExpect(status().isCreated());
        }
        for (var alvo : new Usuario[] { principal, administrador, outroAdministrador }) {
            mvc.perform(post("/api/administracao/usuarios/" + alvo.getId() + "/suspensoes").with(como(administrador))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"escopo\":\"CONTA\",\"motivo\":\"Bloqueio\"}"))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/administracao/usuarios/" + administrador.getId() + "/suspensoes").with(como(principal))
                .contentType(MediaType.APPLICATION_JSON).content("{\"escopo\":\"CONTA\",\"motivo\":\"Bloqueio administrativo\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/administracao/usuarios").with(como(administrador)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("CONTA_SUSPENSA"));
    }

    @ParameterizedTest
    @EnumSource(EscopoSuspensao.class)
    void restricoesSaoAplicadasNoServidorERevogaveis(EscopoSuspensao escopo) throws Exception {
        var suspensao = moderacao.aplicar(aluno.getId(), new SuspensaoRequest(escopo, "Teste", null), administrador.getId());
        switch (escopo) {
            case CONTA -> mvc.perform(get("/api/aluno/cursos").with(como(aluno))).andExpect(status().isForbidden());
            case FORUM -> mvc.perform(get("/api/forum/topicos").with(como(aluno))).andExpect(status().isForbidden());
            case TOPICOS -> mvc.perform(post("/api/forum/topicos").with(como(aluno)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"titulo\":\"Teste\",\"descricao\":\"Duvida\"}")).andExpect(status().isForbidden());
            case COMENTARIOS -> mvc.perform(post("/api/forum/topicos/1/comentarios").with(como(aluno))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"conteudo\":\"Resposta\"}")).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/autenticacao/restricoes").with(como(aluno)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].escopo").value(escopo.name()));
        mvc.perform(delete("/api/administracao/usuarios/" + aluno.getId() + "/suspensoes/" + suspensao.id()).with(como(administrador)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/forum/topicos").with(como(aluno))).andExpect(status().isOk());
    }

    @Test
    void restricoesDeConteudoBloqueiamCrudCompletoDeTopicosEComentarios() throws Exception {
        for (var escopo : new EscopoSuspensao[] { EscopoSuspensao.TOPICOS, EscopoSuspensao.COMENTARIOS }) {
            var suspensao = moderacao.aplicar(aluno.getId(), new SuspensaoRequest(escopo, "Teste CRUD", null), administrador.getId());
            for (String caminho : new String[] { "/api/forum/topicos", "/api/forum/topicos/1/comentarios" }) {
                mvc.perform(get(caminho).with(como(aluno))).andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.codigo").value(escopo.name() + "_SUSPENSOS"));
                mvc.perform(post(caminho).with(como(aluno)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                        .andExpect(status().isForbidden());
            }
            for (String caminho : new String[] { "/api/forum/topicos/1", "/api/forum/topicos/1/arquivar",
                    "/api/forum/topicos/1/fechar", "/api/forum/comentarios/1" }) {
                mvc.perform(put(caminho).with(como(aluno)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                        .andExpect(status().isForbidden());
            }
            for (String caminho : new String[] { "/api/forum/comentarios/1", "/api/forum/topicos/1/comentarios/1" }) {
                mvc.perform(delete(caminho).with(como(aluno))).andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.codigo").value(escopo.name() + "_SUSPENSOS"));
            }
            mvc.perform(get("/api/autenticacao/restricoes").with(como(aluno))).andExpect(status().isOk());
            mvc.perform(get("/api/aluno/cursos").with(como(aluno))).andExpect(status().isOk());
            moderacao.revogar(aluno.getId(), suspensao.id(), administrador.getId());
            mvc.perform(get("/api/forum/topicos").with(como(aluno))).andExpect(status().isOk());
        }
    }

    @Test
    void penalidadeRevogaTodasAsSessoesDoAlvo() throws Exception {
        String primeira = tokens.gerar(aluno);
        String segunda = tokens.gerar(aluno);
        String deOutroUsuario = tokens.gerar(professor);
        for (var escopo : EscopoSuspensao.values()) {
            String atual = tokens.gerar(aluno);
            moderacao.aplicar(aluno.getId(), new SuspensaoRequest(escopo, "Desconectar", null), administrador.getId());
            // A consulta de validacao precisa refletir a atualizacao em lote no banco.
            mvc.perform(get("/api/autenticacao/restricoes").header("Authorization", "Bearer " + atual))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/autenticacao/restricoes").header("Authorization", "Bearer " + primeira))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/autenticacao/restricoes").header("Authorization", "Bearer " + segunda))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/autenticacao/restricoes").header("Authorization", "Bearer " + deOutroUsuario))
                .andExpect(status().isOk());
    }

    @Test
    void publicarTermosPersisteConteudoEPreservaSessaoDoAdministrador() throws Exception {
        politicas.save(new Politica("Anterior", "antes-publicar", "Conteudo anterior"));
        principal.setVersaoTermos("antes-publicar");
        principal.setDataHoraAceite(java.time.LocalDateTime.now());
        usuarios.saveAndFlush(principal);
        String token = tokens.gerar(principal);
        mvc.perform(post("/api/politicas").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Novo documento\",\"versao\":\"depois-publicar\",\"conteudo\":\"Texto definido pelo administrador\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conteudo").value("Texto definido pelo administrador"));
        assertThat(politicas.findByVersao("depois-publicar").orElseThrow().getConteudo())
                .isEqualTo("Texto definido pelo administrador");
        mvc.perform(get("/termos-aceite.html"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Texto definido pelo administrador")));
        mvc.perform(get("/api/administracao/permissoes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.principal").value(true));
    }

    @Test
    void banimentoInformaDuracaoNaSessaoENoLogin() throws Exception {
        moderacao.aplicar(aluno.getId(), new SuspensaoRequest(EscopoSuspensao.CONTA, "Teste", null), administrador.getId());
        mvc.perform(get("/api/aluno/cursos").with(como(aluno)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Você foi banido permanentemente da plataforma."));
        String login = "{\"email\":\"aluno@example.com\",\"senha\":\"SenhaTeste123!\"}";
        mvc.perform(post("/api/autenticacao/login").contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("CONTA_SUSPENSA"))
                .andExpect(jsonPath("$.erro").value("Você foi banido permanentemente da plataforma."));
        moderacao.aplicar(aluno.getId(), new SuspensaoRequest(EscopoSuspensao.CONTA, "Teste",
                Instant.parse("2099-01-02T15:30:00Z")), administrador.getId());
        mvc.perform(post("/api/autenticacao/login").contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro").value("Você foi banido temporariamente da plataforma até 02/01/2099 às 12:30 (horário de Brasília)."));
    }

    @Test
    void escoposLegadosDeConteudoSaoSubstituidosSemDuplicacao() {
        moderacao.aplicar(aluno.getId(), new SuspensaoRequest(EscopoSuspensao.COMENTARIOS, "Antigo", null), administrador.getId());
        moderacao.aplicar(aluno.getId(), new SuspensaoRequest(EscopoSuspensao.TOPICOS, "Atual",
                Instant.now().plusSeconds(3600)), administrador.getId());
        assertThat(moderacao.vigentes(aluno.getId())).hasSize(1);
        assertThat(moderacao.mensagemBanimento(aluno.getId(), EscopoSuspensao.TOPICOS)).contains("temporariamente", "até");
    }

    @Test
    void restricaoTemporariaExpiraERejeitaDatasPassadas() throws Exception {
        mvc.perform(post("/api/administracao/usuarios/" + aluno.getId() + "/suspensoes").with(como(administrador))
                .contentType(MediaType.APPLICATION_JSON).content("{\"escopo\":\"FORUM\",\"motivo\":\"Teste\",\"expiraEm\":\"2000-01-01T00:00:00Z\"}"))
                .andExpect(status().isBadRequest());
        var criada = moderacao.aplicar(aluno.getId(), new SuspensaoRequest(EscopoSuspensao.FORUM, "Teste", Instant.now().plusSeconds(3600)), administrador.getId());
        mvc.perform(get("/api/forum/topicos").with(como(aluno))).andExpect(status().isForbidden());
        var entidade = suspensoes.findById(criada.id()).orElseThrow();
        entidade.setExpiraEm(Instant.now().minusSeconds(1));
        suspensoes.saveAndFlush(entidade);
        mvc.perform(get("/api/forum/topicos").with(como(aluno))).andExpect(status().isOk());
    }

    @Test
    void impedirRevogacaoDeSuspensaoDeOutroUsuarioEAutoPromocao() throws Exception {
        var criada = moderacao.aplicar(aluno.getId(), new SuspensaoRequest(EscopoSuspensao.FORUM, "Teste", null), administrador.getId());
        mvc.perform(delete("/api/administracao/usuarios/" + professor.getId() + "/suspensoes/" + criada.id()).with(como(administrador)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/administracao/usuarios/" + professor.getId() + "/suspensoes").with(como(aluno))
                .contentType(MediaType.APPLICATION_JSON).content("{\"escopo\":\"CONTA\",\"motivo\":\"Teste\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/administracao/usuarios")).andExpect(status().isUnauthorized());
    }


    @Test
    void publicarDocumentosPreservaSessaoEExigeAceiteNoProximoLogin() throws Exception {
        var anterior = politicas.saveAndFlush(new Politica("Anterior", "anterior-regressao", "Conteúdo anterior"));
        aluno.setVersaoTermos(anterior.getVersao());
        aluno.setDataHoraAceite(java.time.LocalDateTime.now().minusDays(1));
        usuarios.saveAndFlush(aluno);
        String tokenAdmin = tokens.gerar(principal);
        String tokenAluno = tokens.gerar(aluno);

        mvc.perform(post("/api/politicas").header("Authorization", "Bearer " + tokenAdmin)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\":\"Termos\",\"versao\":\"sessao-preservada\",\"conteudo\":\"Nova versão\"}"))
            .andExpect(status().isCreated());
        assertThat(usuarios.findById(aluno.getId()).orElseThrow().getAceitouTermosLgpd()).isTrue();
        mvc.perform(get("/api/administracao/permissoes").header("Authorization", "Bearer " + tokenAdmin))
            .andExpect(status().isOk());
        mvc.perform(get("/api/autenticacao/restricoes").header("Authorization", "Bearer " + tokenAluno))
            .andExpect(status().isOk());
        String login = "{\"email\":\"aluno@example.com\",\"senha\":\"SenhaTeste123!\"}";
        mvc.perform(post("/api/autenticacao/login").contentType(MediaType.APPLICATION_JSON).content(login))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("TERMOS_NAO_ACEITOS"));
        mvc.perform(post("/api/autenticacao/aceitar-termos-novamente")
            .header("Authorization", "Bearer " + tokens.gerarTermos(aluno)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"versaoTermos\":\"sessao-preservada\"}"))
            .andExpect(status().isNoContent());
        mvc.perform(post("/api/autenticacao/login").contentType(MediaType.APPLICATION_JSON).content(login))
            .andExpect(status().isOk());
    }

    @Test
    void privacidadePermitePublicarNovamenteSemExpirarSessao() throws Exception {
        var anterior = politicas.saveAndFlush(new Politica("Anterior", "anterior-regressao", "Conteúdo anterior"));
        aluno.setVersaoTermos(anterior.getVersao());
        aluno.setDataHoraAceite(java.time.LocalDateTime.now().minusDays(1));
        usuarios.saveAndFlush(aluno);
        String token = tokens.gerar(principal);
        String documento = "{\"nome\":\"Privacidade\",\"versao\":\"privacidade-1\",\"conteudo\":\"Dados pessoais <script>teste</script>\"}";
        mvc.perform(post("/api/politicas/privacidade").with(como(administrador))
            .contentType(MediaType.APPLICATION_JSON).content(documento)).andExpect(status().isForbidden());
        mvc.perform(post("/api/politicas/privacidade").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(documento)).andExpect(status().isCreated());
        mvc.perform(get("/api/politicas/privacidade/ativa"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.versao").value("privacidade-1"));
        mvc.perform(get("/politica-privacidade.html"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Dados pessoais </article>")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script>teste"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("teste</script>"))));
        mvc.perform(post("/api/politicas/privacidade").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(documento.replace("privacidade-1", "privacidade-2")))
            .andExpect(status().isCreated());
        mvc.perform(get("/api/administracao/permissoes").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
        mvc.perform(post("/api/autenticacao/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"aluno@example.com\",\"senha\":\"SenhaTeste123!\"}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("TERMOS_NAO_ACEITOS"));
    }

    private Usuario criar(String email, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setNomeCompleto(email);
        usuario.setEmail(email);
        usuario.setSenha(senhas.encode("SenhaTeste123!"));
        usuario.setMatricula(email);
        usuario.setPerfil(perfil);
        usuario.setEmailConfirmado(true);
        usuario.setDeclarouMaiorIdade(true);
        usuario.setAceitouTermosLgpd(true);
        usuarios.saveAndFlush(usuario);
        return usuario;
    }

    private RequestPostProcessor como(Usuario usuario) {
        var principal = new UsuarioAutenticado(usuario.getId(), usuario.getEmail(), "senha", usuario.getPerfil(), true, true, true);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
