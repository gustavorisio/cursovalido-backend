package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import java.util.Optional;
import com.cursovalido.backend.authentication.entity.StatusAprovacao;
import com.cursovalido.backend.authentication.entity.TokenEmail;
import com.cursovalido.backend.authentication.exception.LimiteReenvioException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnvioCodigo2faServiceTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final TokenEmailService tokens = mock(TokenEmailService.class);
    private final EmailService emails = mock(EmailService.class);
    private final LogAuditoriaService auditoria = mock(LogAuditoriaService.class);
    private final EnvioCodigo2faService service = new EnvioCodigo2faService(usuarios, tokens, emails, auditoria);

    @Test
    void deveEnviarPrimeiroCodigoPorEmail() {
        Usuario usuario = cadastrarUsuario();
        when(tokens.criarCodigo2fa(usuario)).thenReturn("123456");

        service.enviar(1L, "192.0.2.10", false);

        verify(emails).enviarCodigo2fa(usuario, "123456");
        verify(auditoria).registrar(eq(1L), eq("CODIGO_2FA_ENVIADO"), anyString(), eq("192.0.2.10"));
    }

    @Test
    void deveManterCodigoValidoDuranteNovoLogin() {
        Usuario usuario = cadastrarUsuario();
        TokenEmail token = new TokenEmail("hash", usuario, "CODIGO_2FA", LocalDateTime.now().plusMinutes(5));
        when(tokens.ultimoCodigo2fa(usuario)).thenReturn(Optional.of(token));

        service.enviar(1L, null, false);

        verify(tokens, never()).criarCodigo2fa(usuario);
        verifyNoInteractions(emails);
    }

    @Test
    void novoLoginDepoisDeUmMinutoEnviaCodigoNovo() {
        Usuario usuario = cadastrarUsuario();
        TokenEmail token = new TokenEmail("hash", usuario, "CODIGO_2FA", LocalDateTime.now().plusMinutes(5));
        ReflectionTestUtils.setField(token, "criadoEm", LocalDateTime.now().minusSeconds(61));
        when(tokens.ultimoCodigo2fa(usuario)).thenReturn(Optional.of(token));
        when(tokens.criarCodigo2fa(usuario)).thenReturn("654321");

        service.enviar(1L, null, false);

        verify(emails).enviarCodigo2fa(usuario, "654321");
    }

    @Test
    void novoLoginDepoisDeUsarOCodigoEnviaCodigoNovo() {
        Usuario usuario = cadastrarUsuario();
        TokenEmail token = new TokenEmail("hash", usuario, "CODIGO_2FA", LocalDateTime.now().plusMinutes(5));
        token.setUtilizado(true);
        when(tokens.ultimoCodigo2fa(usuario)).thenReturn(Optional.of(token));
        when(tokens.criarCodigo2fa(usuario)).thenReturn("654321");

        service.enviar(1L, null, false);

        verify(emails).enviarCodigo2fa(usuario, "654321");
    }

    @Test
    void deveAguardarIntervaloMinimoEntreEnvios() {
        Usuario usuario = cadastrarUsuario();
        TokenEmail token = new TokenEmail("hash", usuario, "CODIGO_2FA", LocalDateTime.now().plusMinutes(5));
        when(tokens.ultimoCodigo2fa(usuario)).thenReturn(Optional.of(token));

        assertThrows(LimiteReenvioException.class, () -> service.enviar(1L, null, true));

        verifyNoInteractions(emails);
    }

    @Test
    void deveIgnorarEnvioParaContaInativa() {
        Usuario usuario = cadastrarUsuario();
        usuario.setAtivo(false);

        service.enviar(1L, null, false);

        verifyNoInteractions(tokens, emails);
    }

    private Usuario cadastrarUsuario() {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);
        usuario.setAtivo(true);
        usuario.setStatusAprovacao(StatusAprovacao.APROVADO);
        when(usuarios.buscarParaAtualizar(1L)).thenReturn(Optional.of(usuario));
        return usuario;
    }
}
