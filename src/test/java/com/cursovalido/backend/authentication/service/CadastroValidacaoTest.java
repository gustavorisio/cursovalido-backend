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
            "endereco", "declarouMaiorIdade", "aceitouTermosLgpd", "versaoTermos" })
    void todosCamposDoCadastroSaoObrigatorios(String campo) {
        assertFalse(VALIDATOR.validateValue(CadastroUsuarioRequisicao.class, campo, null).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = { "cep", "logradouro", "bairro", "cidade", "estado" })
    void enderecoNaoAceitaCamposAusentesOuEmBranco(String campo) {
        assertFalse(VALIDATOR.validateValue(EnderecoRequisicao.class, campo, null).isEmpty());
        assertFalse(VALIDATOR.validateValue(EnderecoRequisicao.class, campo, " ").isEmpty());
    }

    @ParameterizedTest
    @CsvSource({ "logradouro,100,440", "bairro,60,280", "cidade,80,360" })
    void limitesDoEnderecoCabemCriptografadosMesmoComUnicode(String campo, int limite, int capacidade) {
        String valor = "\u0800".repeat(limite);
        assertTrue(VALIDATOR.validateValue(EnderecoRequisicao.class, campo, valor).isEmpty());
        assertFalse(VALIDATOR.validateValue(EnderecoRequisicao.class, campo, valor + "a").isEmpty());
        var converter = new CriptografiaDadosConverter();
        String cifrado = converter.convertToDatabaseColumn(valor);
        assertTrue(cifrado.length() <= capacidade);
        assertEquals(valor, converter.convertToEntityAttribute(cifrado));
    }

    @ParameterizedTest
    @CsvSource({ "nomeCompleto,120", "telefone,20", "versaoTermos,30" })
    void rejeitaCamposAcimaDoLimite(String campo, int limite) {
        assertFalse(VALIDATOR.validateValue(CadastroUsuarioRequisicao.class, campo, "1".repeat(limite + 1)).isEmpty());
    }

    @Test
    void validaCepUfEAceites() {
        assertTrue(VALIDATOR.validate(new EnderecoRequisicao("01001000", "Rua A", "Centro", "Sao Paulo", "SP")).isEmpty());
        assertFalse(VALIDATOR.validateValue(EnderecoRequisicao.class, "cep", "01001-000").isEmpty());
        assertFalse(VALIDATOR.validateValue(EnderecoRequisicao.class, "estado", "ZZ").isEmpty());
        assertFalse(VALIDATOR.validateValue(CadastroUsuarioRequisicao.class, "aceitouTermosLgpd", false).isEmpty());
        assertFalse(VALIDATOR.validateValue(CadastroUsuarioRequisicao.class, "declarouMaiorIdade", false).isEmpty());
        assertFalse(VALIDATOR.validateValue(ConviteCadastroRequisicao.class, "cadastro", null).isEmpty());
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
        assertTrue(VALIDATOR.validateValue(CadastroUsuarioRequisicao.class, "senha", "a".repeat(72)).isEmpty());
        assertFalse(VALIDATOR.validateValue(CadastroUsuarioRequisicao.class, "senha", "á".repeat(37)).isEmpty());
        assertFalse(VALIDATOR.validateValue(AlteracaoSenhaRequisicao.class, "novaSenha", "á".repeat(37)).isEmpty());
    }
}
