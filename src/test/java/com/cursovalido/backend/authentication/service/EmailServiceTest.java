package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.ConviteAcesso;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.exception.ServicoEmailIndisponivelException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    private final EmailService service = new EmailService(
            "https://emails.example.com", "chave-teste", "curso@example.com", "https://curso.example.com");
    private MockRestServiceServer servidor;

    @BeforeEach
    void configurarCliente() {
        var cliente = RestClient.builder().baseUrl("https://emails.example.com");
        servidor = MockRestServiceServer.bindTo(cliente).build();
        ReflectionTestUtils.setField(service, "cliente", cliente.build());
    }

    @Test
    void deveEnviarCodigoDeAcessoParaEmailDoUsuario() {
        servidor.expect(requestTo("https://emails.example.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.to[0]").value("aluno@example.com"))
                .andExpect(jsonPath("$.text", org.hamcrest.Matchers.containsString("123456")))
                .andRespond(withSuccess());

        service.enviarCodigo2fa(usuario(), "123456");

        servidor.verify();
    }

    @Test
    void deveEnviarLinkDeRedefinicaoComToken() {
        esperarMensagemCom("https://curso.example.com/redefinir-senha?token=token-teste");

        service.enviarRedefinicaoSenha(usuario(), "token-teste");

        servidor.verify();
    }

    @Test
    void deveEnviarLinkDeCadastroParaConvidado() {
        var convite = new ConviteAcesso("professor@example.com", Perfil.PROFESSOR, "hash",
                LocalDateTime.now().plusDays(2));
        esperarMensagemCom("https://curso.example.com/convites/cadastro?token=token-teste");

        service.enviarConvite(convite, "token-teste");

        servidor.verify();
    }

    @Test
    void deveEnviarCodigoParaConfirmarExclusao() {
        esperarMensagemCom("123456");

        service.enviarExclusaoConta(usuario(), "123456");

        servidor.verify();
    }

    @Test
    void deveUsarContatoComoEnderecoDeRespostaNoSuporte() {
        servidor.expect(requestTo("https://emails.example.com/emails"))
                .andExpect(jsonPath("$.reply_to").value("aluno@example.com"))
                .andRespond(withSuccess());

        service.enviarSuporte("suporte@example.com", "aluno@example.com", "Dúvida", "Preciso de ajuda");

        servidor.verify();
    }

    @Test
    void deveInformarIndisponibilidadeQuandoProvedorFalhar() {
        servidor.expect(requestTo("https://emails.example.com/emails")).andRespond(withServerError());

        assertThrows(ServicoEmailIndisponivelException.class, () -> service.enviarCodigo2fa(usuario(), "123456"));

        servidor.verify();
    }

    private void esperarMensagemCom(String trecho) {
        servidor.expect(requestTo("https://emails.example.com/emails"))
                .andExpect(jsonPath("$.text", org.hamcrest.Matchers.containsString(trecho)))
                .andRespond(withSuccess());
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setNomeCompleto("Aluno Teste");
        usuario.setEmail("aluno@example.com");
        return usuario;
    }
}
