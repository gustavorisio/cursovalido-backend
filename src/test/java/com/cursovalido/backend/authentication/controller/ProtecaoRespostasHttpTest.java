package com.cursovalido.backend.authentication.controller;

import com.cursovalido.backend.authentication.configuration.AutenticacaoExceptionHandler;
import com.cursovalido.backend.authentication.dto.LoginRequest;
import com.cursovalido.backend.authentication.exception.CredenciaisInvalidasException;
import com.cursovalido.backend.authentication.exception.TermosNaoAceitosException;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProtecaoRespostasHttpTest {
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new ControladorTeste())
                .setControllerAdvice(new AutenticacaoExceptionHandler()).build();
    }

    @Test
    void falhaInternaNaoEnviaDetalhesTecnicosAoCliente() throws Exception {
        mvc.perform(get("/teste/falha").queryParam("trace", "true").queryParam("message", "true"))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"))
                .andExpect(jsonPath("$.erro").exists())
                .andExpect(content().string(not(containsString("jdbc:"))))
                .andExpect(content().string(not(containsString("segredo"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist());
    }

    @Test
    void statusDeIndisponibilidadeEPermissaoSaoPreservados() throws Exception {
        mvc.perform(get("/teste/fornecedor"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"))
                .andExpect(content().string(not(containsString("interno.example"))));
        mvc.perform(get("/teste/restrito"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"))
                .andExpect(content().string(not(containsString("ROLE_"))));
    }

    @Test
    void erroDoProtocoloNaoViraFalhaInterna() throws Exception {
        mvc.perform(post("/teste/falha"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_RECUSADA"));
    }

    @Test
    void loginIncorretoOrientaSemExporQualDadoEstaErrado() throws Exception {
        mvc.perform(get("/teste/credenciais"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"))
                .andExpect(jsonPath("$.erro").value("E-mail ou senha incorretos. Confira os dados e tente novamente."));
    }

    @Test
    void aceitePendenteExplicaOsDoisDocumentos() throws Exception {
        mvc.perform(get("/teste/termos"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("TERMOS_NAO_ACEITOS"))
                .andExpect(jsonPath("$.erro").value("Você precisa aceitar os Termos de Uso e a Política de Privacidade para continuar."));
    }

    @Test
    void validacaoApontaCamposComOrientacaoEmPortugues() throws Exception {
        mvc.perform(post("/teste/dados").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"email-incorreto\",\"senha\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CAMPOS_INVALIDOS"))
                .andExpect(jsonPath("$.campos.email").value("Digite um e-mail válido, como nome@exemplo.com."))
                .andExpect(jsonPath("$.campos.senha").value("Preencha este campo."));
    }

    @RestController
    static class ControladorTeste {
        @GetMapping("/teste/credenciais")
        public String credenciais() { throw new CredenciaisInvalidasException(); }

        @GetMapping("/teste/termos")
        public String termos() { throw new TermosNaoAceitosException("token-teste"); }

        @PostMapping("/teste/dados")
        public String dados(@Valid @RequestBody LoginRequest dados) { return "ok"; }

        @GetMapping("/teste/falha")
        public String falha() { throw new IllegalStateException("jdbc:postgresql://interno/db password=segredo"); }

        @GetMapping("/teste/fornecedor")
        public String fornecedor() { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "https://interno.example/segredo"); }

        @GetMapping("/teste/restrito")
        public String restrito() { throw new AccessDeniedException("ROLE_ADMINISTRADOR"); }
    }
}
