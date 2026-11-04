package com.cursovalido.backend.authentication.configuration;

import com.cursovalido.backend.authentication.dto.RespostaCodigo2faErro;
import com.cursovalido.backend.authentication.dto.RespostaTermosPendentes;
import com.cursovalido.backend.authentication.exception.Codigo2faExpiradoException;
import com.cursovalido.backend.authentication.exception.EmailNaoConfirmadoException;
import com.cursovalido.backend.authentication.exception.ServicoEmailIndisponivelException;
import com.cursovalido.backend.authentication.exception.TermosNaoAceitosException;
import com.cursovalido.backend.authentication.exception.ConviteAcessoInvalidoException;
import org.springframework.http.ResponseEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice(basePackages = "com.cursovalido.backend.authentication")
public class AutenticacaoExceptionHandler {
    @ExceptionHandler(com.cursovalido.backend.authentication.exception.BanimentoException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public RespostaCodigo2faErro banimento(com.cursovalido.backend.authentication.exception.BanimentoException excecao) {
        return new RespostaCodigo2faErro(excecao.getReason(), "CONTA_SUSPENSA");
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<RespostaCodigo2faErro> operacaoRecusada(ResponseStatusException excecao) {
        String motivo = excecao.getReason() == null ? "Operacao recusada" : excecao.getReason();
        return ResponseEntity.status(excecao.getStatusCode()).headers(excecao.getHeaders())
                .body(new RespostaCodigo2faErro(motivo, "OPERACAO_RECUSADA"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public RespostaCodigo2faErro corpoInvalido(HttpMessageNotReadableException excecao) {
        return new RespostaCodigo2faErro("Corpo JSON invalido. Verifique os campos e os valores informados.",
                "JSON_INVALIDO");
    }

    @ExceptionHandler(ConviteAcessoInvalidoException.class)
    public ResponseEntity<RespostaCodigo2faErro> conviteInvalido(ConviteAcessoInvalidoException excecao) {
        return ResponseEntity.status(excecao.getStatus())
                .body(new RespostaCodigo2faErro(excecao.getMessage(), excecao.getCodigo()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> camposInvalidos(MethodArgumentNotValidException excecao) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (var erro : excecao.getBindingResult().getFieldErrors())
            campos.put(erro.getField(), erro.getDefaultMessage());
        return Map.of("erro", "Verifique os campos informados", "campos", campos);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public RespostaCodigo2faErro requisicaoInvalida(IllegalArgumentException excecao) {
        return new RespostaCodigo2faErro(excecao.getMessage(), "REQUISICAO_INVALIDA");
    }

    @ExceptionHandler(EmailNaoConfirmadoException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public RespostaCodigo2faErro emailNaoConfirmado(
            EmailNaoConfirmadoException excecao) {
        return new RespostaCodigo2faErro(excecao.getReason(), EmailNaoConfirmadoException.CODIGO);
    }
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
