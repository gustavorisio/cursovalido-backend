package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.PoliticaRequest;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
import java.util.List;
import com.cursovalido.backend.authentication.entity.Politica;
import com.cursovalido.backend.authentication.repository.PoliticaRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PoliticaServiceTest {

    private final PoliticaRepository repositorio = mock(PoliticaRepository.class);
    private final VersaoDocumentoService versoes = mock(VersaoDocumentoService.class);
    private final PrivilegiosAdministrativosService privilegios = mock(PrivilegiosAdministrativosService.class);
    private final PoliticaService service = new PoliticaService(
            repositorio, mock(LogAuditoriaService.class), privilegios, versoes);

    @Test
    void deveListarDadosDaPoliticaAtiva() {
        when(repositorio.findByAtivaTrueOrderByDataPublicacaoDesc())
                .thenReturn(List.of(new Politica("Termos", "v1.0", "Texto")));

        var politica = service.listarAtivas().getFirst();

        assertEquals("Termos", politica.nome());
        assertEquals("v1.0", politica.versao());
    }

    @Test
    void deveConsultarSeVersaoEstaAtiva() {
        when(repositorio.existsByAtivaTrueAndVersao("v1.0")).thenReturn(true);

        assertTrue(service.versaoAtiva("v1.0"));
        assertFalse(service.versaoAtiva("v0.8"));
    }

    @Test
    void deveDesativarPoliticaAnteriorAoPublicar() {
        var anterior = new Politica("Termos", "v1.0", "Texto anterior");
        when(repositorio.findByAtivaTrueOrderByDataPublicacaoDesc()).thenReturn(List.of(anterior));
        when(repositorio.save(any(Politica.class))).thenAnswer(chamada -> chamada.getArgument(0));
        when(versoes.proxima("TERMOS")).thenReturn("v1.2");

        var publicada = service.publicar(new PoliticaRequest(" Termos ", "Novo texto"), 1L);

        assertFalse(anterior.isAtiva());
        assertEquals("v1.2", publicada.versao());
        assertEquals("Termos", publicada.nome());
        verify(privilegios).verificarPrincipal(1L);
    }
}

