package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.ContaExcluida;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.ContaExcluidaRepository;
import java.util.List;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IdentidadeExcluidaServiceTest {

    private final ContaExcluidaRepository repositorio = mock(ContaExcluidaRepository.class);
    private final IdentidadeExcluidaService service = new IdentidadeExcluidaService(repositorio);

    @Test
    void deveConsultarCadastroExcluidoMesmoComMudancaDeMaiusculasEEspacos() {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);
        usuario.setEmail("Aluno@teste.com");

        service.registrar(usuario);

        var captura = ArgumentCaptor.forClass(ContaExcluida.class);
        verify(repositorio).saveAndFlush(captura.capture());
        var registro = captura.getValue();
        assertNotEquals(usuario.getEmail(), registro.getEmailHash());
        when(repositorio.findByEmailHash(registro.getEmailHash())).thenReturn(List.of(registro));

        assertEquals(List.of(1L), service.anteriores(" aluno@teste.com "));
    }
}
