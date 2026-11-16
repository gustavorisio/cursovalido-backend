package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.PoliticaRequest;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
import java.util.List;
import com.cursovalido.backend.authentication.entity.PoliticaPrivacidade;
import com.cursovalido.backend.authentication.repository.PrivacidadeRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrivacidadeServiceTest {

    private final PrivacidadeRepository repositorio = mock(PrivacidadeRepository.class);
    private final VersaoDocumentoService versoes = mock(VersaoDocumentoService.class);
    private final PrivacidadeService service = new PrivacidadeService(repositorio,
            mock(PrivilegiosAdministrativosService.class), mock(LogAuditoriaService.class), versoes);

    @Test
    void devePedirNovoAceiteQuandoPoliticaForMaisRecente() {
        var politica = new PoliticaPrivacidade("Privacidade", "2.0", "Texto");
        when(repositorio.findByAtivaTrueOrderByDataPublicacaoDesc()).thenReturn(List.of(politica));

        assertTrue(service.atualizadaDesde(null));
        assertTrue(service.atualizadaDesde(politica.getDataPublicacao().minusDays(1)));
        assertFalse(service.atualizadaDesde(politica.getDataPublicacao()));
    }

    @Test
    void deveManterAceiteQuandoNaoExistePoliticaAtiva() {
        assertFalse(service.atualizadaDesde(null));
    }

    @Test
    void deveDesativarPoliticaAnteriorAoPublicar() {
        var anterior = new PoliticaPrivacidade("Privacidade", "1.0", "Texto anterior");
        when(repositorio.findByAtivaTrueOrderByDataPublicacaoDesc()).thenReturn(List.of(anterior));
        when(repositorio.save(any(PoliticaPrivacidade.class))).thenAnswer(chamada -> chamada.getArgument(0));
        when(versoes.proxima("PRIVACIDADE")).thenReturn("2.0");

        var publicada = service.publicar(new PoliticaRequest(" Privacidade ", "Novo texto"), 1L);

        assertFalse(anterior.isAtiva());
        assertEquals("2.0", publicada.versao());
        assertEquals("Privacidade", publicada.nome());
    }
}

