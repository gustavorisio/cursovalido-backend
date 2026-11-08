package com.cursovalido.backend.authentication.configuration;

import com.cursovalido.backend.authentication.dto.Codigo2faErroResponse;
import com.cursovalido.backend.authentication.dto.TermosPendentesResponse;
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
    @ExceptionHandler(com.cursovalido.backend.authentication.exception.ValidacaoCpfException.class)
    public ResponseEntity<Codigo2faErroResponse> validacaoCpf(
            com.cursovalido.backend.authentication.exception.ValidacaoCpfException excecao) {
        return ResponseEntity.status(excecao.getStatusCode())
                .body(new Codigo2faErroResponse(excecao.getReason(), excecao.getCodigo()));
    }
    @ExceptionHandler(com.cursovalido.backend.authentication.suporte.LimiteSuporteException.class)
    public ResponseEntity<Codigo2faErroResponse> limiteSuporte(
            com.cursovalido.backend.authentication.suporte.LimiteSuporteException excecao) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).headers(excecao.getHeaders())
                .body(new Codigo2faErroResponse(excecao.getReason(),
                        com.cursovalido.backend.authentication.suporte.LimiteSuporteException.CODIGO));
    }
    @ExceptionHandler(com.cursovalido.backend.authentication.exception.SuporteIndisponivelException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Codigo2faErroResponse suporteIndisponivel(
            com.cursovalido.backend.authentication.exception.SuporteIndisponivelException excecao) {
        return new Codigo2faErroResponse(excecao.getReason(),
                com.cursovalido.backend.authentication.exception.SuporteIndisponivelException.CODIGO);
    }

    @ExceptionHandler(com.cursovalido.backend.authentication.exception.BanimentoException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Codigo2faErroResponse banimento(com.cursovalido.backend.authentication.exception.BanimentoException excecao) {
        return new Codigo2faErroResponse(excecao.getReason(), "CONTA_SUSPENSA");
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Codigo2faErroResponse> operacaoRecusada(ResponseStatusException excecao) {
        String motivo = excecao.getReason() == null ? "Operacao recusada" : excecao.getReason();
        return ResponseEntity.status(excecao.getStatusCode()).headers(excecao.getHeaders())
                .body(new Codigo2faErroResponse(motivo, "OPERACAO_RECUSADA"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Codigo2faErroResponse corpoInvalido(HttpMessageNotReadableException excecao) {
        return new Codigo2faErroResponse("Corpo JSON invalido. Verifique os campos e os valores informados.",
                "JSON_INVALIDO");
    }

    @ExceptionHandler(ConviteAcessoInvalidoException.class)
    public ResponseEntity<Codigo2faErroResponse> conviteInvalido(ConviteAcessoInvalidoException excecao) {
        return ResponseEntity.status(excecao.getStatus())
                .body(new Codigo2faErroResponse(excecao.getMessage(), excecao.getCodigo()));
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
    public Codigo2faErroResponse requisicaoInvalida(IllegalArgumentException excecao) {
        return new Codigo2faErroResponse(excecao.getMessage(), "REQUISICAO_INVALIDA");
    }

    @ExceptionHandler(EmailNaoConfirmadoException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Codigo2faErroResponse emailNaoConfirmado(
            EmailNaoConfirmadoException excecao) {
        return new Codigo2faErroResponse(excecao.getReason(), EmailNaoConfirmadoException.CODIGO);
    }
    @ExceptionHandler(Codigo2faExpiradoException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Codigo2faErroResponse codigo2faExpirado(Codigo2faExpiradoException excecao) {
        return new Codigo2faErroResponse(excecao.getReason(), Codigo2faExpiradoException.CODIGO);
    }

    @ExceptionHandler(ServicoEmailIndisponivelException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Codigo2faErroResponse servicoEmailIndisponivel(ServicoEmailIndisponivelException excecao) {
        return new Codigo2faErroResponse(excecao.getReason(), ServicoEmailIndisponivelException.CODIGO);
    }

    @ExceptionHandler(TermosNaoAceitosException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public TermosPendentesResponse termosPendentes(TermosNaoAceitosException excecao) {
        return new TermosPendentesResponse(excecao.getReason(), TermosNaoAceitosException.CODIGO,
                excecao.getToken());
    }
}
