package com.cursovalido.backend.authentication.validation.politica;

import java.util.regex.Pattern;

public final class ConteudoDocumento {
    private ConteudoDocumento() {
    }

    // Titulo e versao sao metadados do sistema, nao parte do texto revisado.
    // Aceita tambem os HTML completos usados nas publicacoes legadas.
    public static String preparar(String conteudo) {
        var corpo = Pattern.compile("(?is)<body\\b[^>]*>(.*?)</body\\s*>").matcher(conteudo);
        String texto = corpo.find() ? corpo.group(1) : conteudo;
        texto = texto.replaceFirst("(?is)^\\s*<h1\\b[^>]*>.*?</h1>\\s*", "")
                .replaceFirst("(?is)^\\s*<p\\b[^>]*>\\s*(?:<strong\\b[^>]*>)?Vers[aã]o\\b.*?</p>\\s*", "")
                .trim();
        if (texto.isBlank()) {
            throw new IllegalArgumentException("Informe o conteudo do documento alem do titulo e da versao");
        }
        return texto;
    }
}
