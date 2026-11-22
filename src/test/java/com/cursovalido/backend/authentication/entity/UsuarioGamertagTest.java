package com.cursovalido.backend.authentication.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UsuarioGamertagTest {
    @Test
    void novasTagsContemOitoCaracteresComLetrasENumerosEApenasUmPrefixo() {
        var tags = new HashSet<String>();
        for (int i = 0; i < 500; i++) {
            var usuario = new Usuario();
            ReflectionTestUtils.invokeMethod(usuario, "gerarGamertagSeAusente");
            assertThat(usuario.getGamertag()).matches("#[a-z0-9]{8}")
                    .containsPattern("[a-z]").containsPattern("[0-9]");
            tags.add(usuario.getGamertag());
            ReflectionTestUtils.invokeMethod(usuario, "gerarGamertagSeAusente");
            assertThat(tags).contains(usuario.getGamertag());
        }
        assertThat(tags).hasSize(500);
    }
}
