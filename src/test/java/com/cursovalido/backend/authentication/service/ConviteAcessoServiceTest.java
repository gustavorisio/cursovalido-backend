package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.CadastroUsuarioRequest;
import com.cursovalido.backend.authentication.entity.ConviteAcesso;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.StatusAprovacao;
import com.cursovalido.backend.authentication.entity.StatusConvite;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.exception.ConviteAcessoInvalidoException;
import com.cursovalido.backend.authentication.repository.ConviteAcessoRepository;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
import com.cursovalido.backend.authentication.validation.convite.ConviteValidation;
import com.cursovalido.backend.authentication.validation.email.ValidacaoEmailService;
import com.cursovalido.backend.forum.service.UsuarioForumService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

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
    private final TokenEmailService tokensEmail = mock(TokenEmailService.class);
    private final ConviteAcessoService service = new ConviteAcessoService(convites, usuarios, senhas, politicas,
            mock(ResponsabilidadeService.class), emails, mock(ValidacaoEmailService.class),
            mock(ContaUsuarioService.class), mock(LogAuditoriaService.class), forum,
            mock(PrivilegiosAdministrativosService.class), tokensEmail);

    @Test
    void deveCriarConviteNormalizandoEmailEEnviarUmUnicoLink() {
        service.criar(" Professor@teste.com ", Perfil.PROFESSOR, 1L);

        var captura = ArgumentCaptor.forClass(ConviteAcesso.class);
        verify(convites).saveAndFlush(captura.capture());
        ConviteAcesso novo = captura.getValue();
        assertEquals(EMAIL, novo.getEmail());
        assertEquals(StatusConvite.PENDENTE, novo.getStatus());
        assertEquals(Long.valueOf(1L), novo.getCriadoPor());
        assertEquals(1, novo.getQuantidadeEnvios());
        verify(emails, times(1)).enviarConvite(eq(novo), anyString());
    }

    @Test
    void segundoCliqueComConvitePendenteNaoGeraOutroConviteNemEmail() {
        ConviteAcesso pendente = convitePendente();
        when(convites.findFirstByEmailAndStatusOrderByIdDesc(EMAIL, StatusConvite.PENDENTE))
                .thenReturn(Optional.of(pendente));

        var erro = assertThrows(ConviteAcessoInvalidoException.class,
                () -> service.criar(EMAIL, Perfil.PROFESSOR, 1L));

        assertEquals(HttpStatus.CONFLICT, erro.getStatus());
        assertEquals("CONVITE_JA_PENDENTE", erro.getCodigo());
        assertEquals(StatusConvite.PENDENTE, pendente.getStatus());
        verify(convites, never()).saveAndFlush(any());
        verifyNoInteractions(emails);
    }

    @Test
    void convitePendenteExpiradoECanceladoAntesDeCriarOutro() {
        ConviteAcesso expirado = new ConviteAcesso(EMAIL, Perfil.PROFESSOR, "hash-antigo",
                LocalDateTime.now().minusMinutes(1));
        when(convites.findFirstByEmailAndStatusOrderByIdDesc(EMAIL, StatusConvite.PENDENTE))
                .thenReturn(Optional.of(expirado));

        service.criar(EMAIL, Perfil.ADMINISTRADOR, 1L);

        assertEquals(StatusConvite.CANCELADO, expirado.getStatus());
        var captura = ArgumentCaptor.forClass(ConviteAcesso.class);
        verify(convites, times(2)).saveAndFlush(captura.capture());
        assertSame(expirado, captura.getAllValues().get(0));
        ConviteAcesso novo = captura.getAllValues().get(1);
        assertEquals(Perfil.ADMINISTRADOR, novo.getPerfil());
        verify(emails).enviarConvite(eq(novo), anyString());
    }

    @Test
    void requisicoesSimultaneasBarradasPeloBancoViramConflitoSemEmail() {
        when(convites.saveAndFlush(any(ConviteAcesso.class)))
                .thenThrow(new DataIntegrityViolationException("uk_convites_pendente_por_email"));

        var erro = assertThrows(ConviteAcessoInvalidoException.class,
                () -> service.criar(EMAIL, Perfil.PROFESSOR, 1L));

        assertEquals("CONVITE_JA_PENDENTE", erro.getCodigo());
        verifyNoInteractions(emails);
    }

    @Test
    void limitaQuantidadeDeConvitesPorAdministradorNaUltimaHora() {
        when(convites.countByCriadoPorAndCriadoEmAfter(eq(1L), any(LocalDateTime.class)))
                .thenReturn((long) ConviteValidation.MAXIMO_CONVITES_POR_HORA);

        var erro = assertThrows(ConviteAcessoInvalidoException.class,
                () -> service.criar(EMAIL, Perfil.PROFESSOR, 1L));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, erro.getStatus());
        assertEquals("LIMITE_CONVITES", erro.getCodigo());
        verify(convites, never()).saveAndFlush(any());
        verifyNoInteractions(emails);
    }

    @Test
    void recusaConviteParaAlunoOuEmailComConta() {
        var perfil = assertThrows(ConviteAcessoInvalidoException.class,
                () -> service.criar(EMAIL, Perfil.ALUNO, 1L));
        assertEquals("PERFIL_CONVITE_INVALIDO", perfil.getCodigo());

        when(usuarios.existsByEmail(EMAIL)).thenReturn(true);
        var conta = assertThrows(ConviteAcessoInvalidoException.class,
                () -> service.criar(EMAIL, Perfil.PROFESSOR, 1L));
        assertEquals("EMAIL_JA_CADASTRADO", conta.getCodigo());
        verifyNoInteractions(emails);
    }

    @Test
    void deveConsultarConviteValido() {
        when(convites.findByTokenHash(anyString())).thenReturn(Optional.of(convitePendente()));

        var dados = service.consultar("token");

        assertEquals(EMAIL, dados.email());
        assertEquals(Perfil.PROFESSOR, dados.perfil());
    }

    @Test
    void cadastroPeloLinkCriaContaAtivaEAprovadaEConcluiOConvite() {
        ConviteAcesso convite = convitePendente();
        when(convites.findByTokenHash(anyString())).thenReturn(Optional.of(convite));
        when(politicas.versaoAtiva("v1.0")).thenReturn(true);
        when(senhas.encode("Senha@123")).thenReturn("hash");

        service.completar("token", cadastro(true), "192.0.2.10");

        var captura = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).saveAndFlush(captura.capture());
        Usuario usuario = captura.getValue();
        assertEquals(EMAIL, usuario.getEmail());
        assertEquals("hash", usuario.getSenha());
        assertEquals(Perfil.PROFESSOR, usuario.getPerfil());
        assertEquals(StatusAprovacao.APROVADO, usuario.getStatusAprovacao());
        assertTrue(usuario.isAtivo());
        assertTrue(usuario.isEmailConfirmado());
        assertTrue(usuario.getDeclarouMaiorIdade());
        assertEquals(StatusConvite.CONCLUIDO, convite.getStatus());
        assertTrue(convite.isEmailConfirmado());
        verify(forum).sincronizar(usuario);
    }

    @Test
    void cadastroPeloLinkNaoExigeConfirmacaoPreviaDoEmail() {
        ConviteAcesso convite = convitePendente();
        assertFalse(convite.isEmailConfirmado());
        when(convites.findByTokenHash(anyString())).thenReturn(Optional.of(convite));
        when(politicas.versaoAtiva("v1.0")).thenReturn(true);

        assertDoesNotThrow(() -> service.completar("token", cadastro(true), null));
    }

    @Test
    void linkJaUtilizadoNaoCriaSegundaConta() {
        ConviteAcesso convite = convitePendente();
        convite.concluir();
        when(convites.findByTokenHash(anyString())).thenReturn(Optional.of(convite));

        assertThrows(IllegalArgumentException.class, () -> service.completar("token", cadastro(true), null));
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void linkExpiradoNaoPermiteCadastro() {
        ConviteAcesso expirado = new ConviteAcesso(EMAIL, Perfil.PROFESSOR, "hash",
                LocalDateTime.now().minusSeconds(1));
        when(convites.findByTokenHash(anyString())).thenReturn(Optional.of(expirado));

        assertThrows(IllegalArgumentException.class, () -> service.completar("token", cadastro(true), null));
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void cadastroPorConviteExigeDeclaracaoDeMaioridade() {
        assertThrows(IllegalArgumentException.class, () -> service.completar("token", cadastro(false), null));
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void deveAtivarContaAoAprovarPendenciaAntiga() {
        Usuario usuario = new Usuario();
        usuario.setStatusAprovacao(StatusAprovacao.PENDENTE_APROVACAO);
        usuario.setAtivo(false);
        when(usuarios.findById(2L)).thenReturn(Optional.of(usuario));
        when(usuarios.findAll()).thenReturn(List.of(usuario, new Usuario()));

        assertEquals(List.of(usuario), service.pendentes());

        service.aprovar(2L, 1L);

        assertTrue(usuario.isAtivo());
        assertEquals(StatusAprovacao.APROVADO, usuario.getStatusAprovacao());
        verify(tokensEmail).invalidarAcessos(usuario);
        verify(forum).sincronizar(usuario);
    }

    @Test
    void deveRenovarTokenEReenviarDepoisDoIntervalo() {
        ConviteAcesso convite = convitePendente();
        ReflectionTestUtils.setField(convite, "enviadoEm",
                LocalDateTime.now().minusSeconds(ConviteValidation.SEGUNDOS_ENTRE_ENVIOS + 1L));
        when(convites.bloquearPorId(5L)).thenReturn(Optional.of(convite));

        service.reenviar(5L, 1L);

        assertNotEquals("hash-anterior", convite.getTokenHash());
        assertEquals(2, convite.getQuantidadeEnvios());
        verify(convites).save(convite);
        verify(emails).enviarConvite(eq(convite), anyString());
    }

    @Test
    void reenvioLogoAposOEnvioERecusadoSemEmail() {
        ConviteAcesso convite = convitePendente();
        when(convites.bloquearPorId(5L)).thenReturn(Optional.of(convite));

        var erro = assertThrows(ConviteAcessoInvalidoException.class, () -> service.reenviar(5L, 1L));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, erro.getStatus());
        assertEquals("REENVIO_CONVITE_AGUARDE", erro.getCodigo());
        assertEquals("hash-anterior", convite.getTokenHash());
        verifyNoInteractions(emails);
    }

    @Test
    void reenvioRespeitaLimiteDeEnviosPorConvite() {
        ConviteAcesso convite = convitePendente();
        ReflectionTestUtils.setField(convite, "quantidadeEnvios", ConviteValidation.MAXIMO_ENVIOS_POR_CONVITE);
        ReflectionTestUtils.setField(convite, "enviadoEm", LocalDateTime.now().minusHours(1));
        when(convites.bloquearPorId(5L)).thenReturn(Optional.of(convite));

        var erro = assertThrows(ConviteAcessoInvalidoException.class, () -> service.reenviar(5L, 1L));

        assertEquals("LIMITE_REENVIO_CONVITE", erro.getCodigo());
        verifyNoInteractions(emails);
    }

    @Test
    void deveCancelarConvitePendente() {
        ConviteAcesso convite = convitePendente();
        when(convites.bloquearPorId(5L)).thenReturn(Optional.of(convite));

        service.cancelarConvite(5L, 1L);

        assertEquals(StatusConvite.CANCELADO, convite.getStatus());
        verify(convites).save(convite);
    }

    @Test
    void cancelarDuasVezesNaoAlteraConviteJaCancelado() {
        ConviteAcesso convite = convitePendente();
        convite.setStatus(StatusConvite.CANCELADO);
        when(convites.bloquearPorId(5L)).thenReturn(Optional.of(convite));

        assertThrows(IllegalArgumentException.class, () -> service.cancelarConvite(5L, 1L));
        verify(convites, never()).save(any());
    }

    private CadastroUsuarioRequest cadastro(boolean declarouMaiorIdade) {
        return new CadastroUsuarioRequest("Professor Teste", EMAIL, "Senha@123", "Senha@123",
                declarouMaiorIdade, true, "v1.0", true, "1.0");
    }

    private ConviteAcesso convitePendente() {
        return new ConviteAcesso(EMAIL, Perfil.PROFESSOR, "hash-anterior", LocalDateTime.now().plusDays(1));
    }
}
