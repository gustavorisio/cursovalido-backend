package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Sessao;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.SessaoRepository;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenJwtServiceTest {

    private TokenJwtService servico;
    private Usuario usuario;
    private SessaoRepository sessoes;
    private UsuarioRepository usuarios;
    private Map<String, Sessao> registros;

    @BeforeEach
    void configurarCenario() {
        // A chave é definida porque o teste não carrega o Spring.
        sessoes = Mockito.mock(SessaoRepository.class);
        usuarios = Mockito.mock(UsuarioRepository.class);
        Mockito.when(usuarios.buscarParaAtualizar(ArgumentMatchers.anyLong()))
                .thenAnswer(chamada -> Optional.of(usuario));
        servico = new TokenJwtService(sessoes, usuarios);
        registros = new HashMap<>();
        Mockito.doAnswer(chamada -> {
            registros.values().forEach(Sessao::revogar);
            return null;
        })
                .when(sessoes).revogarPorUsuario(ArgumentMatchers.anyLong());
        Mockito.when(sessoes.save(ArgumentMatchers.any())).thenAnswer(chamada -> {
            Sessao sessao = chamada.getArgument(0);
            String id = (String) ReflectionTestUtils.getField(sessao, "id");
            registros.put(id, sessao);
            return sessao;
        });
        Mockito.when(sessoes.findById(ArgumentMatchers.anyString()))
                .thenAnswer(chamada -> Optional.ofNullable(registros.get(chamada.getArgument(0))));
        ReflectionTestUtils.setField(servico, "chaveTexto", "chave-de-teste-com-mais-de-32-caracteres");
        ReflectionTestUtils.setField(servico, "expiracaoMs", 86_400_000L);
        servico.prepararChave();

        usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);
        usuario.setEmail("aluno@teste.com");
        usuario.setPerfil(Perfil.ALUNO);
    }

    @ParameterizedTest
    @ValueSource(strings = { "aluno@teste.com", "professor@teste.com" })
    void deveGerarTokenComEmailInformado(String email) {
        usuario.setEmail(email);
        String token = servico.gerar(usuario);

        assertEquals(email, servico.extrairEmail(token));
    }

    @Test
    void deveExtrairEmailDeTokenValido() {
        String token = servico.gerar(usuario);

        assertEquals(usuario.getEmail(), servico.extrairEmail(token));
    }

    @Test
    void rejeitaTokenComPerfilAlteradoSemNovaAssinatura() {
        String[] partes = servico.gerar(usuario).split("\\.");
        var base64 = Base64.getUrlDecoder();
        String conteudo = new String(base64.decode(partes[1]), StandardCharsets.UTF_8)
                .replace("ALUNO", "ADMINISTRADOR");
        partes[1] = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(conteudo.getBytes(StandardCharsets.UTF_8));
        String adulterado = String.join(".", partes);

        assertThrows(io.jsonwebtoken.security.SignatureException.class,
                () -> servico.extrairEmail(adulterado));
    }

    @Test
    void rejeitaTokenAssinadoComOutraChave() {
        String token = servico.gerar(usuario);
        ReflectionTestUtils.setField(servico, "chaveTexto", "outra-chave-de-teste-com-mais-de-32-caracteres");
        servico.prepararChave();

        assertThrows(io.jsonwebtoken.security.SignatureException.class,
                () -> servico.extrairEmail(token));
    }

    @Test
    void impedeInicializarComChaveMenorQue256Bits() {
        ReflectionTestUtils.setField(servico, "chaveTexto", "chave-curta");
        assertThrows(io.jsonwebtoken.security.WeakKeyException.class, servico::prepararChave);
    }

    @Test
    void deveLancarExcecaoQuandoTokenEstiverExpirado() {
        ReflectionTestUtils.setField(servico, "expiracaoMs", -1L);
        String token = servico.gerar(usuario);

        assertThrows(RuntimeException.class, () -> servico.extrairEmail(token));
    }

    @Test
    void logoutImpedeReutilizarToken() {
        String token = servico.gerar(usuario);
        servico.encerrar(token);
        assertThrows(IllegalArgumentException.class, () -> servico.extrairEmail(token));
    }

    @Test
    void distingueTokenDeTermosDoTokenDeAcesso() {
        Assertions.assertTrue(servico.apenasTermos(servico.gerarTermos(usuario)));
        Assertions.assertFalse(servico.apenasTermos(servico.gerar(usuario)));
    }

    @Test
    void deveLancarExcecaoQuandoTokenEstiverVazio() {
        // Token sem conteúdo não deve ser aceito pelo parser.
        assertThrows(RuntimeException.class, () -> servico.extrairEmail(""));
    }

    @Test
    void reiniciarPreservaSessaoValidaComMesmaChave() {
        String anterior = servico.gerar(usuario);
        servico.prepararChave();
        assertEquals(usuario.getEmail(), servico.extrairEmail(anterior));
        assertEquals(usuario.getEmail(), servico.extrairEmail(servico.gerar(usuario)));
    }

    @Test
    void rejeitaTokenVinculadoASessaoDeOutroUsuario() {
        String token = servico.gerar(usuario);
        String id = registros.keySet().iterator().next();
        registros.put(id, new Sessao(id, 2L, LocalDateTime.now().plusHours(1)));
        assertThrows(IllegalArgumentException.class, () -> servico.extrairEmail(token));
    }

    @Test
    void respeitaExpiracaoDaSessaoPersistida() {
        String token = servico.gerar(usuario);
        String id = registros.keySet().iterator().next();
        registros.put(id, new Sessao(id, 1L, LocalDateTime.now().minusMinutes(1)));
        assertThrows(IllegalArgumentException.class, () -> servico.extrairEmail(token));
    }
}
