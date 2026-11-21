package com.cursovalido.backend.authentication;

import com.cursovalido.backend.authentication.validation.email.ValidacaoEmailService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cursovalido.backend.authentication.entity.*;
import com.cursovalido.backend.authentication.repository.*;
import com.cursovalido.backend.authentication.service.*;
import com.cursovalido.backend.forum.service.UsuarioForumService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "security.bcrypt.strength=10")
@ActiveProfiles("test")
class AutenticacaoHttpTest {
        @Autowired
        private com.cursovalido.backend.authentication.service.TokenEmailService tokensExclusao;
        @LocalServerPort
        private int porta;
        @Autowired
        private UsuarioRepository usuarios;
        @Autowired
        private PoliticaRepository politicas;
        @Autowired
        private PasswordEncoder senhas;
        @Autowired
        private JdbcTemplate jdbc;
        @Autowired
        private LogAuditoriaService auditoria;
        @Autowired
        private TokenJwtService tokens;
        @MockitoBean
        private EmailService emails;
        @MockitoBean
        private ValidacaoEmailService validacaoEmail;
        @MockitoBean
        private UsuarioForumService usuariosForum;
        @Autowired
        private ResponsabilidadeService responsabilidade;
        private RestClient cliente;
        private Usuario usuario;
        private final Map<String, String> codigos = new ConcurrentHashMap<>();
        private final Map<String, String> recuperacoes = new ConcurrentHashMap<>();

        @BeforeEach
        void preparar() {
                cliente = RestClient.builder().baseUrl("http://localhost:" + porta)
                                .defaultStatusHandler(status -> true, (request, response) -> {
                                }).build();
                usuario = new Usuario();
                usuario.setNomeCompleto("Aluno Teste");
                usuario.setEmail(UUID.randomUUID() + "@example.com");
                usuario.setSenha(senhas.encode("SenhaTeste123!"));
                usuario.setMatricula(UUID.randomUUID().toString());
                usuario.setPerfil(Perfil.ALUNO);
                usuario.setEmailConfirmado(true);
                usuario.setAceitouTermosLgpd(true);
                usuario.setDeclarouMaiorIdade(true);
                usuario.setVersaoTermos("teste-http");
                usuario.setDataHoraAceite(LocalDateTime.now());
                usuario = usuarios.save(usuario);
                if (politicas.findByVersao("teste-http").isEmpty())
                        politicas.save(new Politica("Termos", "teste-http", "Termos do teste"));
                doAnswer(chamada -> {
                        Usuario destinatario = chamada.getArgument(0);
                        codigos.put(destinatario.getEmail(), chamada.getArgument(1));
                        return null;
                }).when(emails).enviarCodigo2fa(any(), anyString());
                doAnswer(chamada -> {
                        Usuario destinatario = chamada.getArgument(0);
                        recuperacoes.put(destinatario.getEmail(), chamada.getArgument(1));
                        return null;
                }).when(emails).enviarRedefinicaoSenha(any(), anyString());
        }

        @Test
        void recarregarDadosEReiniciarControlePreservaSessaoAteLogout() {
                String anterior = entrar();
                for (int i = 0; i < 3; i++) {
                        assertEquals(200,
                                        cliente.get().uri("/api/autenticacao/meus-dados")
                                                        .headers(h -> h.setBearerAuth(anterior))
                                                        .retrieve().toBodilessEntity().getStatusCode().value());
                }
                tokens.prepararChave();
                assertEquals(200,
                                cliente.get().uri("/api/autenticacao/meus-dados")
                                                .headers(h -> h.setBearerAuth(anterior))
                                                .retrieve().toBodilessEntity().getStatusCode().value());
                cliente.post().uri("/api/autenticacao/logout").headers(h -> h.setBearerAuth(anterior))
                                .retrieve().toBodilessEntity();
                assertEquals(401,
                                cliente.get().uri("/api/autenticacao/meus-dados")
                                                .headers(h -> h.setBearerAuth(anterior))
                                                .retrieve().toBodilessEntity().getStatusCode().value());
                String novo = entrar();
                assertEquals(200, cliente.get().uri("/api/autenticacao/meus-dados").headers(h -> h.setBearerAuth(novo))
                                .retrieve().toBodilessEntity().getStatusCode().value());
        }

