package com.cursovalido.backend.forum.service;

import com.cursovalido.backend.forum.exception.ConteudoOfensivoException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class FiltroConteudoOfensivo {
    // Termos comparados palavra a palavra, sem acentos e sem letras repetidas.
    private static final Set<String> TERMOS = normalizarLista(List.of(
            "porra", "porras", "caralho", "caralhos", "carai", "caraio", "krl", "merda", "merdas", "bosta", "bostas", "cacete", "foda", "fodase",
            "foder", "fode", "fodeu", "fodido", "fodida", "fuder", "fudido", "fudida", "fdp", "pqp", "vsf",
            "vtnc", "tnc", "puta", "puto", "putaria", "putinha", "buceta", "boceta", "xoxota", "piroca", "cu",
            "cuzao", "cuzinho", "arrombado", "arrombada", "desgraca", "desgracado", "desgracada", "otario",
            "otaria", "idiota", "idiotas", "imbecil", "imbecis", "retardado", "retardada", "retardados", "babaca", "babacas", "escroto", "escrota",
            "vagabundo", "vagabunda", "vadia", "vadias", "corno", "cornos", "corna", "cornas", "viado", "viados", "sapatao", "sapatoes", "traveco", "travecos", "mongoloide",
            "lixo humano", "fuck", "fucking", "fucker", "motherfucker", "shit", "bitch", "asshole", "cunt",
            "nigger", "niggers", "nigga", "niggas", "faggot", "faggots", "retard"));

    // Radicais que cobrem variacoes como plural, diminutivo e aumentativo.
    private static final List<String> RADICAIS = normalizarLista(List.of(
            "caralh", "arromb", "desgracad", "fodid", "fudid", "otari", "retardad", "escrot", "viadinh",
            "bucet", "bocet", "putinh", "putaria", "cuza", "merdinh", "babac", "imbecil", "idiot", "vagabund",
            "motherfuck", "fuck", "bitch", "asshol")).stream().toList();

    public void validar(String... textos) {
        for (String texto : textos) {
            if (contemTermoOfensivo(texto))
                throw new ConteudoOfensivoException();
        }
    }

    public boolean contemTermoOfensivo(String texto) {
        if (texto == null || texto.isBlank())
            return false;
        List<String> palavras = palavras(texto);
        for (String palavra : palavras) {
            if (ofensiva(palavra))
                return true;
        }
        for (String juncao : juncoes(palavras)) {
            if (ofensiva(juncao))
                return true;
        }
        return false;
    }

    private boolean ofensiva(String palavra) {
        if (palavra.isEmpty())
            return false;
        if (TERMOS.contains(palavra))
            return true;
        for (String radical : RADICAIS) {
            if (palavra.startsWith(radical))
                return true;
        }
        return false;
    }

    // Junta letras soltas e pares de palavras para detectar "p o r r a" e "lixo humano".
    private List<String> juncoes(List<String> palavras) {
        List<String> resultado = new ArrayList<>();
        StringBuilder letras = new StringBuilder();
        for (String palavra : palavras) {
            if (palavra.length() == 1) {
                letras.append(palavra);
                continue;
            }
            if (letras.length() > 1)
                resultado.add(colapsar(letras.toString()));
            letras.setLength(0);
        }
        if (letras.length() > 1)
            resultado.add(colapsar(letras.toString()));
        for (int i = 0; i + 1 < palavras.size(); i++)
            resultado.add(palavras.get(i) + " " + palavras.get(i + 1));
        return resultado;
    }

    private static List<String> palavras(String texto) {
        String normalizado = normalizar(texto);
        List<String> resultado = new ArrayList<>();
        for (String parte : normalizado.split("[^a-z]+")) {
            if (!parte.isEmpty())
                resultado.add(colapsar(parte));
        }
        return resultado;
    }

    private static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        StringBuilder convertido = new StringBuilder(semAcento.length());
        for (char caractere : semAcento.toLowerCase(Locale.ROOT).toCharArray()) {
            convertido.append(switch (caractere) {
                case '0' -> 'o';
                case '1', '!' -> 'i';
                case '3' -> 'e';
                case '4', '@' -> 'a';
                case '5', '$' -> 's';
                case '7' -> 't';
                default -> caractere;
            });
        }
        return convertido.toString();
    }

    private static String colapsar(String palavra) {
        StringBuilder resultado = new StringBuilder(palavra.length());
        char anterior = 0;
        for (char caractere : palavra.toCharArray()) {
            if (caractere != anterior)
                resultado.append(caractere);
            anterior = caractere;
        }
        return resultado.toString();
    }

    private static Set<String> normalizarLista(List<String> termos) {
        return termos.stream()
                .map(termo -> String.join(" ", palavras(termo)))
                .collect(Collectors.toUnmodifiableSet());
    }
}
