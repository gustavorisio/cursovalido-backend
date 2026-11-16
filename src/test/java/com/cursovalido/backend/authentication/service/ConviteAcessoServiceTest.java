package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.CadastroUsuarioRequest;
import com.cursovalido.backend.authentication.entity.ConviteAcesso;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.StatusAprovacao;
import com.cursovalido.backend.authentication.entity.StatusConvite;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.ConviteAcessoRepository;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
import com.cursovalido.backend.authentication.validation.email.ValidacaoEmailService;
import com.cursovalido.backend.forum.service.UsuarioForumService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConviteAcessoServiceTest {

    private static final String EMAIL = "professor@teste.com";
    private final ConviteAcessoRepository convites = mock(ConviteAcessoRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PasswordEncoder senhas = mock(PasswordEncoder.class);
    private final PoliticaService politicas = mock(PoliticaService.class);
    private final EmailService emails = mock(EmailService.class);
    private final UsuarioForumService forum = mock(UsuarioForumService.class);
    private final LimiteCadastroService limite = mock(LimiteCadastroService.class);
    private final ConviteAcessoService service = new ConviteAcessoService(convites, usuarios, senhas, politicas,
            mock(ResponsabilidadeService.class), emails, mock(ValidacaoEmailService.class),
            mock(ContaUsuarioService.class), mock(LogAuditoriaService.class), forum,
            mock(PrivilegiosAdministrativosService.class), limite);

    @Test
    void deveCancelarConviteAnteriorAoCriarOutroParaMesmoEmail() {
        ConviteAcesso anterior = convitePendente();
        when(convites.findByEmail(EMAIL)).thenReturn(Optional.of(anterior));

        service.criar(" Professor@teste.com ", Perfil.PROFESSOR, 1L);

        var captura = ArgumentCaptor.forClass(ConviteAcesso.class);
        verify(convites).saveAndFlush(captura.capture());
        ConviteAcesso novo = captura.getValue();
        assertEquals(StatusConvite.CANCELADO, anterior.getStatus());
        assertEquals(EMAIL, novo.getEmail());
        assertEquals(StatusConvite.PENDENTE, novo.getStatus());
        verify(emails).enviarConvite(eq(novo), anyString());
    }

    @Test
    void deveConsultarEConfirmarEmailDeConviteValido() {
        ConviteAcesso convite = convitePendente();
        when(convites.findByTokenHash(anyString())).thenReturn(Optional.of(convite));

        assertEquals(EMAIL, service.consultar("token").email());

        service.confirmarEmail("token");

        assertTrue(convite.isEmailConfirmado());
        verify(convites).save(convite);
    }

    @Test
    void deveCriarContaPendenteAoCompletarConvite() {
        ConviteAcesso convite = convitePendente();
        convite.setEmailConfirmado(true);
        when(convites.findByTokenHash(anyString())).thenReturn(Optional.of(convite));
        when(politicas.versaoAtiva("v1.0")).thenReturn(true);
        when(senhas.encode("Senha@123")).thenReturn("hash");
        var cadastro = new CadastroUsuarioRequest("Professor Teste", EMAIL, "Senha@123", "Senha@123",
                LocalDate.now().minusYears(30), true, true, "v1.0", true, "1.0");

        service.completar("token", cadastro, "192.0.2.10");

        var captura = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captura.capture());
        Usuario usuario = captura.getValue();
        assertEquals(EMAIL, usuario.getEmail());
        assertEquals("hash", usuario.getSenha());
        assertEquals(StatusAprovacao.PENDENTE_APROVACAO, usuario.getStatusAprovacao());
        assertFalse(usuario.isAtivo());
        assertEquals(StatusConvite.CONCLUIDO, convite.getStatus());
        verify(limite).reservar("192.0.2.10");
    }

    @Test
    void deveAtivarContaAoAprovarPendencia() {
        Usuario usuario = new Usuario();
        usuario.setStatusAprovacao(StatusAprovacao.PENDENTE_APROVACAO);
        usuario.setAtivo(false);
        when(usuarios.findById(2L)).thenReturn(Optional.of(usuario));
        when(usuarios.findAll()).thenReturn(List.of(usuario, new Usuario()));

        assertEquals(List.of(usuario), service.pendentes());

        service.aprovar(2L, 1L);

        assertTrue(usuario.isAtivo());
        assertEquals(StatusAprovacao.APROVADO, usuario.getStatusAprovacao());
        verify(forum).sincronizar(usuario);
    }

    @Test
    void deveRenovarTokenEReenviarConvitePendente() {
        ConviteAcesso convite = convitePendente();
        when(convites.findById(5L)).thenReturn(Optional.of(convite));

        service.reenviar(5L, 1L);

        assertNotEquals("hash-anterior", convite.getTokenHash());
        verify(convites).save(convite);
        verify(emails).enviarConvite(eq(convite), anyString());
    }

    @Test
    void deveCancelarConviteExistente() {
        ConviteAcesso convite = convitePendente();
        when(convites.findById(5L)).thenReturn(Optional.of(convite));

        service.cancelarConvite(5L, 1L);

        assertEquals(StatusConvite.CANCELADO, convite.getStatus());
        verify(convites).save(convite);
    }

    private ConviteAcesso convitePendente() {
        return new ConviteAcesso(EMAIL, Perfil.PROFESSOR, "hash-anterior", LocalDateTime.now().plusDays(1));
    }
}

