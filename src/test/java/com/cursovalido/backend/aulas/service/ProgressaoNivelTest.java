package com.cursovalido.backend.aulas.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ProgressaoNivelTest {
    @Test
    void metaCresceACadaNivelEFicaLimitadaAoNivelMaximo() {
        assertThat(ProgressaoNivel.meta(1)).isEqualTo(100);
        assertThat(ProgressaoNivel.meta(2)).isEqualTo(150);
        assertThat(ProgressaoNivel.meta(3)).isEqualTo(230);
        assertThat(ProgressaoNivel.meta(4)).isGreaterThan(ProgressaoNivel.meta(3));
        assertThat(ProgressaoNivel.meta(0)).isEqualTo(ProgressaoNivel.meta(1));
        assertThat(ProgressaoNivel.meta(500)).isEqualTo(ProgressaoNivel.meta(ProgressaoNivel.NIVEL_MAXIMO));
    }

    @Test
    void ganhoSemAtingirMetaMantemNivel() {
        assertThat(ProgressaoNivel.aplicar(1, 20, 50)).isEqualTo(new ProgressaoNivel.Resultado(1, 70));
    }

    @Test
    void ganhoGrandeSobeVariosNiveisEGuardaOSaldo() {
        var resultado = ProgressaoNivel.aplicar(1, 0, 300);
        assertThat(resultado.nivel()).isEqualTo(3);
        assertThat(resultado.xpNivel()).isEqualTo(50);
    }

    @Test
    void valoresNegativosNaoReduzemXpNemNivel() {
        assertThat(ProgressaoNivel.aplicar(0, -10, -5)).isEqualTo(new ProgressaoNivel.Resultado(1, 0));
    }

    @Test
    void nivelMaximoNaoAvanca() {
        var resultado = ProgressaoNivel.aplicar(ProgressaoNivel.NIVEL_MAXIMO, 0, 1_000_000);
        assertThat(resultado.nivel()).isEqualTo(ProgressaoNivel.NIVEL_MAXIMO);
        assertThat(resultado.xpNivel()).isEqualTo(1_000_000);
    }
}