        private ResponseEntity<Map> post(String caminho, Map<String, ?> corpo) {
                return cliente.post().uri("/api/autenticacao/" + caminho).body(corpo).retrieve().toEntity(Map.class);
        }

        @Test
        void alunoRecemCadastradoEntraComASenhaConfirmadaECodigoRecebido() {
                String email = UUID.randomUUID() + "@example.com";
                String senha = "SenhaNova123!";
                var cadastro = post("cadastro", Map.of(
                                "nomeCompleto", "Aluno Novo", "email", email, "senha", senha, "confirmacaoSenha", senha,
                                "declarouMaiorIdade", true,
                                "aceitouTermosLgpd", true, "versaoTermos", "teste-http"));
                assertEquals(HttpStatus.CREATED, cadastro.getStatusCode(), String.valueOf(cadastro.getBody()));
                assertNull(codigos.get(email));

                var login = post("login", Map.of("email", email, "senha", senha));
                assertEquals(HttpStatus.OK, login.getStatusCode(), String.valueOf(login.getBody()));
                assertEquals(true, login.getBody().get("requerCodigo2fa"));
                assertNull(login.getBody().get("token"));
                assertNotNull(codigos.get(email));
                verify(emails, times(1)).enviarCodigo2fa(any(), anyString());

                var confirmacao = post("confirmar-2fa", Map.of("email", email, "codigo", codigos.get(email)));
                assertEquals(HttpStatus.OK, confirmacao.getStatusCode(), String.valueOf(confirmacao.getBody()));
                assertNotNull(confirmacao.getBody().get("token"));
                assertTrue(usuarios.findByEmail(email).orElseThrow().isEmailConfirmado());
        }

