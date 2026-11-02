package com.cursovalido.backend.authentication.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cursovalido.backend.authentication.dto.CadastroUsuarioRequisicao;
import com.cursovalido.backend.authentication.dto.EnderecoRequisicao;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.exception.CadastroDuplicadoException;
import com.cursovalido.backend.authentication.exception.TermosNaoDisponiveisException;
import com.cursovalido.backend.authentication.repository.UsuarioRepositorio;
import com.cursovalido.backend.forum.service.UsuarioForumServico;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CadastroUsuarioServicoTest {

    @Mock
    private UsuarioRepositorio repositorio;
    @Mock
    private PasswordEncoder codificadorSenha;
    @Mock
    private LogAuditoriaServico auditoria;
    @Mock
    private UsuarioForumServico usuariosForum;
    @Mock
    private PoliticaServico politicas;
    @Mock
    private TokenEmailServico tokensEmail;
    @Mock
    private EmailServico emails;
    @InjectMocks
    private CadastroUsuarioServico servico;

    @Test
    void deveCadastrarUsuarioMaiorDeIdade() {
        // Mantemos as integrações fora do teste para avaliar só o cadastro.
        CadastroUsuarioRequisicao requisicao = requisicaoPadrao();
        Usuario salvo = new Usuario();
        when(politicas.versaoAtiva("1.0")).thenReturn(true);
        when(repositorio.existsByEmail(requisicao.email())).thenReturn(false);
        when(repositorio.existsByCpf(requisicao.cpf())).thenReturn(false);
        when(repositorio.existsByMatricula(any(String.class))).thenReturn(false);
        when(codificadorSenha.encode("senha-segura")).thenReturn("hash");
        when(repositorio.save(any(Usuario.class))).thenReturn(salvo);
        when(tokensEmail.criarConfirmacao(salvo)).thenReturn("token");

        Usuario resultado = servico.cadastrar(requisicao, "127.0.0.1");

        assertEquals(salvo, resultado);
        verify(emails).enviarConfirmacao(salvo, "token");
        verify(repositorio).save(any(Usuario.class));
    }

    @Test
    void deveLancarExcecaoQuandoEmailJaEstiverCadastrado() {
        CadastroUsuarioRequisicao requisicao = requisicaoPadrao();
        // A duplicidade deve interromper o fluxo antes de salvar qualquer dado.
        when(politicas.versaoAtiva("1.0")).thenReturn(true);
        when(repositorio.existsByEmail(requisicao.email())).thenReturn(true);

        assertThrows(CadastroDuplicadoException.class,
                () -> servico.cadastrar(requisicao, "127.0.0.1"));
        verify(repositorio, never()).save(any(Usuario.class));
    }

    @Test
    void deveLancarExcecaoQuandoVersaoDosTermosNaoEstiverDisponivel() {
        CadastroUsuarioRequisicao requisicao = requisicaoPadrao();
        when(politicas.versaoAtiva("1.0")).thenReturn(false);

        assertThrows(TermosNaoDisponiveisException.class,
                () -> servico.cadastrar(requisicao, "127.0.0.1"));
        verify(repositorio, never()).existsByEmail(any(String.class));
    }

    @Test
    void deveCadastrarUsuarioNaDataExataEmQueCompletaDezoitoAnos() {
        // A idade mínima também precisa ser aceita no limite da regra.
        CadastroUsuarioRequisicao requisicao = requisicaoComNascimento(
                LocalDate.now().minusYears(18));
        Usuario salvo = new Usuario();
        when(politicas.versaoAtiva("1.0")).thenReturn(true);
        when(repositorio.existsByEmail(requisicao.email())).thenReturn(false);
        when(repositorio.existsByCpf(requisicao.cpf())).thenReturn(false);
        when(repositorio.existsByMatricula(any(String.class))).thenReturn(false);
        when(codificadorSenha.encode("senha-segura")).thenReturn("hash");
        when(repositorio.save(any(Usuario.class))).thenReturn(salvo);
        when(tokensEmail.criarConfirmacao(salvo)).thenReturn("token");

        Usuario resultado = servico.cadastrar(requisicao, "127.0.0.1");

        assertEquals(salvo, resultado);
        verify(repositorio).save(any(Usuario.class));
    }

    private CadastroUsuarioRequisicao requisicaoPadrao() {
        // Dados válidos compartilhados pelos cenários de cadastro.
        return requisicaoComNascimento(LocalDate.now().minusYears(25));
    }

    private CadastroUsuarioRequisicao requisicaoComNascimento(LocalDate dataNascimento) {
        return new CadastroUsuarioRequisicao(
                "Aluno Teste",
                "aluno@teste.com",
                "senha-segura",
                "12345678901",
                dataNascimento,
                "11999999999",
                new EnderecoRequisicao("01001000", "Rua A", "Centro", "Sao Paulo", "SP"),
                true,
                true,
                "1.0");
    }
}
