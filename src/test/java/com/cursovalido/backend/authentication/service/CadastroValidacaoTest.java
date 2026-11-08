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
    @ValueSource(strings = { "nomeCompleto", "email", "senha", "cpf", "dataNascimento",
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

    @ParameterizedTest
    @ValueSource(strings = { "52998224725", "11144477735", "12345678909" })
    void aceitaCpfComDigitosVerificadoresValidos(String cpf) {
        assertTrue(VALIDATOR.validateValue(CadastroUsuarioRequest.class, "cpf", cpf).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = { "12345678901", "52998224724", "5299822472", "529982247250",
            "529.982.247-25", "abc52998224725", "", "00000000000", "11111111111",
            "22222222222", "33333333333", "44444444444", "55555555555", "66666666666",
            "77777777777", "88888888888", "99999999999" })
    void rejeitaCpfInvalidoOuForaDoFormatoDaApi(String cpf) {
        assertFalse(VALIDATOR.validateValue(CadastroUsuarioRequest.class, "cpf", cpf).isEmpty());
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
