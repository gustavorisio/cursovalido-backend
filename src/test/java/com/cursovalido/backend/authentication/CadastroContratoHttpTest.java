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

        private String dados(String cpf) {
                return """
                                {"nomeCompleto":"Aluno Teste","email":"aluno@example.com","senha":"SenhaTeste123!",
                                "cpf":"%s","dataNascimento":"1990-01-01","declarouMaiorIdade":true,
                                "aceitouTermosLgpd":true,"versaoTermos":"1.0"}
                                """.formatted(cpf);
        }

        @Test
        void cadastroPublicoEConviteAceitamCpfValidoSemTelefone() throws Exception {
                mvc.perform(post("/api/autenticacao/cadastro").contentType(MediaType.APPLICATION_JSON)
                                .content(dados("52998224725"))).andExpect(status().isCreated());
                mvc.perform(post("/api/convites/completar-cadastro").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"token\":\"convite\",\"cadastro\":" + dados("11144477735") + "}"))
                                .andExpect(status().isNoContent());
                verify(cadastro).cadastrar(argThat(r -> r.cpf().equals("52998224725")), anyString());
                verify(convites).completar(eq("convite"), argThat(r -> r.cpf().equals("11144477735")));
        }

        @ParameterizedTest
        @ValueSource(strings = { "12345678901", "11111111111", "00000000000", "52998224724", "5299822472" })
        void ambosCadastrosRecusamCpfInvalidoAntesDoServico(String cpf) throws Exception {
                mvc.perform(post("/api/autenticacao/cadastro").contentType(MediaType.APPLICATION_JSON)
                                .content(dados(cpf))).andExpect(status().isBadRequest());
                mvc.perform(post("/api/convites/completar-cadastro").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"token\":\"convite\",\"cadastro\":" + dados(cpf) + "}"))
                                .andExpect(status().isBadRequest());
                verifyNoInteractions(cadastro, convites);
        }
}
