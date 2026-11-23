package com.cursovalido.backend.authentication.validation.senha;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Política de senha única para cadastro, convite, recuperação e alteração.
// Limita a senha ao tamanho aceito pelo BCrypt.
public final class RegrasSenha {
    public static final int MAXIMO_BYTES = 72;
    public static final int MINIMO_CARACTERES = 8;

    public static final String MENSAGEM_MINIMO = "A senha deve ter pelo menos 8 caracteres.";
    public static final String MENSAGEM_MAXIMO = "A senha deve ter no máximo 72 bytes em UTF-8. Use uma senha mais curta.";
    public static final String MENSAGEM_MAIUSCULA = "A senha deve ter pelo menos uma letra maiúscula.";
    public static final String MENSAGEM_MINUSCULA = "A senha deve ter pelo menos uma letra minúscula.";
    public static final String MENSAGEM_ESPECIAL = "A senha deve ter pelo menos um caractere especial, como ! ou @.";

    private RegrasSenha() {
    }

    // Lista as regras de senha não atendidas.
    public static List<String> problemas(String senha) {
        List<String> problemas = new ArrayList<>();
        String valor = senha == null ? "" : senha;
        if (valor.codePointCount(0, valor.length()) < MINIMO_CARACTERES)
            problemas.add(MENSAGEM_MINIMO);
        if (valor.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES)
            problemas.add(MENSAGEM_MAXIMO);
        if (valor.codePoints().noneMatch(Character::isUpperCase))
            problemas.add(MENSAGEM_MAIUSCULA);
        if (valor.codePoints().noneMatch(Character::isLowerCase))
            problemas.add(MENSAGEM_MINUSCULA);
        if (valor.codePoints().noneMatch(RegrasSenha::especial))
            problemas.add(MENSAGEM_ESPECIAL);
        return problemas;
    }

    public static Optional<String> primeiroProblema(String senha) {
        List<String> problemas = problemas(senha);
        return List.of(MENSAGEM_MAIUSCULA, MENSAGEM_MINUSCULA, MENSAGEM_ESPECIAL,
                MENSAGEM_MINIMO, MENSAGEM_MAXIMO).stream()
                .filter(problemas::contains)
                .findFirst();
    }

    public static boolean valida(String senha) {
        return problemas(senha).isEmpty();
    }

    private static boolean especial(int caractere) {
        return !Character.isLetterOrDigit(caractere) && !Character.isWhitespace(caractere)
                && !Character.isSpaceChar(caractere) && !Character.isISOControl(caractere);
    }
}
