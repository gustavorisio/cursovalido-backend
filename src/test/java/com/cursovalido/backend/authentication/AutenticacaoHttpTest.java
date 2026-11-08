package com.cursovalido.backend.authentication;

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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "security.bcrypt.strength=10")
@ActiveProfiles("test")
class AutenticacaoHttpTest {
    @Autowired private com.cursovalido.backend.authentication.service.TokenEmailService tokensExclusao;
    @LocalServerPort private int porta;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PoliticaRepository politicas;
    @Autowired private PasswordEncoder senhas;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private LogAuditoriaService auditoria;
    @Autowired private TokenJwtService tokens;
    @MockitoBean private EmailService emails;
    @MockitoBean private ValidacaoEmailService validacaoEmail;
    @MockitoBean private UsuarioForumService usuariosForum;
    private RestClient cliente;
    private Usuario usuario;
    private final Map<String, String> codigos = new ConcurrentHashMap<>();
    private final Map<String, String> recuperacoes = new ConcurrentHashMap<>();

    @BeforeEach
    void preparar() {
        cliente = RestClient.builder().baseUrl("http://localhost:" + porta)
                .defaultStatusHandler(status -> true, (request, response) -> {}).build();
        usuario = new Usuario();
        usuario.setNomeCompleto("Aluno Teste");
        usuario.setEmail(UUID.randomUUID() + "@example.com");
        usuario.setSenha(senhas.encode("SenhaTeste123!"));
        // Cada teste recebe um CPF diferente para manter a restricao de unicidade.
        usuario.setCpf(String.format("%011d", Math.abs(UUID.randomUUID().getMostSignificantBits() % 100_000_000_000L)));
        usuario.setMatricula(UUID.randomUUID().toString());
        usuario.setDataNascimento(LocalDate.of(1990, 1, 1));
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
            assertEquals(200, cliente.get().uri("/api/autenticacao/meus-dados").headers(h -> h.setBearerAuth(anterior))
                    .retrieve().toBodilessEntity().getStatusCode().value());
        }
        tokens.prepararChave();
        assertEquals(200, cliente.get().uri("/api/autenticacao/meus-dados").headers(h -> h.setBearerAuth(anterior))
                .retrieve().toBodilessEntity().getStatusCode().value());
        cliente.post().uri("/api/autenticacao/logout").headers(h -> h.setBearerAuth(anterior))
                .retrieve().toBodilessEntity();
        assertEquals(401, cliente.get().uri("/api/autenticacao/meus-dados").headers(h -> h.setBearerAuth(anterior))
                .retrieve().toBodilessEntity().getStatusCode().value());
        String novo = entrar();
        assertEquals(200, cliente.get().uri("/api/autenticacao/meus-dados").headers(h -> h.setBearerAuth(novo))
                .retrieve().toBodilessEntity().getStatusCode().value());
    }

    private ResponseEntity<Map> post(String caminho, Map<String, ?> corpo) {
        return cliente.post().uri("/api/autenticacao/" + caminho).body(corpo).retrieve().toEntity(Map.class);
    }

    private String entrar() {
        var primeiraEtapa = post("login", Map.of("email", usuario.getEmail(), "senha", "SenhaTeste123!"));
        assertEquals(200, primeiraEtapa.getStatusCode().value());
        assertNull(primeiraEtapa.getBody().get("token"));
        var segundaEtapa = post("confirmar-2fa", Map.of("email", usuario.getEmail(), "codigo", codigos.get(usuario.getEmail())));
        assertEquals(200, segundaEtapa.getStatusCode().value());
        assertEquals("ALUNO", segundaEtapa.getBody().get("perfil"));
        return (String) segundaEtapa.getBody().get("token");
    }

    @Test
    void logoutInvalidaJwtEExportacaoNaoExibeSegredos() {
        String token = entrar();
        var dados = cliente.get().uri("/api/autenticacao/exportar-dados")
                .headers(h -> h.setBearerAuth(token)).retrieve().toEntity(Map.class);
        assertEquals(200, dados.getStatusCode().value());
        assertEquals(usuario.getEmail(), dados.getBody().get("email"));
        assertFalse(dados.getBody().containsKey("senha"));
        assertFalse(dados.getBody().containsKey("cpfHash"));
        assertTrue(dados.getHeaders().getFirst("Content-Disposition").contains("meus-dados.json"));
        assertEquals(204, cliente.post().uri("/api/autenticacao/logout").headers(h -> h.setBearerAuth(token))
                .retrieve().toBodilessEntity().getStatusCode().value());
        assertEquals(401, cliente.get().uri("/api/autenticacao/meus-dados").headers(h -> h.setBearerAuth(token))
                .retrieve().toBodilessEntity().getStatusCode().value());
    }

    @Test
    void naoEnviaCodigoSemSenhaCorretaOuSemConfirmacaoDoEmail() {
        assertEquals(401, post("login", Map.of("email", usuario.getEmail(), "senha", "incorreta")).getStatusCode().value());
        assertEquals(204, post("reenviar-2fa", Map.of("email", usuario.getEmail())).getStatusCode().value());
        usuario.setEmailConfirmado(false);
        usuarios.save(usuario);
        assertEquals(403, post("login", Map.of("email", usuario.getEmail(), "senha", "SenhaTeste123!")).getStatusCode().value());
        verify(emails, never()).enviarCodigo2fa(any(), anyString());
    }

    @Test
    void tokenDeTermosNaoViraTokenDeAcessoDepoisDoAceite() {
        usuario.setAceitouTermosLgpd(false);
        usuarios.save(usuario);
        var login = post("login", Map.of("email", usuario.getEmail(), "senha", "SenhaTeste123!"));
        assertEquals(403, login.getStatusCode().value());
        String token = (String) login.getBody().get("token");
        assertNotNull(token);
        assertEquals(403, cliente.get().uri("/api/autenticacao/meus-dados").headers(h -> h.setBearerAuth(token))
                .retrieve().toBodilessEntity().getStatusCode().value());
        assertEquals(204, cliente.post().uri("/api/autenticacao/aceitar-termos-novamente")
                .headers(h -> h.setBearerAuth(token)).body(Map.of("versaoTermos", "teste-http"))
                .retrieve().toBodilessEntity().getStatusCode().value());
        assertEquals(401, cliente.get().uri("/api/autenticacao/meus-dados").headers(h -> h.setBearerAuth(token))
                .retrieve().toBodilessEntity().getStatusCode().value());
        assertNotNull(entrar());
    }

    @Test
    void recuperacaoInvalidaTokenUsadoESessoesAntigas() {
        String sessao = entrar();
        assertEquals(204, post("solicitar-alteracao-senha", Map.of("email", usuario.getEmail())).getStatusCode().value());
        String token = recuperacoes.get(usuario.getEmail());
        var corpo = Map.of("token", token, "novaSenha", "NovaSenha123!", "confirmacaoSenha", "NovaSenha123!");
        assertEquals(200, post("alterar-senha", corpo).getStatusCode().value());
        assertEquals(400, post("alterar-senha", corpo).getStatusCode().value());
        assertEquals(401, cliente.get().uri("/api/autenticacao/meus-dados").headers(h -> h.setBearerAuth(sessao))
                .retrieve().toBodilessEntity().getStatusCode().value());
        assertTrue(senhas.matches("NovaSenha123!", usuarios.findById(usuario.getId()).orElseThrow().getSenha()));
    }

    @Test
    void codigo2faNaoPodeSerUsadoDuasVezes() {
        entrar();
        var resposta = post("confirmar-2fa", Map.of("email", usuario.getEmail(), "codigo", codigos.get(usuario.getEmail())));
        assertEquals(400, resposta.getStatusCode().value());
    }

    @Test
    void contaInativaOuPendenteNaoInicia2fa() {
        usuario.setAtivo(false);
        usuario.setAceitouTermosLgpd(false);
        usuarios.save(usuario);
        assertEquals(401, post("login", Map.of("email", usuario.getEmail(), "senha", "SenhaTeste123!")).getStatusCode().value());
        usuario.setAtivo(true);
        usuario.setStatusAprovacao(StatusAprovacao.PENDENTE_APROVACAO);
        usuarios.save(usuario);
        assertEquals(403, post("login", Map.of("email", usuario.getEmail(), "senha", "SenhaTeste123!")).getStatusCode().value());
        verify(emails, never()).enviarCodigo2fa(any(), anyString());
    }

    @Test
    void recuperacaoExpiradaNaoTrocaSenhaERegistraFalha() {
        post("solicitar-alteracao-senha", Map.of("email", usuario.getEmail()));
        jdbc.update("update tb_tokens_email set expira_em = ? where usuario_id = ?",
                LocalDateTime.now().minusMinutes(1), usuario.getId());
        assertEquals(400, post("alterar-senha", Map.of("token", recuperacoes.get(usuario.getEmail()),
                "novaSenha", "NovaSenha123!", "confirmacaoSenha", "NovaSenha123!")).getStatusCode().value());
        assertTrue(senhas.matches("SenhaTeste123!", usuarios.findById(usuario.getId()).orElseThrow().getSenha()));
        assertTrue(jdbc.queryForObject("select count(*) from tb_logs_auditoria where tipo_acao = 'RECUPERACAO_FALHOU'", Long.class) > 0);
    }

    @Test
    void revogarConsentimentoEncerraSessaoEExigeNovoAceite() {
        String token = entrar();
        assertEquals(204, cliente.post().uri("/api/autenticacao/revogar-termos").headers(h -> h.setBearerAuth(token))
                .retrieve().toBodilessEntity().getStatusCode().value());
        assertEquals(401, cliente.get().uri("/api/autenticacao/meus-dados").headers(h -> h.setBearerAuth(token))
                .retrieve().toBodilessEntity().getStatusCode().value());
        var login = post("login", Map.of("email", usuario.getEmail(), "senha", "SenhaTeste123!"));
        assertEquals(403, login.getStatusCode().value());
        assertEquals("TERMOS_NAO_ACEITOS", login.getBody().get("codigo"));
    }

    @Test
    void excluirContaApagaCadastroESessoes() {
        String token = entrar();
        String codigo = tokensExclusao.criarExclusao(usuario);
        assertEquals(204, cliente.method(org.springframework.http.HttpMethod.DELETE).uri("/api/autenticacao/conta")
                .headers(h -> h.setBearerAuth(token)).body(Map.of("codigo", codigo))
                .retrieve().toBodilessEntity().getStatusCode().value());
        assertTrue(usuarios.findById(usuario.getId()).isEmpty());
        assertEquals(401, cliente.get().uri("/api/autenticacao/meus-dados").headers(h -> h.setBearerAuth(token))
                .retrieve().toBodilessEntity().getStatusCode().value());
    }

    @Test
    void cpfFicaCifradoELogsSaoVerificaveis() {
        var dados = jdbc.queryForMap("select cpf from tb_usuarios where id = ?", usuario.getId());
        assertTrue(usuarios.existsByCpf(usuario.getCpf()));
        assertNotEquals(usuario.getCpf(), dados.get("CPF"));
        entrar();
        assertTrue(auditoria.verificarIntegridade());
        Long id = jdbc.queryForObject("select max(id) from tb_logs_auditoria", Long.class);
        String original = jdbc.queryForObject("select detalhes from tb_logs_auditoria where id = ?", String.class, id);
        try {
            jdbc.update("update tb_logs_auditoria set detalhes = 'adulterado' where id = ?", id);
            assertFalse(auditoria.verificarIntegridade());
        } finally {
            jdbc.update("update tb_logs_auditoria set detalhes = ? where id = ?", original, id);
        }
    }
}
