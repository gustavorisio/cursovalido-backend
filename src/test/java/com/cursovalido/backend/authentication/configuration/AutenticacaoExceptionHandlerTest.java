package com.cursovalido.backend.authentication.configuration;

import com.cursovalido.backend.authentication.exception.ConviteAcessoInvalidoException;
import com.cursovalido.backend.forum.exception.ConteudoOfensivoException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.*;

class AutenticacaoExceptionHandlerTest {
    private final AutenticacaoExceptionHandler handler = new AutenticacaoExceptionHandler();

    @Test
    void conteudoOfensivoTemCodigoProprio() {
        var ofensa = handler.conteudoOfensivo(new ConteudoOfensivoException());
        assertThat(ofensa.codigo()).isEqualTo(ConteudoOfensivoException.CODIGO);
        assertThat(ofensa.erro()).isEqualTo(ConteudoOfensivoException.MENSAGEM);
    }

    @Test
    void conviteInvalidoUsaStatusECodigoDaRegra() {
        var resposta = handler.conviteInvalido(new ConviteAcessoInvalidoException(HttpStatus.TOO_MANY_REQUESTS,
                "REENVIO_CONVITE_AGUARDE", "Aguarde 2 minutos para reenviar."));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(resposta.getBody().codigo()).isEqualTo("REENVIO_CONVITE_AGUARDE");
        assertThat(resposta.getBody().erro()).isEqualTo("Aguarde 2 minutos para reenviar.");
    }

    @Test
    @SuppressWarnings("unchecked")
    void campoComVariosErrosPriorizaOAvisoDePreenchimento() throws Exception {
        var resultado = new BeanPropertyBindingResult(new Object(), "cadastro");
        resultado.addError(erro("senha", "SenhaValida", "A senha deve ter pelo menos uma letra maiúscula."));
        resultado.addError(erro("senha", "NotBlank", "Informe uma senha."));
        resultado.addError(erro("email", "Email", "Digite um e-mail válido."));
        resultado.addError(erro("email", "Size", "O e-mail deve ter no máximo 254 caracteres."));
        var parametro = new MethodParameter(Object.class.getMethod("equals", Object.class), 0);

        Map<String, Object> resposta = handler.camposInvalidos(new MethodArgumentNotValidException(parametro, resultado));

        assertThat(resposta).containsEntry("codigo", "CAMPOS_INVALIDOS");
        var campos = (Map<String, String>) resposta.get("campos");
        assertThat(campos).containsEntry("senha", "Informe uma senha.");
        assertThat(campos).containsEntry("email", "Digite um e-mail válido.");
    }

    @Test
    void erroDeRegraDeNegocioDevolveAMensagemParaOUsuario() {
        var resposta = handler.requisicaoInvalida(new IllegalArgumentException("Use o mesmo e-mail que recebeu o convite."));
        assertThat(resposta.codigo()).isEqualTo("REQUISICAO_INVALIDA");
        assertThat(resposta.erro()).isEqualTo("Use o mesmo e-mail que recebeu o convite.");
    }

    private static FieldError erro(String campo, String codigo, String mensagem) {
        return new FieldError("cadastro", campo, null, false, new String[] { codigo + "." + campo, codigo }, null,
                mensagem);
    }
}
