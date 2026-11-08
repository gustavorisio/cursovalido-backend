package com.cursovalido.backend.authentication.service;

import static org.junit.jupiter.api.Assertions.*;

import com.cursovalido.backend.authentication.dto.*;
import com.cursovalido.backend.authentication.security.CriptografiaDadosConverter;
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
    static void fechar() { FACTORY.close(); }

    @ParameterizedTest
    @ValueSource(strings = { "nomeCompleto", "email", "senha", "cpf", "dataNascimento", "telefone",
            "declarouMaiorIdade", "aceitouTermosLgpd", "versaoTermos" })
    void todosCamposDoCadastroSaoObrigatorios(String campo) {
        assertFalse(VALIDATOR.validateValue(CadastroUsuarioRequest.class, campo, null).isEmpty());
    }

    @ParameterizedTest
    @CsvSource({ "nomeCompleto,120", "telefone,20", "versaoTermos,30" })
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
    void criptografiaUsaIvDiferenteERejeitaConteudoAdulterado() {
        var converter = new CriptografiaDadosConverter();
        String primeiro = converter.convertToDatabaseColumn("Rua A");
        String segundo = converter.convertToDatabaseColumn("Rua A");
        assertNotEquals(primeiro, segundo);
        byte[] dados = java.util.Base64.getDecoder().decode(primeiro);
        dados[dados.length - 1] ^= 1;
        String adulterado = java.util.Base64.getEncoder().encodeToString(dados);
        assertThrows(IllegalStateException.class, () -> converter.convertToEntityAttribute(adulterado));
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
        assertTrue(VALIDATOR.validateValue(CadastroUsuarioRequest.class, "senha", "a".repeat(72)).isEmpty());
        assertFalse(VALIDATOR.validateValue(CadastroUsuarioRequest.class, "senha", "á".repeat(37)).isEmpty());
        assertFalse(VALIDATOR.validateValue(AlteracaoSenhaRequest.class, "novaSenha", "á".repeat(37)).isEmpty());
    }
}
