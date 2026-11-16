package com.cursovalido.backend.authentication.configuration;

import com.cursovalido.backend.authentication.dto.Codigo2faErroResponse;
import com.cursovalido.backend.authentication.dto.TermosPendentesResponse;
import com.cursovalido.backend.authentication.exception.Codigo2faExpiradoException;
import com.cursovalido.backend.authentication.exception.ServicoEmailIndisponivelException;
import com.cursovalido.backend.authentication.exception.TermosNaoAceitosException;
import com.cursovalido.backend.authentication.exception.ConviteAcessoInvalidoException;
import com.cursovalido.backend.authentication.exception.CredenciaisInvalidasException;
import com.cursovalido.backend.authentication.exception.CadastroDuplicadoException;
import com.cursovalido.backend.authentication.exception.TermosNaoDisponiveisException;
import com.cursovalido.backend.authentication.exception.PendenteAprovacaoException;
import com.cursovalido.backend.authentication.exception.AcessoRejeitadoException;
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
import org.springframework.web.ErrorResponse;
import org.springframework.security.access.AccessDeniedException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class AutenticacaoExceptionHandler {
    @ExceptionHandler(CredenciaisInvalidasException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Codigo2faErroResponse credenciaisInvalidas(CredenciaisInvalidasException excecao) {
        return new Codigo2faErroResponse(excecao.getReason(), CredenciaisInvalidasException.CODIGO);
    }

    @ExceptionHandler(CadastroDuplicadoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Codigo2faErroResponse cadastroDuplicado(CadastroDuplicadoException excecao) {
        return new Codigo2faErroResponse("Este e-mail já está cadastrado. Entre na sua conta ou recupere sua senha.", "EMAIL_JA_CADASTRADO");
    }

    @ExceptionHandler(TermosNaoDisponiveisException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Codigo2faErroResponse termosAtualizados(TermosNaoDisponiveisException excecao) {
        return new Codigo2faErroResponse(excecao.getReason(), "TERMOS_ATUALIZADOS");
    }

    @ExceptionHandler(PendenteAprovacaoException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Codigo2faErroResponse cadastroPendente(PendenteAprovacaoException excecao) {
        return new Codigo2faErroResponse("Seu cadastro está aguardando aprovação. Aguarde a liberação do administrador.", "PENDENTE_APROVACAO");
    }

    @ExceptionHandler(AcessoRejeitadoException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Codigo2faErroResponse cadastroRejeitado(AcessoRejeitadoException excecao) {
        return new Codigo2faErroResponse("Seu cadastro não foi aprovado. Entre em contato pelo Suporte para pedir uma revisão.", "ACESSO_REJEITADO");
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
    public Codigo2faErroResponse banimento(
            com.cursovalido.backend.authentication.exception.BanimentoException excecao) {
        return new Codigo2faErroResponse(excecao.getReason(), "CONTA_SUSPENSA");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Codigo2faErroResponse> operacaoRecusada(ResponseStatusException excecao) {
        if (excecao.getStatusCode().is5xxServerError()) return falhaInterna(excecao, excecao.getStatusCode());
        String motivo = excecao.getReason() == null ? "Operacao recusada" : excecao.getReason();
        return ResponseEntity.status(excecao.getStatusCode()).headers(excecao.getHeaders())
                .body(new Codigo2faErroResponse(motivo, "OPERACAO_RECUSADA"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Codigo2faErroResponse corpoInvalido(HttpMessageNotReadableException excecao) {
        return new Codigo2faErroResponse("Não foi possível ler os dados enviados. Confira os campos e tente novamente.",
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
        return Map.of("erro", "Confira os campos indicados e tente novamente.", "codigo", "CAMPOS_INVALIDOS", "campos", campos);
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> parametroInvalido(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException excecao) {
        return Map.of("erro", "Não foi possível abrir esta página. Confira os dados e tente novamente.", "codigo", "PARAMETRO_INVALIDO",
                "campos", Map.of(excecao.getName(), "Confira o valor informado."));
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> parametroAusente(
            org.springframework.web.bind.MissingServletRequestParameterException excecao) {
        return Map.of("erro", "Faltam informações para continuar. Volte à página anterior e tente novamente.", "codigo", "PARAMETRO_INVALIDO",
                "campos", Map.of(excecao.getParameterName(), "Preencha este campo."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Codigo2faErroResponse requisicaoInvalida(IllegalArgumentException excecao) {
        return new Codigo2faErroResponse(excecao.getMessage(), "REQUISICAO_INVALIDA");
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

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Codigo2faErroResponse acessoNegado(AccessDeniedException excecao) {
        return new Codigo2faErroResponse("Você não tem permissão para fazer isso.", "ACESSO_NEGADO");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Codigo2faErroResponse> erroInesperado(Exception excecao) {
        if (excecao instanceof ErrorResponse erro && !erro.getStatusCode().is5xxServerError()) {
            return ResponseEntity.status(erro.getStatusCode()).headers(erro.getHeaders())
                    .cacheControl(org.springframework.http.CacheControl.noStore())
                    .body(new Codigo2faErroResponse("Requisicao nao permitida ou recurso indisponivel", "REQUISICAO_RECUSADA"));
        }
        var status = excecao instanceof ErrorResponse erro ? erro.getStatusCode() : HttpStatus.INTERNAL_SERVER_ERROR;
        return falhaInterna(excecao, status);
    }

    private ResponseEntity<Codigo2faErroResponse> falhaInterna(Exception excecao,
            org.springframework.http.HttpStatusCode status) {
        log.error("Falha interna ao atender requisicao", excecao);
        return ResponseEntity.status(status)
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(new Codigo2faErroResponse("Nao foi possivel concluir a operacao. Tente novamente mais tarde.", "ERRO_INTERNO"));
    }
}
