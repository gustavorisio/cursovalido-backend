package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.validation.email.ValidacaoEmailService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cursovalido.backend.authentication.dto.CadastroUsuarioRequest;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.exception.CadastroDuplicadoException;
import com.cursovalido.backend.authentication.exception.TermosNaoDisponiveisException;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
        org.mockito.Mockito.doThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "E-mail invalido"))
                .when(validacaoEmail).validar(requisicao.email());

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> servico.cadastrar(requisicao, "127.0.0.1"));
        org.mockito.Mockito.verifyNoInteractions(repositorio, codificadorSenha, politicas);
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

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = { 18, 90 })
    void aceitaIdadesNosLimites(int idade) {
        // A idade mínima também precisa ser aceita no limite da regra.
        CadastroUsuarioRequest requisicao = requisicaoComNascimento(
                LocalDate.now().minusYears(idade));
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

    private CadastroUsuarioRequest requisicaoPadrao() {
        // Dados válidos compartilhados pelos cenários de cadastro.
        return requisicaoComNascimento(LocalDate.now().minusYears(25));
    }

    @Test
    void rejeitaCadastroNoDiaAnteriorACompletarDezoitoAnos() {
        var requisicao = requisicaoComNascimento(LocalDate.now().minusYears(18).plusDays(1));
        when(politicas.versaoAtiva("1.0")).thenReturn(true);
        assertThrows(com.cursovalido.backend.authentication.exception.MenorDeIdadeException.class,
                () -> servico.cadastrar(requisicao, "127.0.0.1"));
        verify(repositorio, never()).save(any(Usuario.class));
    }

    private CadastroUsuarioRequest requisicaoComNascimento(LocalDate dataNascimento) {
        return new CadastroUsuarioRequest(
                "Aluno Teste",
                "aluno@teste.com",
                "senha-segura",
                "senha-segura",
                dataNascimento,
                true,
                true,
                "1.0");
    }

    @Test
    void rejeitaIdadeAcimaDeNoventaAnos() {
        when(politicas.versaoAtiva("1.0")).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
                () -> servico.cadastrar(requisicaoComNascimento(LocalDate.now().minusYears(91)), "127.0.0.1"));
        verify(repositorio, never()).save(any(Usuario.class));
    }
}
