package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.repository.SessaoRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.test.util.ReflectionTestUtils;
import com.cursovalido.backend.authentication.dto.AceiteTermosRequest;
import com.cursovalido.backend.authentication.entity.AceiteTermoArquivado;
import com.cursovalido.backend.authentication.exception.CredenciaisInvalidasException;
import com.cursovalido.backend.authentication.repository.AceiteTermoArquivadoRepository;
import com.cursovalido.backend.forum.service.UsuarioForumService;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TermosUsuarioServiceTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final AceiteTermoArquivadoRepository aceites = mock(AceiteTermoArquivadoRepository.class);
    private final PoliticaService politicas = mock(PoliticaService.class);
    private final SessaoRepository sessoes = mock(SessaoRepository.class);
    private final TokenEmailService tokensEmail = mock(TokenEmailService.class);
    private final TokenJwtService tokens = mock(TokenJwtService.class);
    private final TermosUsuarioService service = new TermosUsuarioService(usuarios, aceites,
            mock(UsuarioForumService.class), politicas, mock(ResponsabilidadeService.class),
            mock(LogAuditoriaService.class), sessoes, tokensEmail, tokens);

    @Test
    void deveArquivarAceiteAnteriorAoConfirmarNovaVersao() {
        Usuario usuario = cadastrarUsuario();
        when(politicas.versaoAtiva("v1.2")).thenReturn(true);
        when(tokens.apenasTermos("token-termos")).thenReturn(true);
        when(tokens.gerar(usuario)).thenReturn("token-acesso");

        var resposta = service.confirmarAceite(1L, "token-termos", new AceiteTermosRequest("v1.2"), null);

        assertEquals("token-acesso", resposta.token());
        assertEquals("v1.2", usuario.getVersaoTermos());
        var captura = ArgumentCaptor.forClass(AceiteTermoArquivado.class);
        verify(aceites).save(captura.capture());
        assertEquals("v1.0", captura.getValue().getVersao());
    }

    @Test
    void deveRevogarSessoesEAcessosAoRevogarTermos() {
        Usuario usuario = cadastrarUsuario();

        service.revogarTermos(1L);

        assertFalse(usuario.getAceitouTermosLgpd());
        verify(sessoes).revogarPorUsuario(1L);
        verify(tokensEmail).invalidarAcessos(usuario);
    }

    @Test
    void deveEncerrarSessaoAposRecusa() {
        cadastrarUsuario();

        service.encerrarPorDecisao(1L, "token", false, null);

        verify(tokens).encerrar("token");
    }

    @Test
    void deveRecusarConfirmacaoComTokenDeAcesso() {
        cadastrarUsuario();

        assertThrows(CredenciaisInvalidasException.class,
                () -> service.confirmarAceite(1L, "token-acesso", new AceiteTermosRequest("v1.2"), null));

        verify(usuarios, never()).save(any());
    }

    private Usuario cadastrarUsuario() {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);
        usuario.setAceitouTermosLgpd(true);
        usuario.setVersaoTermos("v1.0");
        usuario.setDataHoraAceite(LocalDateTime.of(2025, 1, 1, 10, 0));
        when(usuarios.buscarParaAtualizar(1L)).thenReturn(Optional.of(usuario));
        return usuario;
    }
}
