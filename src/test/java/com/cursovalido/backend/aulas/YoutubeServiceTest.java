package com.cursovalido.backend.aulas;

import com.cursovalido.backend.aulas.service.YoutubeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class YoutubeServiceTest {
    private final YoutubeService servico = new YoutubeService();

    @ParameterizedTest
    @ValueSource(strings = {
            "https://www.youtube.com/watch?v=M7lc1UVf-VE",
            "https://youtube.com/watch?feature=share&v=M7lc1UVf-VE&t=20",
            "https://youtu.be/M7lc1UVf-VE?si=teste",
            " https://www.youtube.com/watch?v=M7lc1UVf-VE " })
    void extrairIdDeLinkValido(String url) {
        assertThat(servico.extrairVideoId(url)).isEqualTo("M7lc1UVf-VE");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            " ", "nao e url", "M7lc1UVf-VE", "https://youtu.be/",
            "http://youtube.com/watch?v=M7lc1UVf-VE",
            "https://youtube.com.evil.com/watch?v=M7lc1UVf-VE",
            "https://youtube.com@evil.com/watch?v=M7lc1UVf-VE",
            "https://evil.com@youtube.com/watch?v=M7lc1UVf-VE",
            "https://youtube.com:443/watch?v=M7lc1UVf-VE",
            "https://www.youtube.com/watch?list=teste", "https://youtu.be/curto",
            "https://youtu.be/M7lc1UVf-VE/extra",
            "https://youtube.com/watch?v=M7lc1UVf-VE&v=M7lc1UVf-VE",
            "<iframe src='https://youtube.com/embed/M7lc1UVf-VE'></iframe>" })
    void rejeitarLinkInvalido(String url) {
        assertThatThrownBy(() -> servico.extrairVideoId(url))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void rejeitarLinkAcimaDoLimite() {
        assertThatThrownBy(() -> servico.extrairVideoId("https://youtu.be/" + "a".repeat(2048)))
                .isInstanceOf(ResponseStatusException.class);
    }
}
