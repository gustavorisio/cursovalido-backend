package com.cursovalido.backend.Forum.configuracao;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

import com.cursovalido.backend.Forum.controlador.RespostaErro;

@RestControllerAdvice
public class TratamentoExcecoesForum {
    private static final Logger log = LoggerFactory.getLogger(TratamentoExcecoesForum.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<RespostaErro> tratarExcecaoForum(ResponseStatusException excecao) {
        log.warn("Excecao de negocio no forum: status={}, motivo={}", excecao.getStatusCode(), excecao.getReason());
        return ResponseEntity.status(excecao.getStatusCode())
                .body(new RespostaErro(excecao.getReason(), Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespostaErro> tratarValidacao(MethodArgumentNotValidException excecao) {
        Map<String, String> erros = excecao.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        erro -> erro.getField(),
                        erro -> erro.getDefaultMessage() == null ? "Valor invalido" : erro.getDefaultMessage(),
                        (mensagemAtual, mensagemAnterior) -> mensagemAtual,
                        LinkedHashMap::new));
        log.warn("Falha de validacao no forum: campos={}", erros.keySet());
        return ResponseEntity.badRequest().body(new RespostaErro("Dados invalidos", erros));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespostaErro> tratarJsonInvalido(HttpMessageNotReadableException excecao) {
        log.warn("JSON invalido recebido pelo forum", excecao);
        return ResponseEntity.badRequest().body(new RespostaErro("JSON invalido", Map.of()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<RespostaErro> tratarRotaNaoEncontrada(NoResourceFoundException excecao) {
        log.warn("Rota nao encontrada no forum: {}", excecao.getResourcePath());
        return ResponseEntity.status(404).body(new RespostaErro("Rota nao encontrada", Map.of()));
    }
}
