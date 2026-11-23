package com.cursovalido.backend.authentication.validation.politica;

import java.util.regex.Pattern;
import org.springframework.web.util.HtmlUtils;

public final class ConteudoDocumento {
    private ConteudoDocumento() {
    }

    public static String preparar(String conteudo) {
        var corpo = Pattern.compile("(?is)<body\\b[^>]*>(.*?)</body\\s*>").matcher(conteudo);
        String texto = corpo.find() ? corpo.group(1) : conteudo;
        texto = texto.replaceFirst("(?is)^\\s*<h1\\b[^>]*>.*?</h1>\\s*", "")
                .replaceFirst("(?is)^\\s*<p\\b[^>]*>\\s*(?:<strong\\b[^>]*>)?Vers[aã]o\\b.*?</p>\\s*", "")
                .replaceAll("(?is)<(script|style|iframe|object)\\b[^>]*>.*?</\\1\\s*>", "")
                .replaceAll("(?i)<br\\s*/?>|</(?:p|div|section|article|h[1-6]|li|ul|ol|blockquote|tr)\\s*>", "\n")
                .replaceAll("<[^>]*>", "");
        texto = HtmlUtils.htmlUnescape(texto).replaceAll("[\\t\\f\\u000B ]+", " ")
                .replaceAll(" *\\n *", "\n").replaceAll("\\n{3,}", "\n\n").trim();
        if (texto.isBlank())
            throw new IllegalArgumentException("Informe o conteudo do documento alem do titulo e da versao");
        return texto;
    }
}
