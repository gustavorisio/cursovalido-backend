package com.cursovalido.backend.authentication.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cursovalido.backend.authentication.dto.LoginRequisicao;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.StatusAprovacao;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.exception.CredenciaisInvalidasException;
import com.cursovalido.backend.authentication.exception.TermosNaoAceitosException;
import com.cursovalido.backend.authentication.repository.SessaoRepositorio;
import com.cursovalido.backend.authentication.repository.UsuarioRepositorio;
import com.cursovalido.backend.forum.service.UsuarioForumServico;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class AdministradorInicialServicoTest {
    private final UsuarioRepositorio usuarios = mock(UsuarioRepositorio.class);
    private final PasswordEncoder senhas = new BCryptPasswordEncoder(4);

    @Test
    void criaAdministradorComHashSemInventarDadosOuAceite() {
        servico("senha-admin-segura").criar();
        var captura = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captura.capture());
        var admin = captura.getValue();
        assertEquals("cursovalido@gmail.com", admin.getEmail());
        assertEquals(Perfil.ADMINISTRADOR, admin.getPerfil());
        assertEquals(StatusAprovacao.APROVADO, admin.getStatusAprovacao());
        assertTrue(admin.isAtivo());
        assertTrue(admin.isEmailConfirmado());
        assertTrue(senhas.matches("senha-admin-segura", admin.getSenha()));
        assertNotEquals("senha-admin-segura", admin.getSenha());
        assertNotNull(admin.getMatricula());
        assertFalse(admin.getAceitouTermosLgpd());
        assertNull(admin.getCpf());
        assertNull(admin.getEndereco());
        assertNull(admin.getDataNascimento());
        assertNull(admin.getDataHoraAceite());
        assertNull(admin.getVersaoTermos());
    }

    @Test
    void senhaSincronizadaPassaPelaValidacaoDoLogin() {
        var admin = new Usuario();
        admin.setEmail(AdministradorInicialServico.EMAIL);
        admin.setPerfil(Perfil.ADMINISTRADOR);
        admin.setSenha(senhas.encode("senha-antiga"));
        admin.setAceitouTermosLgpd(false);
        admin.setEmailConfirmado(true);
        when(usuarios.findByEmail(AdministradorInicialServico.EMAIL)).thenReturn(Optional.of(admin));
        servico("nova-senha-segura").criar();

        var autenticacao = new AutenticacaoServico(usuarios, mock(TokenJwtServico.class),
                mock(TokenEmailServico.class), mock(EmailServico.class), senhas,
                mock(LogAuditoriaServico.class), mock(UsuarioForumServico.class),
                mock(ProtecaoForcaBrutaServico.class));

        assertThrows(TermosNaoAceitosException.class,
                () -> autenticacao.autenticar(new LoginRequisicao(
                        AdministradorInicialServico.EMAIL, "nova-senha-segura"), "127.0.0.1"));
        assertThrows(CredenciaisInvalidasException.class,
                () -> autenticacao.autenticar(new LoginRequisicao(
                        AdministradorInicialServico.EMAIL, "senha-antiga"), "127.0.0.1"));
    }

    @Test
    void semVariavelNaoCriaConta() {
        servico("").criar();
        verifyNoInteractions(usuarios);
    }

    @Test
    void sincronizaSenhaSemReativarAdministradorExistente() {
        var admin = new Usuario();
        admin.setPerfil(Perfil.ADMINISTRADOR);
        admin.setSenha(senhas.encode("senha-antiga"));
        admin.setAtivo(false);
        when(usuarios.findByEmail(AdministradorInicialServico.EMAIL)).thenReturn(Optional.of(admin));
        servico("nova-senha-segura").criar();
        assertTrue(senhas.matches("nova-senha-segura", admin.getSenha()));
        assertFalse(senhas.matches("senha-antiga", admin.getSenha()));
        assertFalse(admin.isAtivo());
        verify(usuarios).save(admin);
    }

    @Test
    void preservaHashQuandoSenhaConfiguradaJaCorresponde() {
        var admin = new Usuario();
        admin.setPerfil(Perfil.ADMINISTRADOR);
        String hash = senhas.encode("senha-admin-segura");
        admin.setSenha(hash);
        when(usuarios.findByEmail(AdministradorInicialServico.EMAIL)).thenReturn(Optional.of(admin));
        servico("senha-admin-segura").criar();
        assertEquals(hash, admin.getSenha());
        verify(usuarios, never()).save(any());
    }

    @Test
    void naoPromoveContaExistenteDeOutroPerfil() {
        var aluno = new Usuario();
        aluno.setPerfil(Perfil.ALUNO);
        when(usuarios.findByEmail(AdministradorInicialServico.EMAIL)).thenReturn(Optional.of(aluno));
        assertThrows(IllegalStateException.class,
                () -> servico("senha-admin-segura").criar());
        assertEquals(Perfil.ALUNO, aluno.getPerfil());
        verify(usuarios, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "curta", "        ",
            "1234567890123456789012345678901234567890123456789012345678901234567890123",
            "ááááááááááááááááááááááááááááááááááááá" })
    void rejeitaSenhaInvalidaSemPersistir(String senha) {
        assertThrows(IllegalStateException.class,
                () -> servico(senha).criar());
        verify(usuarios, never()).save(any());
    }

    @Test
    void reinicializacaoNaoDuplicaConta() {
        var admin = new Usuario();
        admin.setPerfil(Perfil.ADMINISTRADOR);
        admin.setSenha(senhas.encode("senha-admin-segura"));
        when(usuarios.findByEmail(AdministradorInicialServico.EMAIL))
                .thenReturn(Optional.empty(), Optional.of(admin));
        var servico = servico("senha-admin-segura");
        servico.criar();
        servico.criar();
        verify(usuarios, times(1)).save(any());
    }
    private AdministradorInicialServico servico(String senha) {
        var servico = new AdministradorInicialServico(usuarios, senhas,
                mock(SessaoRepositorio.class), mock(TokenEmailServico.class));
        ReflectionTestUtils.setField(servico, "senha", senha);
        return servico;
    }
}
