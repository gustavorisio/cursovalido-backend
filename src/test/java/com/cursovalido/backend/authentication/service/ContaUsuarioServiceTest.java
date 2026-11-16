package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import com.cursovalido.backend.authentication.entity.AceiteTermoArquivado;
import com.cursovalido.backend.authentication.entity.LogAuditoria;
import com.cursovalido.backend.authentication.entity.StatusAprovacao;
import com.cursovalido.backend.authentication.repository.AceiteTermoArquivadoRepository;
import com.cursovalido.backend.authentication.repository.LogAuditoriaRepository;
import com.cursovalido.backend.authentication.repository.SessaoRepository;
import com.cursovalido.backend.authentication.repository.TokenEmailRepository;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
import com.cursovalido.backend.forum.repository.UsuarioForumRepository;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContaUsuarioServiceTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final IdentidadeExcluidaService identidades = mock(IdentidadeExcluidaService.class);
    private final TokenEmailService tokens = mock(TokenEmailService.class);
    private final EmailService emails = mock(EmailService.class);
    private final TokenEmailRepository repositorioTokens = mock(TokenEmailRepository.class);
    private final LogAuditoriaRepository auditoria = mock(LogAuditoriaRepository.class);
    private final AceiteTermoArquivadoRepository aceites = mock(AceiteTermoArquivadoRepository.class);
    private final SessaoRepository sessoes = mock(SessaoRepository.class);
    private final ContaUsuarioService service = new ContaUsuarioService(usuarios, identidades,
            mock(LogAuditoriaService.class), tokens, emails, repositorioTokens, auditoria, aceites,
            mock(UsuarioForumRepository.class), sessoes, mock(PrivilegiosAdministrativosService.class));

    @Test
    void deveConsultarDadosComHistoricoDeAceiteEAuditoria() {
        Usuario usuario = cadastrarUsuario(Perfil.ALUNO);
        when(aceites.findByUsuarioIdOriginalOrderByDataHoraArquivamentoAsc(2L))
                .thenReturn(List.of(new AceiteTermoArquivado(2L, "v0.8", usuario.getDataHoraAceite(), "NOVA_VERSAO")));
        when(auditoria.findByUsuarioIdOrderByDataHoraAsc(2L))
                .thenReturn(List.of(new LogAuditoria(2L, "LOGIN", null, null, null, null)));

        var dados = service.consultarDados(2L);

        assertEquals(usuario.getEmail(), dados.email());
        assertEquals("v0.8", dados.historicoConsentimentos().getFirst().versao());
        assertEquals("LOGIN", dados.registrosAuditoria().getFirst().acao());
    }

    @Test
    void deveExportarDadosSemConsultarAuditoria() {
        Usuario usuario = cadastrarUsuario(Perfil.ALUNO);

        assertEquals(usuario.getEmail(), service.consultarDadosParaExportacao(2L).email());
        verifyNoInteractions(auditoria);
    }

    @Test
    void deveEnviarCodigoDeExclusaoParaOUsuario() {
        Usuario usuario = cadastrarUsuario(Perfil.ALUNO);
        when(tokens.criarExclusao(usuario)).thenReturn("123456");

        service.solicitarExclusao(2L);

        verify(emails).enviarExclusaoConta(usuario, "123456");
    }

    @Test
    void deveAnonimizarContaEArquivarAceiteAposValidarCodigo() {
        Usuario usuario = cadastrarUsuario(Perfil.ALUNO);

        service.excluirConta(2L, "123456");

        assertFalse(usuario.isAtivo());
        assertFalse(usuario.isEmailConfirmado());
        assertEquals("excluido-2@conta.invalid", usuario.getEmail());
        assertEquals("", usuario.getSenha());
        verify(tokens).validarExclusao(2L, "123456");
        verify(identidades).registrar(usuario);
        verify(aceites).save(any(AceiteTermoArquivado.class));
        verify(repositorioTokens).deleteByUsuarioId(2L);
        verify(sessoes).deleteByUsuarioId(2L);
    }

    @Test
    void devePermitirExclusaoAdministrativaDeProfessor() {
        Usuario usuario = cadastrarUsuario(Perfil.PROFESSOR);
        when(usuarios.buscarParaAtualizar(2L)).thenReturn(Optional.of(usuario));

        service.excluirPelaAdministracao(2L, 1L);

        assertNotNull(usuario.getExcluidoEm());
        verify(usuarios).saveAndFlush(usuario);
    }

    @Test
    void deveImpedirAutoexclusaoPeloPainel() {
        var erro = assertThrows(ResponseStatusException.class, () -> service.excluirPelaAdministracao(1L, 1L));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
        verifyNoInteractions(usuarios);
    }

    @Test
    void deveMarcarComoRejeitadaAoExcluirPendencia() {
        Usuario usuario = cadastrarUsuario(Perfil.PROFESSOR);
        usuario.setStatusAprovacao(StatusAprovacao.PENDENTE_APROVACAO);

        service.excluirPendencia(2L);

        assertEquals(StatusAprovacao.REJEITADO, usuario.getStatusAprovacao());
        assertNotNull(usuario.getExcluidoEm());
    }

    private Usuario cadastrarUsuario(Perfil perfil) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 2L);
        usuario.setPerfil(perfil);
        usuario.setEmail("usuario@teste.com");
        usuario.setEmailConfirmado(true);
        usuario.setVersaoTermos("v1.0");
        usuario.setDataHoraAceite(LocalDateTime.of(2025, 1, 1, 10, 0));
        when(usuarios.findById(2L)).thenReturn(Optional.of(usuario));
        return usuario;
    }
}

