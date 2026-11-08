package com.cursovalido.backend.authentication;

import com.cursovalido.backend.authentication.configuration.AutenticacaoExceptionHandler;
import com.cursovalido.backend.authentication.configuration.LimiteRequisicoesFilter;
import com.cursovalido.backend.authentication.configuration.TokenJwtFilter;
import com.cursovalido.backend.authentication.exception.ServicoEmailIndisponivelException;
import com.cursovalido.backend.authentication.service.EmailService;
import com.cursovalido.backend.authentication.suporte.SuporteController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SuporteTest {
    private EmailService emails;
    private MockMvc mvc;
    private com.cursovalido.backend.authentication.suporte.LimiteSuporteService limites;
    private static final String JSON = """
            {"emailConta":"conta@example.com","usuario":"Maria","emailContato":"retorno@example.com",
             "motivo":"REVISAO_BANIMENTO","assunto":"Revisar bloqueio",
             "descricao":"Minha conta foi bloqueada por engano ontem."}
            """;

    @BeforeEach
    void preparar() {
        emails = mock(EmailService.class);
        mvc = criar("suporte@example.com");
    }

    private MockMvc criar(String destino) {
        limites = mock(com.cursovalido.backend.authentication.suporte.LimiteSuporteService.class);
        doAnswer(chamada -> { chamada.getArgument(3, Runnable.class).run(); return null; }).when(limites)
                .enviar(anyString(), anyString(), anyString(), any(Runnable.class));
        return MockMvcBuilders.standaloneSetup(new SuporteController(emails, destino, limites))
                .setControllerAdvice(new AutenticacaoExceptionHandler())
                .addFilters(new LimiteRequisicoesFilter(2), new TokenJwtFilter(null, null, null)).build();
    }

    @Test
    void limiteDiarioRetornaCodigoETempoParaNovaTentativa() throws Exception {
        doThrow(new com.cursovalido.backend.authentication.suporte.LimiteSuporteException(86400))
                .when(limites).enviar(anyString(), anyString(), anyString(), any(Runnable.class));
        mvc.perform(post("/api/suporte/solicitacoes").header("X-Forwarded-For", "8.8.8.8")
                        .contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("LIMITE_SUPORTE_ATINGIDO"))
                .andExpect(header().string("Retry-After", "86400"));
        verify(limites).enviar(eq("127.0.0.1"), eq("conta@example.com"), eq("retorno@example.com"), any(Runnable.class));
        verifyNoInteractions(emails);
    }

    @Test
    void recebeSemLoginMesmoComTokenAntigoERespondeParaContato() throws Exception {
        mvc.perform(post("/api/suporte/solicitacoes").header("Authorization", "Bearer token-antigo")
                        .contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.protocolo").isNotEmpty());
        verify(emails).enviarSuporte(eq("suporte@example.com"), eq("retorno@example.com"),
                contains("REVISAO_BANIMENTO"), argThat(texto -> texto.contains("conta@example.com")
                        && texto.contains("Maria") && texto.contains("por engano")));
    }

    @Test
    void rejeitaCamposInvalidosSemEnviarEmail() throws Exception {
        mvc.perform(post("/api/suporte/solicitacoes").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.replace("conta@example.com", "invalido").replace("Maria", " ")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.emailConta").exists())
                .andExpect(jsonPath("$.campos.usuario").exists());
        verifyNoInteractions(emails);
    }

    @Test
    void rejeitaMotivoDesconhecido() throws Exception {
        mvc.perform(post("/api/suporte/solicitacoes").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.replace("REVISAO_BANIMENTO", "INVALIDO")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(emails);
    }

    @Test
    void naoConfirmaEnvioQuandoProvedorFalha() throws Exception {
        doThrow(new ServicoEmailIndisponivelException()).when(emails)
                .enviarSuporte(anyString(), anyString(), anyString(), anyString());
        mvc.perform(post("/api/suporte/solicitacoes").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.codigo").value("SERVICO_EMAIL_INDISPONIVEL"))
                .andExpect(jsonPath("$.protocolo").doesNotExist());
    }

    @Test
    void exigeConfiguracaoDoDestinatario() throws Exception {
        criar("").perform(post("/api/suporte/solicitacoes").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("SUPORTE_INDISPONIVEL"))
                .andExpect(jsonPath("$.erro").value("O suporte está temporariamente indisponível. Tente novamente mais tarde."));
        verifyNoInteractions(emails);
    }

    @Test
    void limitaEnviosRepetidos() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/suporte/solicitacoes").contentType(MediaType.APPLICATION_JSON).content(JSON))
                    .andExpect(status().isCreated());
        }
        mvc.perform(post("/api/suporte/solicitacoes").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "60"));
        verify(emails, times(2)).enviarSuporte(anyString(), anyString(), anyString(), anyString());
    }
}
