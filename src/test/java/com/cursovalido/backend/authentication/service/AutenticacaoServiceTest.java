package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import java.util.Optional;
import com.cursovalido.backend.authentication.dto.LoginRequest;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.StatusAprovacao;
import com.cursovalido.backend.authentication.exception.CredenciaisInvalidasException;
import com.cursovalido.backend.authentication.exception.AcessoRejeitadoException;
import com.cursovalido.backend.authentication.exception.PendenteAprovacaoException;
import com.cursovalido.backend.authentication.validation.login.ProtecaoForcaBrutaService;
import com.cursovalido.backend.forum.service.UsuarioForumService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AutenticacaoServiceTest {

    private static final String EMAIL = "aluno@teste.com";
    private static final String IP = "192.0.2.10";
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final TokenJwtService tokens = mock(TokenJwtService.class);
    private final TokenEmailService tokensEmail = mock(TokenEmailService.class);
    private final EnvioCodigo2faService envio = mock(EnvioCodigo2faService.class);
    private final PasswordEncoder senhas = mock(PasswordEncoder.class);
    private final LogAuditoriaService auditoria = mock(LogAuditoriaService.class);
    private final UsuarioForumService forum = mock(UsuarioForumService.class);
    private final ProtecaoForcaBrutaService protecao = mock(ProtecaoForcaBrutaService.class);
    private final PoliticaService politicas = mock(PoliticaService.class);
    private final ResponsabilidadeService responsabilidade = mock(ResponsabilidadeService.class);
    private final AutenticacaoService service = new AutenticacaoService(usuarios, tokens, tokensEmail, envio,
            senhas, auditoria, forum, protecao, mock(ModeracaoUsuarioService.class), politicas,
            responsabilidade, mock(PrivacidadeService.class));

    @Test
    void devePedirCodigoAposValidarSenha() {
        Usuario usuario = usuarioAprovado();
        when(usuarios.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(senhas.matches("senha", usuario.getSenha())).thenReturn(true);

        var resposta = service.autenticar(new LoginRequest(EMAIL, "senha"), IP);

        assertTrue(resposta.requerCodigo2fa());
        verify(envio).enviar(1L, IP, false);
    }

    @Test
    void deveRegistrarFalhaSemEnviarCodigoQuandoCredencialForInvalida() {
        assertThrows(CredenciaisInvalidasException.class,
                () -> service.autenticar(new LoginRequest(EMAIL, "errada"), IP));

        verify(protecao).registrarFalha(EMAIL);
        verifyNoInteractions(envio);
    }

    @Test
    void deveRecusarLoginDeContaPendente() {
        Usuario usuario = usuarioAprovado();
        usuario.setStatusAprovacao(StatusAprovacao.PENDENTE_APROVACAO);
        when(usuarios.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(senhas.matches("senha", usuario.getSenha())).thenReturn(true);

        assertThrows(PendenteAprovacaoException.class,
                () -> service.autenticar(new LoginRequest(EMAIL, "senha"), IP));

        verifyNoInteractions(envio);
    }

    @Test
    void deveRecusarLoginDeContaRejeitada() {
        Usuario usuario = usuarioAprovado();
        usuario.setStatusAprovacao(StatusAprovacao.REJEITADO);
        when(usuarios.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(senhas.matches("senha", usuario.getSenha())).thenReturn(true);

        assertThrows(AcessoRejeitadoException.class,
                () -> service.autenticar(new LoginRequest(EMAIL, "senha"), IP));

        verifyNoInteractions(envio);
    }

    @Test
    void deveConfirmarEmailELiberarAcessoComCodigoValido() {
        Usuario usuario = usuarioAprovado();
        usuario.setAceitouTermosLgpd(true);
        usuario.setVersaoTermos("v1.2");
        when(tokensEmail.validarCodigo2fa(EMAIL, "123456")).thenReturn(usuario);
        when(politicas.versaoAtiva("v1.2")).thenReturn(true);
        when(responsabilidade.atualizado(usuario)).thenReturn(true);
        when(tokens.gerar(usuario)).thenReturn("token");

        var resposta = service.confirmarCodigo(EMAIL, "123456", IP);

        assertEquals("token", resposta.token());
        assertTrue(usuario.isEmailConfirmado());
        verify(usuarios).save(usuario);
        verify(forum).sincronizar(usuario);
    }

    @Test
    void deveRegistrarFalhaQuandoCodigoForInvalido() {
        when(tokensEmail.validarCodigo2fa(EMAIL, "123456")).thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class, () -> service.confirmarCodigo(EMAIL, "123456", IP));

        verify(protecao).registrarFalha(EMAIL + ":2fa");
        verifyNoInteractions(tokens);
    }

    @Test
    void deveReenviarCodigoParaContaAprovada() {
        when(usuarios.findByEmail(EMAIL)).thenReturn(Optional.of(usuarioAprovado()));

        service.reenviarCodigo2fa(EMAIL, IP);

        verify(envio).enviar(1L, IP, true);
    }

    private Usuario usuarioAprovado() {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);
        usuario.setEmail(EMAIL);
        usuario.setSenha("hash");
        usuario.setPerfil(Perfil.ALUNO);
        usuario.setAtivo(true);
        usuario.setStatusAprovacao(StatusAprovacao.APROVADO);
        return usuario;
    }
}
