package com.cursovalido.backend.forum.service;

import com.cursovalido.backend.forum.exception.ConteudoOfensivoException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
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
            "lixo", "lixo humano", "fodace", "foda-se", "pau no cu", "fuck", "fucking", "fucker", "motherfucker", "shit", "bitch", "asshole", "cunt",
            "nigger", "niggers", "nigga", "niggas", "faggot", "faggots", "retard"));

    // Radicais que cobrem variações como plural, diminutivo e aumentativo.
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
        return texto != null && !texto.equals(censurar(texto));
    }

    // Preserva os índices do texto ao ocultar ofensas.
    public String censurar(String texto) {
        if (texto == null || texto.isBlank()) return texto;
        var tokens = new ArrayList<Token>();
        var matcher = Pattern.compile("[\\p{L}\\p{M}\\p{N}@$]+").matcher(texto);
        while (matcher.find()) {
            tokens.add(new Token(colapsar(normalizar(matcher.group())), matcher.start(), matcher.end()));
        }
        boolean[] ocultos = new boolean[texto.length()];
        for (int i = 0; i < tokens.size(); i++) {
            StringBuilder frase = new StringBuilder();
            for (int j = i; j < tokens.size() && j < i + 4; j++) {
                if (j > i) frase.append(' ');
                frase.append(tokens.get(j).valor());
                if (ofensiva(frase.toString())) marcar(ocultos, tokens.get(i).inicio(), tokens.get(j).fim());
            }
            if (tokens.get(i).valor().length() == 1) {
                StringBuilder letras = new StringBuilder();
                for (int j = i; j < tokens.size() && j < i + 32 && tokens.get(j).valor().length() == 1; j++) {
                    letras.append(tokens.get(j).valor());
                    if (ofensiva(colapsar(letras.toString())))
                        marcar(ocultos, tokens.get(i).inicio(), tokens.get(j).fim());
                }
            }
        }
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            if (!ocultos[i]) resultado.append(texto.charAt(i));
            else if (i == 0 || !ocultos[i - 1]) resultado.append("***");
        }
        return resultado.toString();
    }

    private record Token(String valor, int inicio, int fim) {}

    private static void marcar(boolean[] ocultos, int inicio, int fim) {
        Arrays.fill(ocultos, inicio, fim, true);
    }

    private boolean ofensiva(String palavra) {
        if (palavra.isEmpty())
            return false;
        if (TERMOS.contains(palavra))
            return true;
        if (palavra.contains(" ")) return false;
        for (String radical : RADICAIS) {
            if (palavra.startsWith(radical))
                return true;
        }
        return false;
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
