package com.cursovalido.backend.authentication.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Sessao;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.SessaoRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class TokenJwtServiceTest {

    private TokenJwtService servico;
    private Usuario usuario;
    private SessaoRepository sessoes;
    private Map<String, Sessao> registros;

    @BeforeEach
    void configurarCenario() {
        // A chave é definida porque o teste não carrega o Spring.
        sessoes = org.mockito.Mockito.mock(SessaoRepository.class);
        servico = new TokenJwtService(sessoes);
        registros = new HashMap<>();
        org.mockito.Mockito.doAnswer(chamada -> { registros.values().forEach(Sessao::revogar); return null; })
                .when(sessoes).revogarTodas();
        org.mockito.Mockito.when(sessoes.save(org.mockito.ArgumentMatchers.any())).thenAnswer(chamada -> {
            Sessao sessao = chamada.getArgument(0);
            String id = (String) ReflectionTestUtils.getField(sessao, "id");
            registros.put(id, sessao);
            return sessao;
        });
        org.mockito.Mockito.when(sessoes.findById(org.mockito.ArgumentMatchers.anyString()))
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
    @ValueSource(strings = {"aluno@teste.com", "professor@teste.com"})
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
        org.junit.jupiter.api.Assertions.assertTrue(servico.apenasTermos(servico.gerarTermos(usuario)));
        org.junit.jupiter.api.Assertions.assertFalse(servico.apenasTermos(servico.gerar(usuario)));
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
        registros.put(id, new Sessao(id, 2L, java.time.LocalDateTime.now().plusHours(1)));
        assertThrows(IllegalArgumentException.class, () -> servico.extrairEmail(token));
    }

    @Test
    void respeitaExpiracaoDaSessaoPersistida() {
        String token = servico.gerar(usuario);
        String id = registros.keySet().iterator().next();
        registros.put(id, new Sessao(id, 1L, java.time.LocalDateTime.now().minusMinutes(1)));
        assertThrows(IllegalArgumentException.class, () -> servico.extrairEmail(token));
    }
}
