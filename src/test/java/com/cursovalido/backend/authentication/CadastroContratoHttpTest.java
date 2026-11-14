package com.cursovalido.backend.authentication;

import com.cursovalido.backend.authentication.configuration.AutenticacaoExceptionHandler;
import com.cursovalido.backend.authentication.controller.AutenticacaoController;
import com.cursovalido.backend.authentication.controller.ConviteController;
import com.cursovalido.backend.authentication.service.CadastroUsuarioService;
import com.cursovalido.backend.authentication.service.ConviteAcessoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CadastroContratoHttpTest {
        private final CadastroUsuarioService cadastro = mock(CadastroUsuarioService.class);
        private final ConviteAcessoService convites = mock(ConviteAcessoService.class);
        private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
                        new AutenticacaoController(cadastro, null, null, null, null, null, null),
                        new ConviteController(convites))
                        .setControllerAdvice(new AutenticacaoExceptionHandler()).build();

        private String dados() {
                return """
                                {"nomeCompleto":"Aluno Teste","email":"aluno@example.com","senha":"SenhaTeste123!",
                                "dataNascimento":"1990-01-01","declarouMaiorIdade":true,
                                "aceitouTermosLgpd":true,"versaoTermos":"1.0"}
                                """;
        }

        @Test
        void cadastroPublicoEConviteAceitamDadosDoCadastro() throws Exception {
                mvc.perform(post("/api/autenticacao/cadastro").contentType(MediaType.APPLICATION_JSON)
                                .content(dados())).andExpect(status().isCreated());
                mvc.perform(post("/api/convites/completar-cadastro").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"token\":\"convite\",\"cadastro\":" + dados() + "}"))
                                .andExpect(status().isNoContent());
                verify(cadastro).cadastrar(argThat(r -> r.email().equals("aluno@example.com")), anyString());
                verify(convites).completar(eq("convite"), argThat(r -> r.email().equals("aluno@example.com")));
        }

        @ParameterizedTest
        @ValueSource(strings = { "nomeCompleto", "email", "senha", "dataNascimento", "declarouMaiorIdade", "aceitouTermosLgpd", "versaoTermos" })
        void ambosCadastrosRecusamCampoObrigatorioNulo(String campo) throws Exception {
                String incompleto = dados().replaceAll("\"" + campo + "\"\\s*:\\s*(\"[^\"]*\"|true)", "\"" + campo + "\":null");
                mvc.perform(post("/api/autenticacao/cadastro").contentType(MediaType.APPLICATION_JSON)
                                .content(incompleto)).andExpect(status().isBadRequest());
                mvc.perform(post("/api/convites/completar-cadastro").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"token\":\"convite\",\"cadastro\":" + incompleto + "}"))
                                .andExpect(status().isBadRequest());
                verifyNoInteractions(cadastro, convites);
        }
}
