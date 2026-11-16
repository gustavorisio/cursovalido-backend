package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.dto.CaminhosRequest;
import com.cursovalido.backend.aulas.dto.CaminhosRequest.Opcao;
import com.cursovalido.backend.aulas.dto.CaminhosRequest.Pergunta;
import com.cursovalido.backend.aulas.dto.CaminhosRequest.Resultado;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidacaoCaminhosTest {

    @Test
    void deveAceitarCaminhoQueChegaAoResultado() {
        var inicial = new Pergunta("inicio", "Quer aprender Java?", List.of(new Opcao("Sim", "nivel", null)));
        var nivel = new Pergunta("nivel", "Qual seu nível?", List.of(new Opcao("Iniciante", null, "java")));

        assertDoesNotThrow(() -> ValidacaoCaminhos.validar(questionario(inicial, nivel)));
    }

    @Test
    void deveRecusarCicloEntrePerguntas() {
        var inicial = new Pergunta("inicio", "Quer aprender Java?", List.of(new Opcao("Sim", "nivel", null)));
        var nivel = new Pergunta("nivel", "Qual seu nível?", List.of(new Opcao("Voltar", "inicio", null)));

        var erro = assertThrows(ResponseStatusException.class,
                () -> ValidacaoCaminhos.validar(questionario(inicial, nivel)));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        assertTrue(erro.getReason().contains("ciclo"));
    }

    @Test
    void deveRecusarPerguntaSemCaminhoAPartirDoInicio() {
        var opcao = new Opcao("Iniciante", null, "java");
        var inicial = new Pergunta("inicio", "Primeira pergunta", List.of(opcao));
        var isolada = new Pergunta("isolada", "Pergunta isolada", List.of(opcao));

        var erro = assertThrows(ResponseStatusException.class,
                () -> ValidacaoCaminhos.validar(questionario(inicial, isolada)));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        assertTrue(erro.getReason().contains("alcancadas"));
    }

    private CaminhosRequest questionario(Pergunta... perguntas) {
        var resultado = new Resultado("java", "Java", "Comece pela sintaxe", List.of("Variáveis"));
        return new CaminhosRequest("Trilha", "", true, 1, "inicio", List.of(perguntas), List.of(resultado));
    }
}

