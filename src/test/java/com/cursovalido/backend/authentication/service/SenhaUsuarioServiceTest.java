package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.repository.SessaoRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.test.util.ReflectionTestUtils;
import com.cursovalido.backend.authentication.entity.TokenEmail;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SenhaUsuarioServiceTest {

    private static final String NOVA_SENHA = "SenhaNova@123";
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PasswordEncoder senhas = mock(PasswordEncoder.class);
    private final TokenEmailService tokens = mock(TokenEmailService.class);
    private final EmailService emails = mock(EmailService.class);
    private final SessaoRepository sessoes = mock(SessaoRepository.class);
    private final SenhaUsuarioService service = new SenhaUsuarioService(
            usuarios, senhas, mock(LogAuditoriaService.class), tokens, emails, sessoes);

    @Test
    void deveSalvarNovaSenhaECancelarSessoes() {
        Usuario usuario = usuarioAtivo();
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));
        when(senhas.matches("senha-atual", "hash-antigo")).thenReturn(true);
        when(senhas.encode(NOVA_SENHA)).thenReturn("hash-novo");

        service.alterarSenhaAutenticado(1L, "senha-atual", NOVA_SENHA, NOVA_SENHA);

        assertEquals("hash-novo", usuario.getSenha());
        verify(usuarios).save(usuario);
        verify(sessoes).revogarPorUsuario(1L);
        verify(tokens).invalidarAcessos(usuario);
    }

    @Test
    void deveEnviarLinkDeRecuperacaoParaContaAtiva() {
        Usuario usuario = usuarioAtivo();
        when(usuarios.findByEmail("aluno@teste.com")).thenReturn(Optional.of(usuario));
        when(tokens.criarAlteracaoSenha(usuario)).thenReturn("token");

        service.solicitarAlteracaoSenha("aluno@teste.com");

        verify(emails).enviarRedefinicaoSenha(usuario, "token");
    }

    @Test
    void deveAtualizarSenhaComLinkValido() {
        Usuario usuario = usuarioAtivo();
        var registro = new TokenEmail("hash", usuario, "ALTERACAO_SENHA", LocalDateTime.now().plusMinutes(10));
        when(tokens.finalidadeAlteracaoSenha()).thenReturn("ALTERACAO_SENHA");
        when(tokens.validar("token", "ALTERACAO_SENHA")).thenReturn(registro);
        when(senhas.encode(NOVA_SENHA)).thenReturn("hash-novo");

        service.alterarSenha("token", NOVA_SENHA, NOVA_SENHA);

        assertEquals("hash-novo", usuario.getSenha());
        verify(sessoes).revogarPorUsuario(1L);
    }

    @Test
    void deveRecusarRecuperacaoQuandoSenhasForemDiferentes() {
        assertThrows(IllegalArgumentException.class, () -> service.alterarSenha("token", NOVA_SENHA, "outra"));

        verifyNoInteractions(tokens, usuarios);
    }

    private Usuario usuarioAtivo() {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);
        usuario.setSenha("hash-antigo");
        return usuario;
    }
}

