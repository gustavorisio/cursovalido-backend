package com.cursovalido.backend.authentication.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cursovalido.backend.authentication.entity.*;
import com.cursovalido.backend.authentication.repository.*;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class AdministradorInicialServiceTest {
    private static final ValidatorFactory VALIDADORES = Validation.buildDefaultValidatorFactory();
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final InicializacaoAdministradorRepository inicializacao = mock(InicializacaoAdministradorRepository.class);
    private final PasswordEncoder senhas = new BCryptPasswordEncoder(4);
    private final InicializacaoAdministrador registro = new InicializacaoAdministrador();

    @BeforeEach
    void preparar() {
        when(inicializacao.buscarParaAtualizar()).thenReturn(Optional.of(registro));
        when(usuarios.saveAndFlush(any())).thenAnswer(chamada -> {
            Usuario usuario = chamada.getArgument(0);
            ReflectionTestUtils.setField(usuario, "id", 10L);
            return usuario;
        });
    }

    @AfterAll
    static void encerrarValidadores() { VALIDADORES.close(); }

    @Test
    void criaComEmailConfiguradoEHashSemInventarDadosOuAceite() {
        servico(" Inicial@Example.com ", "senha-admin-segura").criar();
        var captura = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).saveAndFlush(captura.capture());
        var admin = captura.getValue();
        assertEquals("inicial@example.com", admin.getEmail());
        assertEquals(Perfil.ADMINISTRADOR, admin.getPerfil());
        assertTrue(admin.isAtivo());
        assertTrue(admin.isEmailConfirmado());
        assertTrue(senhas.matches("senha-admin-segura", admin.getSenha()));
        assertNotEquals("senha-admin-segura", admin.getSenha());
        assertFalse(admin.getAceitouTermosLgpd());
        assertNull(admin.getCpf());
        assertNull(admin.getDataHoraAceite());
        assertTrue(registro.isConcluida());
        assertEquals(10L, registro.getIdUsuario());
        assertNotNull(registro.getConcluidaEm());
    }

    @Test
    void reiniciarComOutrasCredenciaisNaoCriaContaNemAlteraSenha() {
        servico("inicial@example.com", "senha-original").criar();
        servico("outro@example.com", "senha-diferente").criar();
        var captura = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios, times(1)).saveAndFlush(captura.capture());
        assertTrue(senhas.matches("senha-original", captura.getValue().getSenha()));
        assertEquals(10L, registro.getIdUsuario());
    }

    @Test
    void exclusaoDaContaNaoPermiteRecriar() {
        registro.concluir(null);
        servico("novo@example.com", "senha-admin-segura").criar();
        verifyNoInteractions(usuarios);
        assertTrue(registro.isConcluida());
    }

    @Test
    void aposCriacaoIgnoraAteCredenciaisAusentesOuInvalidas() {
        registro.concluir(10L);
        assertDoesNotThrow(() -> servico("", "").criar());
        assertDoesNotThrow(() -> servico("invalido", "curta").criar());
        verifyNoInteractions(usuarios);
    }

    @Test
    void semVariaveisAguardaConfiguracaoSemEncerrarInicializacao() {
        servico("", "").criar();
        assertFalse(registro.isConcluida());
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void administradorPreexistenteEncerraInicializacaoSemPromoverOutraConta() {
        when(usuarios.existsByPerfil(Perfil.ADMINISTRADOR)).thenReturn(true);
        servico("outro@example.com", "senha-admin-segura").criar();
        assertTrue(registro.isConcluida());
        assertNull(registro.getIdUsuario());
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void emailJaUsadoNaoPromoveContaExistente() {
        when(usuarios.existsByEmail("aluno@example.com")).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> servico("aluno@example.com", "senha-admin-segura").criar());
        assertFalse(registro.isConcluida());
        verify(usuarios, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "invalido", "admin@", "@example.com" })
    void exigeEmailValidoQuandoSenhaFoiConfigurada(String email) {
        assertThrows(IllegalStateException.class, () -> servico(email, "senha-admin-segura").criar());
        verify(usuarios, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "curta", "        ",
            "1234567890123456789012345678901234567890123456789012345678901234567890123",
            "ááááááááááááááááááááááááááááááááááááá" })
    void rejeitaSenhaInvalidaSemPersistir(String senha) {
        assertThrows(IllegalStateException.class, () -> servico("inicial@example.com", senha).criar());
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void privilegiosDependemDoIdPersistidoEnaoDoEmail() {
        registro.concluir(10L);
        when(inicializacao.findById(1L)).thenReturn(Optional.of(registro));
        var privilegios = new PrivilegiosAdministrativosService(usuarios, inicializacao);
        var principal = new Usuario();
        ReflectionTestUtils.setField(principal, "id", 10L);
        principal.setEmail("email-alterado@example.com");
        assertTrue(privilegios.ehPrincipal(principal));
        var outro = new Usuario();
        ReflectionTestUtils.setField(outro, "id", 20L);
        outro.setEmail("inicial@example.com");
        assertFalse(privilegios.ehPrincipal(outro));
    }

    private AdministradorInicialService servico(String email, String senha) {
        var servico = new AdministradorInicialService(usuarios, senhas, inicializacao, VALIDADORES.getValidator());
        ReflectionTestUtils.setField(servico, "email", email);
        ReflectionTestUtils.setField(servico, "senha", senha);
        return servico;
    }
}
