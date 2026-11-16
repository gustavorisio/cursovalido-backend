package com.cursovalido.backend.authentication.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LimiteCadastroUnitarioTest {

    private static final String SQL = "INSERT INTO tb_cadastro_vagas (chave) VALUES (?) ON CONFLICT DO NOTHING";
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final LimiteCadastroService service = new LimiteCadastroService(jdbc);

    @Test
    void deveUsarSegundaVagaQuandoPrimeiraEstiverOcupada() {
        when(jdbc.update(SQL, "192.0.2.10:2")).thenReturn(1);

        service.reservar("192.0.2.10");

        var ordem = inOrder(jdbc);
        ordem.verify(jdbc).update(SQL, "192.0.2.10:1");
        ordem.verify(jdbc).update(SQL, "192.0.2.10:2");
    }

    @Test
    void deveRecusarCadastroQuandoDuasVagasEstiveremOcupadas() {
        var erro = assertThrows(ResponseStatusException.class, () -> service.reservar("192.0.2.10"));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, erro.getStatusCode());
    }

    @Test
    void deveIgnorarIpEmBranco() {
        service.reservar(" ");

        verifyNoInteractions(jdbc);
    }
}

