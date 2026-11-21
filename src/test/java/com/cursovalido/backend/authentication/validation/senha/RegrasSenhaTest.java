package com.cursovalido.backend.authentication.validation.senha;

import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegrasSenhaTest {
    @Test
    void senhaComAsTresClassesEDentroDoLimiteEValida() {
        assertThat(RegrasSenha.valida("Senha@1x")).isTrue();
        assertThat(RegrasSenha.valida("Senha@123")).isTrue();
        assertThat(RegrasSenha.problemas("Senha@1x")).isEmpty();
    }

    @Test
    void cadaRegraQueFaltaGeraSuaPropriaMensagem() {
        assertThat(RegrasSenha.primeiroProblema("abc!")).contains(RegrasSenha.MENSAGEM_MAIUSCULA);
        assertThat(RegrasSenha.primeiroProblema("ABC!")).contains(RegrasSenha.MENSAGEM_MINUSCULA);
        assertThat(RegrasSenha.primeiroProblema("Abc")).contains(RegrasSenha.MENSAGEM_ESPECIAL);
        assertThat(RegrasSenha.problemas("abc"))
                .containsExactly(RegrasSenha.MENSAGEM_MINIMO, RegrasSenha.MENSAGEM_MAIUSCULA,
                        RegrasSenha.MENSAGEM_ESPECIAL);
    }

    @Test
    void limiteConsideraBytesDoBcrypt() {
        assertThat(RegrasSenha.valida("A!" + "a".repeat(70))).isTrue();
        assertThat(RegrasSenha.primeiroProblema("Aa!" + "a".repeat(70))).contains(RegrasSenha.MENSAGEM_MAXIMO);
        assertThat(RegrasSenha.problemas("á".repeat(37))).contains(RegrasSenha.MENSAGEM_MAXIMO);
    }

    @Test
    void espacoNaoContaComoCaractereEspecial() {
        assertThat(RegrasSenha.primeiroProblema("Abc def")).contains(RegrasSenha.MENSAGEM_ESPECIAL);
        assertThat(RegrasSenha.problemas(null)).contains(RegrasSenha.MENSAGEM_MAIUSCULA);
    }

    @Test
    void validadorDeixaCampoVazioParaONotBlankEInformaARegraQueFaltou() {
        var validador = new SenhaValidaValidator();
        var contexto = mock(ConstraintValidatorContext.class);
        var construtor = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(contexto.buildConstraintViolationWithTemplate(anyString())).thenReturn(construtor);

        assertThat(validador.isValid(null, contexto)).isTrue();
        assertThat(validador.isValid("", contexto)).isTrue();
        assertThat(validador.isValid("Senha@1x", contexto)).isTrue();
        assertThat(validador.isValid("senha@1", contexto)).isFalse();
        verify(contexto).disableDefaultConstraintViolation();
        verify(contexto).buildConstraintViolationWithTemplate(RegrasSenha.MENSAGEM_MAIUSCULA);
        verify(construtor).addConstraintViolation();
        assertThat(validador.isValid("senha", null)).isFalse();
    }

    @Test
    void alteracaoAutenticadaConfereSenhaAtualRegrasConfirmacaoESenhaRepetida() {
        PasswordEncoder senhas = mock(PasswordEncoder.class);
        Usuario usuario = new Usuario();
        usuario.setSenha("hash");
        when(senhas.matches("Atual@1", "hash")).thenReturn(true);

        assertThatThrownBy(() -> SenhaUsuarioValidation.validarAlteracao(usuario, "errada", "Nova@1", "Nova@1", senhas))
                .hasMessage("Senha atual incorreta.");
        assertThatThrownBy(() -> SenhaUsuarioValidation.validarAlteracao(usuario, "Atual@1", "nova@1", "nova@1", senhas))
                .hasMessage(RegrasSenha.MENSAGEM_MAIUSCULA);
        assertThatThrownBy(() -> SenhaUsuarioValidation.validarAlteracao(usuario, "Atual@1", "Nova@1", "Outra@1", senhas))
                .hasMessage(SenhaUsuarioValidation.MENSAGEM_CONFIRMACAO);
        assertThatThrownBy(() -> SenhaUsuarioValidation.validarAlteracao(usuario, "Atual@1", "Atual@1", "Atual@1", senhas))
                .hasMessage("A nova senha deve ser diferente da senha atual.");
        assertThatCode(() -> SenhaUsuarioValidation.validarAlteracao(usuario, "Atual@1", "Nova@1", "Nova@1", senhas))
                .doesNotThrowAnyException();
    }

    @Test
    void recuperacaoAuditaConfirmacaoDiferenteEAplicaAsRegras() {
        LogAuditoriaService auditoria = mock(LogAuditoriaService.class);

        assertThatThrownBy(() -> SenhaUsuarioValidation.validarRecuperacao("Nova@1", "Outra@1", auditoria))
                .hasMessage(SenhaUsuarioValidation.MENSAGEM_CONFIRMACAO);
        verify(auditoria).registrar(null, "RECUPERACAO_FALHOU", "Senhas diferentes", null);
        assertThatThrownBy(() -> SenhaUsuarioValidation.validarRecuperacao("nova", "nova", auditoria))
                .hasMessage(RegrasSenha.MENSAGEM_MAIUSCULA);
        assertThatCode(() -> SenhaUsuarioValidation.validarRecuperacao("Nova@1", "Nova@1", auditoria))
                .doesNotThrowAnyException();
    }
}
