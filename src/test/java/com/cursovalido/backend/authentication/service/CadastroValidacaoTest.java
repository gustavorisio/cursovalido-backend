package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.validation.senha.RegrasSenha;

import static org.junit.jupiter.api.Assertions.*;

import com.cursovalido.backend.authentication.dto.*;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class CadastroValidacaoTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void fechar() {
        FACTORY.close();
    }

    @ParameterizedTest
    @ValueSource(strings = { "nomeCompleto", "email", "senha",
            "declarouMaiorIdade", "aceitouTermosLgpd", "versaoTermos" })
    void todosCamposDoCadastroSaoObrigatorios(String campo) {
        assertFalse(VALIDATOR.validateValue(CadastroUsuarioRequest.class, campo, null).isEmpty());
    }

    @ParameterizedTest
    @CsvSource({ "nomeCompleto,120", "versaoTermos,30" })
    void rejeitaCamposAcimaDoLimite(String campo, int limite) {
        assertFalse(VALIDATOR.validateValue(CadastroUsuarioRequest.class, campo, "1".repeat(limite + 1)).isEmpty());
    }

    @Test
    void validaAceites() {
        assertFalse(VALIDATOR.validateValue(CadastroUsuarioRequest.class, "aceitouTermosLgpd", false).isEmpty());
        assertFalse(VALIDATOR.validateValue(CadastroUsuarioRequest.class, "declarouMaiorIdade", false).isEmpty());
        assertFalse(VALIDATOR.validateValue(ConviteCadastroRequest.class, "cadastro", null).isEmpty());
    }

    @Test
    void bcryptGeraSaltDiferenteParaSenhasIguais() {
        var encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(4);
        String primeiro = encoder.encode("SenhaTeste123!");
        String segundo = encoder.encode("SenhaTeste123!");
        assertNotEquals(primeiro, segundo);
        assertTrue(encoder.matches("SenhaTeste123!", primeiro));
        assertTrue(encoder.matches("SenhaTeste123!", segundo));
    }

    @Test
    void senhaRespeitaLimiteEmBytesDoBcrypt() {
        assertTrue(VALIDATOR.validateValue(CadastroUsuarioRequest.class, "senha", "A!" + "a".repeat(70)).isEmpty());
        assertFalse(VALIDATOR.validateValue(CadastroUsuarioRequest.class, "senha", "á".repeat(37)).isEmpty());
        assertFalse(VALIDATOR.validateValue(AlteracaoSenhaRequest.class, "novaSenha", "á".repeat(37)).isEmpty());
    }

    @Test
    void senhaExigeOitoCaracteresMaiusculaMinusculaEEspecial() {
        assertTrue(VALIDATOR.validateValue(CadastroUsuarioRequest.class, "senha", "Senha@1x").isEmpty());
        assertTrue(VALIDATOR.validateValue(CadastroUsuarioRequest.class, "senha", "Aa!x").stream()
                .anyMatch(erro -> erro.getMessage().equals(RegrasSenha.MENSAGEM_MINIMO)));
        var esperadas = java.util.Map.of(
                "ABC!", RegrasSenha.MENSAGEM_MINUSCULA,
                "abc!", RegrasSenha.MENSAGEM_MAIUSCULA,
                "Abc", RegrasSenha.MENSAGEM_ESPECIAL,
                "A! ", RegrasSenha.MENSAGEM_MINUSCULA,
                "123!", RegrasSenha.MENSAGEM_MAIUSCULA);
        esperadas.forEach((senha, mensagem) -> {
            var erros = VALIDATOR.validateValue(CadastroUsuarioRequest.class, "senha", senha);
            assertFalse(erros.isEmpty(), senha);
            assertTrue(erros.stream().anyMatch(erro -> erro.getMessage().equals(mensagem)), senha);
        });
    }

    @Test
    void senhaVaziaInformaApenasQueOCampoEObrigatorio() {
        var erros = VALIDATOR.validateValue(CadastroUsuarioRequest.class, "senha", "");
        assertEquals(1, erros.size());
        assertEquals("Informe uma senha.", erros.iterator().next().getMessage());
    }

    @Test
    void confirmacaoDiferenteGeraMensagemPropria() {
        var requisicao = new CadastroUsuarioRequest("Aluno Teste", "aluno@teste.com", "Senha@123", "Outra@123",
                true, true, "v1", null, null);
        var erros = VALIDATOR.validate(requisicao);
        assertTrue(erros.stream().anyMatch(erro -> erro.getMessage().equals("As senhas devem coincidir.")));
    }

    @Test
    void limitesDosCamposTemMensagemClara() {
        assertTrue(VALIDATOR.validateValue(CadastroUsuarioRequest.class, "nomeCompleto", "a".repeat(121)).stream()
                .anyMatch(erro -> erro.getMessage().contains("120 caracteres")));
        assertTrue(VALIDATOR.validateValue(CadastroUsuarioRequest.class, "email", "a".repeat(250) + "@x.com").stream()
                .anyMatch(erro -> erro.getMessage().contains("254 caracteres")));
    }
}
