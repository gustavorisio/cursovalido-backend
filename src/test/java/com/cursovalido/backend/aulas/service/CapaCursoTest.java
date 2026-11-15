package com.cursovalido.backend.aulas.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class CapaCursoTest {
    private String imagem(int largura, int altura, String formato) throws Exception {
        var saida = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB), formato, saida);
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(saida.toByteArray());
    }

    @Test void aceitaJpegEPermiteRemoverCapa() throws Exception {
        CapaCurso.validar(null);
        CapaCurso.validar(imagem(550, 250, "jpeg"));
        CapaCurso.validar(imagem(275, 125, "jpeg"));
        CapaCurso.validar(imagem(1, 1, "jpeg"));
        var curso = new com.cursovalido.backend.aulas.entity.Curso();
        curso.setCapaJpeg(imagem(550, 250, "jpeg"));
        assertEquals(curso.getCapaJpeg(), com.cursovalido.backend.aulas.dto.CursoResponse.de(curso).capaJpeg());
    }

    @Test void rejeitaFormatoConteudoTamanhoEResolucaoInvalidos() throws Exception {
        assertThrows(ResponseStatusException.class, () -> CapaCurso.validar("https://exemplo.com/capa.jpg"));
        assertThrows(ResponseStatusException.class, () -> CapaCurso.validar("data:image/jpeg;base64,invalid!"));
        assertThrows(ResponseStatusException.class, () -> CapaCurso.validar(imagem(640, 640, "png")));
        assertThrows(ResponseStatusException.class, () -> CapaCurso.validar(imagem(551, 250, "jpeg")));
        assertThrows(ResponseStatusException.class, () -> CapaCurso.validar(imagem(550, 251, "jpeg")));
        assertThrows(ResponseStatusException.class, () -> CapaCurso.validar(imagem(2049, 2049, "jpeg")));
        assertThrows(ResponseStatusException.class, () -> CapaCurso.validar("data:image/jpeg;base64," + "A".repeat(2796208)));
    }
}
