package com.cursovalido.backend.authentication.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cursovalido.backend.authentication.entity.*;
import com.cursovalido.backend.authentication.exception.LimiteReenvioException;
import com.cursovalido.backend.authentication.repository.TokenEmailRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class TokenEmailServiceTest {
    private final TokenEmailRepository repositorio = mock(TokenEmailRepository.class);
    private final TokenEmailService servico = new TokenEmailService(repositorio);

    @Test
    void deveConsumirCodigo2faValido() {
        Usuario usuario = new Usuario();
        usuario.setEmail("aluno@example.com");
        var registro = new TokenEmail("hash", usuario, "CODIGO_2FA", LocalDateTime.now().plusMinutes(5));
        when(repositorio.findByTokenHashAndFinalidade(anyString(), eq("CODIGO_2FA")))
                .thenReturn(Optional.of(registro));

        assertSame(usuario, servico.validarCodigo2fa("ALUNO@example.com", "123456"));
        assertTrue(registro.isUtilizado());
        verify(repositorio).save(registro);
    }

    @Test
    void deveGerarTokenDeRecuperacaoSemGuardarValorEmTexto() {
        Usuario usuario = new Usuario();
        String finalidade = servico.finalidadeAlteracaoSenha();

        String token = servico.criarAlteracaoSenha(usuario);

        var captura = ArgumentCaptor.forClass(TokenEmail.class);
        verify(repositorio).save(captura.capture());
        assertEquals(finalidade, captura.getValue().getFinalidade());
        assertFalse(token.isBlank());
        assertNotEquals(token, captura.getValue().getTokenHash());
        verify(repositorio).invalidarTokensAtivos(usuario.getId(), finalidade);
    }

    @Test
    void deveCriarCodigoDeExclusaoEInvalidarOAnterior() {
        Usuario usuario = new Usuario();

        String codigo = servico.criarExclusao(usuario);

        assertTrue(codigo.matches("[0-9]{6}"));
        var captura = ArgumentCaptor.forClass(TokenEmail.class);
        verify(repositorio).save(captura.capture());
        assertEquals("EXCLUSAO_CONTA", captura.getValue().getFinalidade());
        assertNotEquals(codigo, captura.getValue().getTokenHash());
        verify(repositorio).invalidarTokensAtivos(usuario.getId(), "EXCLUSAO_CONTA");
    }

    @Test
    void deveConsumirCodigoDeExclusaoValido() {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);
        var registro = new TokenEmail("hash", usuario, "EXCLUSAO_CONTA", LocalDateTime.now().plusMinutes(5));
        when(repositorio.findByTokenHashAndFinalidade(anyString(), eq("EXCLUSAO_CONTA")))
                .thenReturn(Optional.of(registro));

        servico.validarExclusao(1L, "123456");

        assertTrue(registro.isUtilizado());
        verify(repositorio).save(registro);
    }

    @Test
    void deveInvalidarLoginERecuperacaoAoRevogarAcessos() {
        Usuario usuario = new Usuario();

        servico.invalidarAcessos(usuario);

        verify(repositorio).invalidarTokensAtivos(usuario.getId(), "CODIGO_2FA");
        verify(repositorio).invalidarTokensAtivos(usuario.getId(), "ALTERACAO_SENHA");
    }

    @Test
    void mesmoCodigoParaDuasPessoasNaoGeraMesmoHash() {
        var aleatorio = mock(SecureRandom.class);
        when(aleatorio.nextInt(1_000_000)).thenReturn(123456);
        ReflectionTestUtils.setField(servico, "aleatorio", aleatorio);
        Usuario primeiro = new Usuario();
        primeiro.setEmail("primeiro@example.com");
        Usuario segundo = new Usuario();
        segundo.setEmail("segundo@example.com");
        assertEquals(servico.criarCodigo2fa(primeiro), servico.criarCodigo2fa(segundo));
        var captura = ArgumentCaptor.forClass(TokenEmail.class);
        verify(repositorio, times(2)).save(captura.capture());
        assertNotEquals(captura.getAllValues().get(0).getTokenHash(), captura.getAllValues().get(1).getTokenHash());
    }

    @Test
    void recuperacaoExpiradaNaoConsomeToken() {
        var registro = new TokenEmail("hash", new Usuario(), "ALTERACAO_SENHA", LocalDateTime.now().minusMinutes(1));
        when(repositorio.findByTokenHashAndFinalidade(anyString(), eq("ALTERACAO_SENHA")))
                .thenReturn(Optional.of(registro));
        assertThrows(IllegalArgumentException.class, () -> servico.validar("token", "ALTERACAO_SENHA"));
        assertFalse(registro.isUtilizado());
        verify(repositorio, never()).save(any());
    }

    @Test
    void limitaReenviosDeCodigo() {
        Usuario usuario = new Usuario();
        when(repositorio.countByUsuarioIdAndFinalidadeAndCriadoEmAfter(isNull(), eq("CODIGO_2FA"), any()))
                .thenReturn(3L);
        assertThrows(LimiteReenvioException.class,
                () -> servico.criarCodigo2fa(usuario));
        verify(repositorio, never()).save(any());
    }
}
