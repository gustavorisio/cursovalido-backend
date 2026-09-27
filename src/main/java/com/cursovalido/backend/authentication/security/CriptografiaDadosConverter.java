package com.cursovalido.backend.authentication.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@Converter
public class CriptografiaDadosConverter implements AttributeConverter<String, String> {
    private static final int IV_SIZE = 12;
    private static final int TAG_SIZE = 128;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final SecretKeySpec KEY = criarChave();

    @Override
    public String convertToDatabaseColumn(String valor) {
        if (valor == null || valor.isBlank()) {
            return valor;
        }
        try {
            byte[] iv = new byte[IV_SIZE];
            RANDOM.nextBytes(iv);
            Cipher cifra = Cipher.getInstance("AES/GCM/NoPadding");
            cifra.init(Cipher.ENCRYPT_MODE, KEY, new GCMParameterSpec(TAG_SIZE, iv));
            byte[] cifrado = cifra.doFinal(valor.getBytes(StandardCharsets.UTF_8));
            byte[] resultado = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, resultado, 0, iv.length);
            System.arraycopy(cifrado, 0, resultado, iv.length, cifrado.length);
            return Base64.getEncoder().encodeToString(resultado);
        } catch (GeneralSecurityException erro) {
            throw new IllegalStateException("Nao foi possivel criptografar dado pessoal", erro);
        }
    }

    @Override
    public String convertToEntityAttribute(String valor) {
        if (valor == null || valor.isBlank()) {
            return valor;
        }
        try {
            byte[] dados = Base64.getDecoder().decode(valor);
            byte[] iv = new byte[IV_SIZE];
            byte[] cifrado = new byte[dados.length - IV_SIZE];
            System.arraycopy(dados, 0, iv, 0, IV_SIZE);
            System.arraycopy(dados, IV_SIZE, cifrado, 0, cifrado.length);
            Cipher cifra = Cipher.getInstance("AES/GCM/NoPadding");
            cifra.init(Cipher.DECRYPT_MODE, KEY, new GCMParameterSpec(TAG_SIZE, iv));
            return new String(cifra.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException erro) {
            // Permite ler registros antigos para que sejam regravados de forma protegida.
            return valor;
        }
    }

    private static SecretKeySpec criarChave() {
        String chave = System.getenv().getOrDefault("DADOS_CRIPTOGRAFIA_CHAVE",
            "0123456789abcdef0123456789abcdef");
        if (chave.getBytes(StandardCharsets.UTF_8).length != 32) {
            throw new IllegalStateException("DADOS_CRIPTOGRAFIA_CHAVE deve ter 32 bytes");
        }
        return new SecretKeySpec(chave.getBytes(StandardCharsets.UTF_8), "AES");
    }
}