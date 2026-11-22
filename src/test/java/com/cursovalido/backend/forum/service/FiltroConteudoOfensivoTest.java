package com.cursovalido.backend.forum.service;

import com.cursovalido.backend.forum.exception.ConteudoOfensivoException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class FiltroConteudoOfensivoTest {
    private final FiltroConteudoOfensivo filtro = new FiltroConteudoOfensivo();

    @ParameterizedTest
    @ValueSource(strings = { "Que porra é essa", "PORRAAA", "p0rr4", "p o r r a", "Você é um idiota",
            "seu caralhinho", "isso é merdinha", "ele é lixo humano", "fuck you", "Arrombados", "v-i-a-d-o" })
    void detectaPalavroesVariacoesEDisfarces(String texto) {
        assertThat(filtro.contemTermoOfensivo(texto)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "Olá, alguém pode ajudar com Java?", "A cumbuca está cheia", "Computador novo",
            "Espetacular aula" })
    void naoBloqueiaTextoComumComTrechosParecidos(String texto) {
        assertThat(filtro.contemTermoOfensivo(texto)).isFalse();
    }

    @Test
    void textoVazioNuloOuEmBrancoNaoEOfensivo() {
        assertThat(filtro.contemTermoOfensivo(null)).isFalse();
        assertThat(filtro.contemTermoOfensivo("   ")).isFalse();
    }

    @Test
    void validarRecusaQuandoQualquerTextoTemOfensa() {
        assertThatCode(() -> filtro.validar("Dúvida sobre Spring", "Como configurar o banco?"))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> filtro.validar("Dúvida sobre Spring", "que merda"))
                .isInstanceOf(ConteudoOfensivoException.class);
    }
    @ParameterizedTest
    @ValueSource(strings = { "lixo", "fodace", "foda-se", "pau no cu", "idiota", "otario", "OTÁRIO", "p0rr4", "p o r r a" })
    void ocultaTermosSolicitadosPreservandoRestante(String termo) {
        assertThat(filtro.censurar("Olá, " + termo + ". Tudo bem?"))
                .isEqualTo("Olá, ***. Tudo bem?");
    }

    @Test
    void preservaTextoComumEAsteriscosExistentes() {
        assertThat(filtro.censurar("Curso de estruturas: cumbuca, computador e espetáculo!"))
                .isEqualTo("Curso de estruturas: cumbuca, computador e espetáculo!");
        assertThat(filtro.censurar("Olá, ***." )).isEqualTo("Olá, ***.");
    }
}
