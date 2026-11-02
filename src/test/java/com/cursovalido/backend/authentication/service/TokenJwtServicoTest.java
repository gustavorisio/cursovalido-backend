package com.cursovalido.backend.authentication.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class TokenJwtServicoTest {

    private TokenJwtServico servico;
    private Usuario usuario;

    @BeforeEach
    void configurarCenario() {
        // A chave é definida porque o teste não carrega o Spring.
        servico = new TokenJwtServico();
        ReflectionTestUtils.setField(servico, "chaveTexto", "chave-de-teste-com-mais-de-32-caracteres");
        ReflectionTestUtils.setField(servico, "expiracaoMs", 86_400_000L);
        servico.prepararChave();

        usuario = new Usuario();
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
    void deveLancarExcecaoQuandoTokenEstiverVazio() {
        // Token sem conteúdo não deve ser aceito pelo parser.
        assertThrows(RuntimeException.class, () -> servico.extrairEmail(""));
    }
}
