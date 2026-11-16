package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.VersaoDocumento;
import com.cursovalido.backend.authentication.repository.VersaoDocumentoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VersaoDocumentoServiceTest {

    private final VersaoDocumentoRepository repositorio = mock(VersaoDocumentoRepository.class);
    private final VersaoDocumentoService service = new VersaoDocumentoService(repositorio);

    @Test
    void deveIncrementarVersaoDosTermosEmDoisDecimos() {
        when(repositorio.bloquear("TERMOS")).thenReturn(Optional.of(new VersaoDocumento("TERMOS", 1)));

        assertEquals("v1.2", service.proxima("TERMOS"));
        assertEquals("v1.4", service.proxima("TERMOS"));
    }

    @Test
    void deveIncrementarVersaoDaPrivacidadeEmUmaUnidade() {
        when(repositorio.bloquear("PRIVACIDADE")).thenReturn(Optional.of(new VersaoDocumento("PRIVACIDADE", 1)));

        assertEquals("2.0", service.proxima("PRIVACIDADE"));
    }

    @Test
    void deveFalharQuandoSequenciaNaoFoiInicializada() {
        assertThrows(IllegalStateException.class, () -> service.proxima("TERMOS"));
    }
}

