package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.LogAuditoria;
import com.cursovalido.backend.authentication.repository.LogAuditoriaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogAuditoriaServiceTest {

    private final LogAuditoriaRepository repositorio = mock(LogAuditoriaRepository.class);
    private final LogAuditoriaService service = new LogAuditoriaService(repositorio);

    @BeforeEach
    void configurarChave() {
        ReflectionTestUtils.setField(service, "chaveTexto", "chave-de-teste-sem-uso-em-producao");
        service.validarChave();
    }

    @Test
    void deveVincularNovoRegistroAoAnteriorEValidarEncadeamento() {
        LogAuditoria primeiro = registrarPrimeiroEvento();
        when(repositorio.findTopByOrderByIdDesc()).thenReturn(Optional.of(primeiro));

        service.registrar(1L, "SAIDA", null, null);

        var captura = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(repositorio, times(2)).save(captura.capture());
        var segundo = captura.getValue();
        assertEquals(primeiro.getAssinatura(), segundo.getHashAnterior());
        when(repositorio.findAllByOrderByIdAsc()).thenReturn(List.of(primeiro, segundo));
        assertTrue(service.verificarIntegridade());
    }

    @Test
    void deveRecusarRegistroSemAssinatura() {
        var registro = new LogAuditoria(1L, "LOGIN", null, null, null, null);
        when(repositorio.findAllByOrderByIdAsc()).thenReturn(List.of(registro));

        assertFalse(service.verificarIntegridade());
    }

    private LogAuditoria registrarPrimeiroEvento() {
        service.registrar(1L, "LOGIN", "Login efetuado", null);
        var captura = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(repositorio).save(captura.capture());
        return captura.getValue();
    }
}
