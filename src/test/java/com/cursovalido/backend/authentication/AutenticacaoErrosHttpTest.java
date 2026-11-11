package com.cursovalido.backend.authentication;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cursovalido.backend.authentication.configuration.AutenticacaoExceptionHandler;
import com.cursovalido.backend.authentication.controller.ConviteController;
import com.cursovalido.backend.authentication.service.ConviteAcessoService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

class AutenticacaoErrosHttpTest {
        private final ConviteAcessoService convites = mock(ConviteAcessoService.class);

        @Test
        void preservaStatusEMotivoDaRecusa() throws Exception {
                for (var status : new HttpStatus[] { HttpStatus.BAD_REQUEST, HttpStatus.FORBIDDEN,
                                HttpStatus.SERVICE_UNAVAILABLE }) {
                        doThrow(new ResponseStatusException(status, "Motivo da recusa"))
                                        .when(convites).confirmarEmail("token");
                        MockMvcBuilders.standaloneSetup(new ConviteController(convites))
                                        .setControllerAdvice(new AutenticacaoExceptionHandler()).build()
                                        .perform(post("/api/convites/confirmar-email")
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content("{\"token\":\"token\"}"))
                                        .andExpect(status().is(status.value()))
                                        .andExpect(jsonPath("$.erro").value("Motivo da recusa"))
                                        .andExpect(jsonPath("$.codigo").value("OPERACAO_RECUSADA"));
                }
        }

        @Test
        void jsonInvalidoRetornaErroLegivel() throws Exception {
                MockMvcBuilders.standaloneSetup(new ConviteController(convites))
                                .setControllerAdvice(new AutenticacaoExceptionHandler()).build()
                                .perform(post("/api/convites/confirmar-email").contentType(MediaType.APPLICATION_JSON)
                                                .content("{"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.codigo").value("JSON_INVALIDO"));
                verifyNoInteractions(convites);
        }
}