        @Test
        void loginsSimultaneosEnviamUmUnicoCodigo2fa() throws Exception {
                var credenciais = Map.of("email", usuario.getEmail(), "senha", "SenhaTeste123!");
                var inicio = new java.util.concurrent.CountDownLatch(1);
                try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
                        var tarefas = java.util.stream.IntStream.range(0, 2).mapToObj(i -> executor.submit(() -> {
                                inicio.await();
                                return post("login", credenciais).getStatusCode().value();
                        })).toList();
                        inicio.countDown();
                        for (var tarefa : tarefas)
                                assertEquals(200, tarefa.get(10, java.util.concurrent.TimeUnit.SECONDS));
                }
                verify(emails, times(1)).enviarCodigo2fa(any(), anyString());
                assertEquals(429, post("reenviar-2fa", Map.of("email", usuario.getEmail())).getStatusCode().value());
                verify(emails, times(1)).enviarCodigo2fa(any(), anyString());
        }

        private String entrar() {
                var primeiraEtapa = post("login", Map.of("email", usuario.getEmail(), "senha", "SenhaTeste123!"));
                assertEquals(200, primeiraEtapa.getStatusCode().value());
                assertNull(primeiraEtapa.getBody().get("token"));
                var segundaEtapa = post("confirmar-2fa",
                                Map.of("email", usuario.getEmail(), "codigo", codigos.get(usuario.getEmail())));
                assertEquals(200, segundaEtapa.getStatusCode().value());
                assertEquals("ALUNO", segundaEtapa.getBody().get("perfil"));
                return (String) segundaEtapa.getBody().get("token");
        }

        private ResponseEntity<Map> autenticarAteTermos() {
                var inicio = post("login", Map.of("email", usuario.getEmail(), "senha", "SenhaTeste123!"));
                assertEquals(200, inicio.getStatusCode().value());
                assertNull(inicio.getBody().get("token"));
                return post("confirmar-2fa",
                                Map.of("email", usuario.getEmail(), "codigo", codigos.get(usuario.getEmail())));
        }

        @Test
        void emissoesSimultaneasMantemUmaUnicaSessaoAtiva() throws Exception {
                try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
                        var inicio = new java.util.concurrent.CountDownLatch(1);
                        var tarefas = java.util.stream.IntStream.range(0, 2).mapToObj(i -> executor.submit(() -> {
                                inicio.await();
                                return tokens.gerar(usuario);
                        })).toList();
                        inicio.countDown();
                        var emitidos = new java.util.ArrayList<String>();
                        for (var tarefa : tarefas)
                                emitidos.add(tarefa.get(10, java.util.concurrent.TimeUnit.SECONDS));
                        int ativos = 0;
                        for (String token : emitidos) {
                                try {
                                        tokens.extrairEmail(token);
                                        ativos++;
                                } catch (IllegalArgumentException encerrada) {
                                }
                        }
                        assertEquals(1, ativos);
                        assertEquals(1, jdbc.queryForObject(
                                        "select count(*) from tb_sessoes where usuario_id = ? and revogada = false",
                                        Integer.class,
                                        usuario.getId()));
                }
        }

        @Test
        void novoLoginEncerraSessaoAnteriorSomenteDepoisDoSegundoFator() {
                String anterior = entrar();
                assertEquals(200, post("login", Map.of("email", usuario.getEmail(), "senha", "SenhaTeste123!"))
                                .getStatusCode().value());
                assertEquals(200, cliente.get().uri("/api/autenticacao/meus-dados")
                                .headers(h -> h.setBearerAuth(anterior)).retrieve().toBodilessEntity().getStatusCode()
                                .value());
                var acesso = post("confirmar-2fa",
                                Map.of("email", usuario.getEmail(), "codigo", codigos.get(usuario.getEmail())));
                String novo = acesso.getBody().get("token").toString();
                assertEquals(401, cliente.get().uri("/api/autenticacao/meus-dados")
                                .headers(h -> h.setBearerAuth(anterior)).retrieve().toBodilessEntity().getStatusCode()
                                .value());
                assertEquals(200, cliente.get().uri("/api/autenticacao/meus-dados")
                                .headers(h -> h.setBearerAuth(novo)).retrieve().toBodilessEntity().getStatusCode()
                                .value());
        }

        @Test
        void contaLegadaSemConfirmacaoPodeRedefinirSenhaEEntrarComCodigo() {
                usuario.setEmailConfirmado(false);
                usuarios.saveAndFlush(usuario);
                assertEquals(204, post("solicitar-alteracao-senha", Map.of("email", usuario.getEmail()))
                                .getStatusCode().value());
                verify(emails).enviarRedefinicaoSenha(any(), anyString());
                assertNotNull(entrar());
                assertTrue(usuarios.findById(usuario.getId()).orElseThrow().isEmailConfirmado());
        }

        @Test
        void logoutInvalidaJwtEExportacaoNaoExibeSegredos() {
                String token = entrar();
                var dados = cliente.get().uri("/api/autenticacao/exportar-dados")
                                .headers(h -> h.setBearerAuth(token)).retrieve().toEntity(Map.class);
                assertEquals(200, dados.getStatusCode().value());
                assertEquals(usuario.getEmail(), dados.getBody().get("email"));
                assertFalse(dados.getBody().containsKey("senha"));
                assertTrue(dados.getHeaders().getFirst("Content-Disposition").contains("meus-dados.json"));
                assertEquals(204, cliente.post().uri("/api/autenticacao/logout").headers(h -> h.setBearerAuth(token))
                                .retrieve().toBodilessEntity().getStatusCode().value());
                assertEquals(401, cliente.get().uri("/api/autenticacao/meus-dados").headers(h -> h.setBearerAuth(token))
                                .retrieve().toBodilessEntity().getStatusCode().value());
        }

        @Test
        void naoEnviaCodigoSemSenhaCorretaOuDesafioPendente() {
                assertEquals(401,
                                post("login", Map.of("email", usuario.getEmail(), "senha", "incorreta")).getStatusCode()
                                                .value());
                assertEquals(204, post("reenviar-2fa", Map.of("email", usuario.getEmail())).getStatusCode().value());
                verify(emails, never()).enviarCodigo2fa(any(), anyString());
        }

        @Test
        void tokenDeTermosNaoViraTokenDeAcessoDepoisDoAceite() {
                usuario.setAceitouTermosLgpd(false);
                usuarios.save(usuario);
                var login = autenticarAteTermos();
                assertEquals(403, login.getStatusCode().value());
                String token = (String) login.getBody().get("token");
                assertNotNull(token);
                assertEquals(403, cliente.get().uri("/api/autenticacao/meus-dados").headers(h -> h.setBearerAuth(token))
                                .retrieve().toBodilessEntity().getStatusCode().value());
                assertEquals(200, cliente.post().uri("/api/autenticacao/aceitar-termos-novamente")
                                .headers(h -> h.setBearerAuth(token)).body(Map.of("versaoTermos", "teste-http"))
                                .retrieve().toBodilessEntity().getStatusCode().value());
                assertEquals(401, cliente.get().uri("/api/autenticacao/meus-dados").headers(h -> h.setBearerAuth(token))
                                .retrieve().toBodilessEntity().getStatusCode().value());
                assertNotNull(entrar());
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.ValueSource(booleans = { true, false })
        void trocaDeTermosRegistraAceiteSomenteAposNovoLogin(boolean aceitou) {
                String anterior = entrar();
                jdbc.update("update tb_politicas set ativa = false where versao = 'teste-http'");
                var publicada = politicas.saveAndFlush(new Politica("Novo termo", "v1.2", "Leia o novo termo"));
                try {
                        var aviso = cliente.get().uri("/api/autenticacao/meus-dados")
                                        .headers(h -> h.setBearerAuth(anterior))
                                        .retrieve().toEntity(Map.class);
                        assertEquals(403, aviso.getStatusCode().value());
                        assertEquals("TERMOS_REAUTENTICACAO", aviso.getBody().get("codigo"));
                        assertNull(aviso.getBody().get("token"));
                        assertEquals(403,
                                        enviarDecisao("aceitar-termos-novamente", anterior,
                                                        Map.of("versaoTermos", "v1.2"))
                                                        .getStatusCode().value());
                        assertEquals(204, enviarDecisao("decidir-termos", anterior, Map.of("aceitou", aceitou))
                                        .getStatusCode().value());
                        assertEquals(401,
                                        cliente.get().uri("/api/autenticacao/meus-dados")
                                                        .headers(h -> h.setBearerAuth(anterior))
                                                        .retrieve().toBodilessEntity().getStatusCode().value());

                        var pendencia = autenticarAteTermos();
                        assertEquals("TERMOS_NAO_ACEITOS", pendencia.getBody().get("codigo"));
                        String recusado = (String) pendencia.getBody().get("token");
                        assertEquals(204, enviarDecisao("decidir-termos", recusado, Map.of("aceitou", false))
                                        .getStatusCode().value());
                        assertEquals(401,
                                        enviarDecisao("aceitar-termos-novamente", recusado,
                                                        Map.of("versaoTermos", "v1.2"))
                                                        .getStatusCode().value());
                        var antes = usuarios.findById(usuario.getId()).orElseThrow();
                        assertEquals("teste-http", antes.getVersaoTermos());

                        String restrito = (String) autenticarAteTermos().getBody().get("token");
                        var aceite = enviarDecisao("aceitar-termos-novamente", restrito,
                                        Map.of("versaoTermos", "v1.2"));
                        assertEquals(200, aceite.getStatusCode().value());
                        String acesso = (String) aceite.getBody().get("token");
                        assertEquals(200,
                                        cliente.get().uri("/api/autenticacao/meus-dados")
                                                        .headers(h -> h.setBearerAuth(acesso))
                                                        .retrieve().toBodilessEntity().getStatusCode().value());
                        var depois = usuarios.findById(usuario.getId()).orElseThrow();
                        assertEquals("v1.2", depois.getVersaoTermos());
                        assertTrue(depois.getDataHoraAceite().isAfter(antes.getDataHoraAceite()));
                } finally {
                        politicas.delete(publicada);
                        jdbc.update("update tb_politicas set ativa = true where versao = 'teste-http'");
                }
        }

        private ResponseEntity<Map> enviarDecisao(String caminho, String token, Map<String, ?> corpo) {
                return cliente.post().uri("/api/autenticacao/" + caminho).headers(h -> h.setBearerAuth(token))
                                .body(corpo).retrieve().toEntity(Map.class);
        }

        @Test
        void recuperacaoInvalidaTokenUsadoESessoesAntigas() {
                String sessao = entrar();
                assertEquals(204,
                                post("solicitar-alteracao-senha", Map.of("email", usuario.getEmail())).getStatusCode()
                                                .value());
                String token = recuperacoes.get(usuario.getEmail());
                var corpo = Map.of("token", token, "novaSenha", "NovaSenha123!", "confirmacaoSenha", "NovaSenha123!");
                assertEquals(200, post("alterar-senha", corpo).getStatusCode().value());
                assertEquals(400, post("alterar-senha", corpo).getStatusCode().value());
                assertEquals(401,
                                cliente.get().uri("/api/autenticacao/meus-dados").headers(h -> h.setBearerAuth(sessao))
                                                .retrieve().toBodilessEntity().getStatusCode().value());
                assertTrue(senhas.matches("NovaSenha123!",
                                usuarios.findById(usuario.getId()).orElseThrow().getSenha()));
        }

        @Test
        void codigo2faNaoPodeSerUsadoDuasVezes() {
                entrar();
                var resposta = post("confirmar-2fa",
                                Map.of("email", usuario.getEmail(), "codigo", codigos.get(usuario.getEmail())));
                assertEquals(400, resposta.getStatusCode().value());
        }

        @Test
        void contaInativaOuPendenteNaoInicia2fa() {
                usuario.setAtivo(false);
                usuario.setAceitouTermosLgpd(false);
                usuarios.save(usuario);
                assertEquals(401,
                                post("login", Map.of("email", usuario.getEmail(), "senha", "SenhaTeste123!"))
                                                .getStatusCode().value());
                usuario.setAtivo(true);
                usuario.setStatusAprovacao(StatusAprovacao.PENDENTE_APROVACAO);
                usuarios.save(usuario);
                assertEquals(403,
                                post("login", Map.of("email", usuario.getEmail(), "senha", "SenhaTeste123!"))
                                                .getStatusCode().value());
                verify(emails, never()).enviarCodigo2fa(any(), anyString());
        }

        @Test
        void recuperacaoExpiradaNaoTrocaSenhaERegistraFalha() {
                post("solicitar-alteracao-senha", Map.of("email", usuario.getEmail()));
                jdbc.update("update tb_tokens_email set expira_em = ? where usuario_id = ?",
                                LocalDateTime.now().minusMinutes(1), usuario.getId());
                assertEquals(400, post("alterar-senha", Map.of("token", recuperacoes.get(usuario.getEmail()),
                                "novaSenha", "NovaSenha123!", "confirmacaoSenha", "NovaSenha123!")).getStatusCode()
                                .value());
                assertTrue(senhas.matches("SenhaTeste123!",
                                usuarios.findById(usuario.getId()).orElseThrow().getSenha()));
                assertTrue(jdbc.queryForObject(
                                "select count(*) from tb_logs_auditoria where tipo_acao = 'RECUPERACAO_FALHOU'",
                                Long.class) > 0);
        }

        @Test
        void revogarConsentimentoEncerraSessaoEExigeNovoAceite() {
                String token = entrar();
                assertEquals(204,
                                cliente.post().uri("/api/autenticacao/revogar-termos")
                                                .headers(h -> h.setBearerAuth(token))
                                                .retrieve().toBodilessEntity().getStatusCode().value());
                assertEquals(401, cliente.get().uri("/api/autenticacao/meus-dados").headers(h -> h.setBearerAuth(token))
                                .retrieve().toBodilessEntity().getStatusCode().value());
                var login = autenticarAteTermos();
                assertEquals(403, login.getStatusCode().value());
                assertEquals("TERMOS_NAO_ACEITOS", login.getBody().get("codigo"));
        }

        @Test
        void excluirContaApagaCadastroESessoes() {
                String token = entrar();
                String codigo = tokensExclusao.criarExclusao(usuario);
                assertEquals(204,
                                cliente.method(org.springframework.http.HttpMethod.DELETE)
                                                .uri("/api/autenticacao/conta")
                                                .headers(h -> h.setBearerAuth(token)).body(Map.of("codigo", codigo))
                                                .retrieve().toBodilessEntity().getStatusCode().value());
                assertNotNull(usuarios.findById(usuario.getId()).orElseThrow().getExcluidoEm());
                assertEquals(401, cliente.get().uri("/api/autenticacao/meus-dados").headers(h -> h.setBearerAuth(token))
                                .retrieve().toBodilessEntity().getStatusCode().value());
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.EnumSource(value = Perfil.class, names = { "PROFESSOR", "ADMINISTRADOR" })
        void perfilElevadoExigeResponsabilidadeNoLoginENasSessoesExistentes(Perfil perfil) {
                usuario.setPerfil(perfil);
                usuarios.saveAndFlush(usuario);
                String sessaoAntiga = tokens.gerar(usuario);
                var bloqueio = cliente.get().uri("/api/autenticacao/meus-dados")
                                .headers(h -> h.setBearerAuth(sessaoAntiga)).retrieve().toEntity(Map.class);
                assertEquals(403, bloqueio.getStatusCode().value());
                assertEquals("TERMOS_REAUTENTICACAO", bloqueio.getBody().get("codigo"));
                var login = autenticarAteTermos();
                assertEquals(403, login.getStatusCode().value());
                String restrito = login.getBody().get("token").toString();
                assertEquals(200, cliente.get().uri("/api/politicas/responsabilidade/" + perfil)
                                .retrieve().toBodilessEntity().getStatusCode().value());
                assertEquals(400, cliente.post().uri("/api/autenticacao/aceitar-termos-novamente")
                                .headers(h -> h.setBearerAuth(restrito)).body(Map.of("versaoTermos", "teste-http"))
                                .retrieve().toBodilessEntity().getStatusCode().value());
                var confirmacao = Map.of("versaoTermos", "teste-http",
                                "aceitouResponsabilidade", true, "versaoResponsabilidade", "1.0");
                assertEquals(200, enviarDecisao("aceitar-termos-novamente", restrito, confirmacao).getStatusCode()
                                .value());
                assertEquals(401, enviarDecisao("aceitar-termos-novamente", restrito, confirmacao).getStatusCode()
                                .value());
                assertEquals(200, cliente.post().uri("/api/autenticacao/login")
                                .body(Map.of("email", usuario.getEmail(), "senha", "SenhaTeste123!"))
                                .retrieve().toBodilessEntity().getStatusCode().value());
        }

        @Test
        void logsSaoVerificaveis() {
                entrar();
                assertTrue(auditoria.verificarIntegridade());
                Long id = jdbc.queryForObject("select max(id) from tb_logs_auditoria", Long.class);
                String original = jdbc.queryForObject("select detalhes from tb_logs_auditoria where id = ?",
                                String.class, id);
                try {
                        jdbc.update("update tb_logs_auditoria set detalhes = 'adulterado' where id = ?", id);
                        assertFalse(auditoria.verificarIntegridade());
                } finally {
                        jdbc.update("update tb_logs_auditoria set detalhes = ? where id = ?", original, id);
                }
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.EnumSource(Perfil.class)
        void todosOsPerfisAlteramSenhaERevogamSessoes(Perfil perfil) {
                usuario.setPerfil(perfil);
                usuarios.save(usuario);
                if (perfil != Perfil.ALUNO)
                        responsabilidade.registrar(usuario, true, "1.0", "127.0.0.1");
                String outraSessao = tokens.gerar(usuario);
                String token = tokens.gerar(usuario);
                var resposta = cliente.post().uri("/api/autenticacao/alterar-senha-autenticado")
                                .headers(h -> h.setBearerAuth(token))
                                .body(Map.of("senhaAtual", "SenhaTeste123!", "novaSenha", "NovaSenha123!",
                                                "confirmacaoSenha", "NovaSenha123!"))
                                .retrieve().toBodilessEntity();
                assertEquals(204, resposta.getStatusCode().value());
                assertTrue(senhas.matches("NovaSenha123!",
                                usuarios.findById(usuario.getId()).orElseThrow().getSenha()));
                assertThrows(IllegalArgumentException.class, () -> tokens.extrairEmail(token));
                assertThrows(IllegalArgumentException.class, () -> tokens.extrairEmail(outraSessao));
        }

        @Test
        void alteracaoAutenticadaExigeSenhaAtualConfirmacaoEPolitica() {
                var valido = Map.of("senhaAtual", "SenhaTeste123!", "novaSenha", "NovaSenha123!", "confirmacaoSenha",
                                "NovaSenha123!");
                assertEquals(401, post("alterar-senha-autenticado", valido).getStatusCode().value());
                String token = tokens.gerar(usuario);
                for (var corpo : java.util.List.of(
                                Map.of("senhaAtual", "errada", "novaSenha", "NovaSenha123!", "confirmacaoSenha",
                                                "NovaSenha123!"),
                                Map.of("senhaAtual", "SenhaTeste123!", "novaSenha", "NovaSenha123!", "confirmacaoSenha",
                                                "Diferente123!"),
                                Map.of("senhaAtual", "SenhaTeste123!", "novaSenha", "A!", "confirmacaoSenha", "A!"))) {
                        var resposta = cliente.post().uri("/api/autenticacao/alterar-senha-autenticado")
                                        .headers(h -> h.setBearerAuth(token)).body(corpo).retrieve()
                                        .toEntity(String.class);
                        assertEquals(400, resposta.getStatusCode().value());
                        assertFalse(resposta.getBody().contains("8 caracteres"));
                        assertTrue(senhas.matches("SenhaTeste123!",
                                        usuarios.findById(usuario.getId()).orElseThrow().getSenha()));
                        assertDoesNotThrow(() -> tokens.extrairEmail(token));
                }
        }

        @Test
        void corsAutorizaPreflightApenasDeOrigemPermitida() {
                for (String origem : new String[] { "http://localhost:3000", "https://desconhecido.example" }) {
                        var resposta = cliente.method(HttpMethod.OPTIONS)
                                        .uri("/api/autenticacao/alterar-senha-autenticado")
                                        .header("Origin", origem).header("Access-Control-Request-Method", "POST")
                                        .header("Access-Control-Request-Headers", "authorization,content-type")
                                        .retrieve().toBodilessEntity();
                        assertEquals(origem.contains("localhost") ? 200 : 403, resposta.getStatusCode().value());
                        assertEquals(origem.contains("localhost") ? origem : null,
                                        resposta.getHeaders().getFirst("Access-Control-Allow-Origin"));
                }
        }
}
