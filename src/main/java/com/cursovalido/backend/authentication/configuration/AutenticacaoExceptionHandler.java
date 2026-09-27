package com.cursovalido.backend.authentication.configuration;

import com.cursovalido.backend.authentication.dto.RespostaCodigo2faErro;
import com.cursovalido.backend.authentication.dto.RespostaTermosPendentes;
import com.cursovalido.backend.authentication.exception.Codigo2faExpiradoException;
import com.cursovalido.backend.authentication.exception.ServicoEmailIndisponivelException;
import com.cursovalido.backend.authentication.exception.TermosNaoAceitosException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AutenticacaoExceptionHandler {
    @ExceptionHandler(Codigo2faExpiradoException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public RespostaCodigo2faErro codigo2faExpirado(Codigo2faExpiradoException excecao) {
        return new RespostaCodigo2faErro(excecao.getReason(), Codigo2faExpiradoException.CODIGO);
    }

    @ExceptionHandler(ServicoEmailIndisponivelException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public RespostaCodigo2faErro servicoEmailIndisponivel(ServicoEmailIndisponivelException excecao) {
        return new RespostaCodigo2faErro(excecao.getReason(), ServicoEmailIndisponivelException.CODIGO);
    }

    @ExceptionHandler(TermosNaoAceitosException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public RespostaTermosPendentes termosPendentes(TermosNaoAceitosException excecao) {
        return new RespostaTermosPendentes(excecao.getReason(), TermosNaoAceitosException.CODIGO,
                excecao.getToken());
    }
}