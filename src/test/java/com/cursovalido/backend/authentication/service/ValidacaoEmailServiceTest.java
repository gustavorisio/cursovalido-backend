package com.cursovalido.backend.authentication.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import javax.naming.CommunicationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ValidacaoEmailServiceTest {
    private final DominioEmailService dominios = mock(DominioEmailService.class);
    private final ValidacaoEmailService servico = new ValidacaoEmailService(dominios);

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "@gmail.com", "aluno", "aluno@", "aluno@@gmail.com", "aluno@gmail",
            "aluno..teste@gmail.com", ".aluno@gmail.com", "aluno @gmail.com", "aluno@gmail.com.",
            "aluno@-gmail.com", "aluno@[127.0.0.1]", "aluno@gmail.com/path" })
    void rejeitaFormatoInvalidoSemConsultarDns(String email) {
        var erro = assertThrows(ResponseStatusException.class, () -> servico.validar(email));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verifyNoInteractions(dominios);
    }

    @Test
    void rejeitaDominioInexistente() throws Exception {
        when(dominios.aceitaEmail("gmail.commm")).thenReturn(false);
        var erro = assertThrows(ResponseStatusException.class, () -> servico.validar("aluno@gmail.commm"));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = { "aluno@gmail.com", "aluno+curso@universidade.edu.br", "nome.sobrenome@empresa.com.br" })
    void aceitaDominiosComEntregaDeEmail(String email) throws Exception {
        when(dominios.aceitaEmail(email.substring(email.indexOf('@') + 1))).thenReturn(true);
        assertDoesNotThrow(() -> servico.validar(email));
    }

    @Test
    void falhaTemporariaNaoEhTratadaComoEmailInvalido() throws Exception {
        when(dominios.aceitaEmail("gmail.com")).thenThrow(new CommunicationException("timeout"));
        var erro = assertThrows(ResponseStatusException.class, () -> servico.validar("aluno@gmail.com"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, erro.getStatusCode());
    }
}
