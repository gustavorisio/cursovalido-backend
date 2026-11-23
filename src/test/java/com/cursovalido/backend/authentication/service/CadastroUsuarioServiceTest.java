package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.CadastroUsuarioRequest;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.StatusAprovacao;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.exception.CadastroDuplicadoException;
import com.cursovalido.backend.authentication.exception.TermosNaoDisponiveisException;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.validation.email.ValidacaoEmailService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CadastroUsuarioServiceTest {

    @Mock
    private UsuarioRepository repositorio;
    @Mock
    private PasswordEncoder codificadorSenha;
    @Mock
    private LogAuditoriaService auditoria;
    @Mock
    private PoliticaService politicas;
    @Mock
    private ValidacaoEmailService validacaoEmail;
    @InjectMocks
    private CadastroUsuarioService servico;

    @Test
    void deveInterromperCadastroAntesDeSalvarQuandoEmailForInvalido() {
        var requisicao = requisicaoPadrao();
        Mockito.doThrow(new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "E-mail invalido"))
                .when(validacaoEmail).validar(requisicao.email());

        assertThrows(ResponseStatusException.class,
                () -> servico.cadastrar(requisicao, "127.0.0.1"));
        Mockito.verifyNoInteractions(repositorio, codificadorSenha, politicas);
    }

    @Test
    void deveCadastrarUsuarioMaiorDeIdade() {
        // Mantemos as integrações fora do teste para avaliar só o cadastro.
        CadastroUsuarioRequest requisicao = requisicaoPadrao();
        Usuario salvo = new Usuario();
        when(politicas.versaoAtiva("1.0")).thenReturn(true);
        when(repositorio.existsByEmail(requisicao.email())).thenReturn(false);
        when(repositorio.existsByMatricula(any(String.class))).thenReturn(false);
        when(codificadorSenha.encode("senha-segura")).thenReturn("hash");
        when(repositorio.save(any(Usuario.class))).thenReturn(salvo);

        Usuario resultado = servico.cadastrar(requisicao, "127.0.0.1");

        assertEquals(salvo, resultado);
        verify(repositorio).save(any(Usuario.class));
    }

    @Test
    void deveLancarExcecaoQuandoEmailJaEstiverCadastrado() {
        CadastroUsuarioRequest requisicao = requisicaoPadrao();
        // A duplicidade deve interromper o fluxo antes de salvar qualquer dado.
        when(politicas.versaoAtiva("1.0")).thenReturn(true);
        when(repositorio.existsByEmail(requisicao.email())).thenReturn(true);

        assertThrows(CadastroDuplicadoException.class,
                () -> servico.cadastrar(requisicao, "127.0.0.1"));
        verify(repositorio, never()).save(any(Usuario.class));
    }

    @Test
    void deveLancarExcecaoQuandoVersaoDosTermosNaoEstiverDisponivel() {
        CadastroUsuarioRequest requisicao = requisicaoPadrao();
        when(politicas.versaoAtiva("1.0")).thenReturn(false);

        assertThrows(TermosNaoDisponiveisException.class,
                () -> servico.cadastrar(requisicao, "127.0.0.1"));
        verify(repositorio, never()).existsByEmail(any(String.class));
    }

    private CadastroUsuarioRequest requisicaoPadrao() {
        return requisicao(true);
    }

    @Test
    void exigeConfirmacaoDeMaioridadeNoCadastro() {
        when(politicas.versaoAtiva("1.0")).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
                () -> servico.cadastrar(requisicao(false), "127.0.0.1"));
        verify(repositorio, never()).save(any(Usuario.class));
    }

    @Test
    void cadastroPublicoCriaSomenteAlunoAtivo() {
        when(politicas.versaoAtiva("1.0")).thenReturn(true);
        when(repositorio.existsByMatricula(any(String.class))).thenReturn(false);
        when(repositorio.save(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Usuario resultado = servico.cadastrar(requisicaoPadrao(), "127.0.0.1");

        assertEquals(Perfil.ALUNO, resultado.getPerfil());
        assertEquals(StatusAprovacao.APROVADO,
                resultado.getStatusAprovacao());
        Assertions.assertTrue(resultado.isAtivo());
        Assertions.assertTrue(resultado.getDeclarouMaiorIdade());
    }

    private CadastroUsuarioRequest requisicao(boolean declarouMaiorIdade) {
        return new CadastroUsuarioRequest("Aluno Teste", "aluno@teste.com", "senha-segura", "senha-segura",
                declarouMaiorIdade, true, "1.0", null, null);
    }
}
