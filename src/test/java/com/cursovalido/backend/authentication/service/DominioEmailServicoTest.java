package com.cursovalido.backend.authentication.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import javax.naming.NameNotFoundException;
import javax.naming.CommunicationException;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.InitialDirContext;
import org.junit.jupiter.api.Test;

class DominioEmailServicoTest {
    private final InitialDirContext contexto = mock(InitialDirContext.class);

    private DominioEmailServico servico() throws Exception {
        var servico = spy(new DominioEmailServico());
        doReturn(contexto).when(servico).criarContexto();
        return servico;
    }

    @Test
    void aceitaMxEFechaContexto() throws Exception {
        when(contexto.getAttributes(eq("gmail.com"), any(String[].class)))
                .thenReturn(new BasicAttributes("MX", "10 smtp.gmail.com."));
        assertTrue(servico().aceitaEmail("gmail.com"));
        verify(contexto).getAttributes("gmail.com", new String[] { "MX" });
        verify(contexto).close();
    }

    @Test
    void rejeitaNullMxMesmoComEnderecoIp() throws Exception {
        var atributos = new BasicAttributes("MX", "0 .");
        atributos.put("A", "192.0.2.1");
        when(contexto.getAttributes(anyString(), any(String[].class))).thenReturn(atributos);
        assertFalse(servico().aceitaEmail("example.com"));
    }

    @Test
    void aceitaEnderecoSemMx() throws Exception {
        when(contexto.getAttributes("example.com", new String[] { "MX" })).thenReturn(new BasicAttributes());
        when(contexto.getAttributes("example.com", new String[] { "A" })).thenReturn(new BasicAttributes());
        when(contexto.getAttributes("example.com", new String[] { "AAAA" }))
                .thenReturn(new BasicAttributes("AAAA", "2001:db8::1"));
        assertTrue(servico().aceitaEmail("example.com"));
    }

    @Test
    void consultaMxEspecificoMesmoQuandoAnyNaoRetornaRegistros() throws Exception {
        when(contexto.getAttributes(anyString(), any(String[].class))).thenReturn(new BasicAttributes());
        when(contexto.getAttributes("gmail.com", new String[] { "MX" }))
                .thenReturn(new BasicAttributes("MX", "5 gmail-smtp-in.l.google.com."));
        assertTrue(servico().aceitaEmail("gmail.com"));
        verify(contexto, never()).getAttributes("gmail.com", new String[] { "MX", "A", "AAAA" });
    }

    @Test
    void aceitaIpv4NaAusenciaDeMxSemConsultarIpv6() throws Exception {
        when(contexto.getAttributes("example.com", new String[] { "MX" })).thenReturn(new BasicAttributes());
        when(contexto.getAttributes("example.com", new String[] { "A" }))
                .thenReturn(new BasicAttributes("A", "192.0.2.1"));
        assertTrue(servico().aceitaEmail("example.com"));
        verify(contexto, never()).getAttributes("example.com", new String[] { "AAAA" });
    }

    @Test
    void rejeitaDominioSemRegistros() throws Exception {
        when(contexto.getAttributes(anyString(), any(String[].class))).thenReturn(new BasicAttributes());
        assertFalse(servico().aceitaEmail("example.com"));
    }

    @Test
    void rejeitaDominioInexistenteEFechaContexto() throws Exception {
        when(contexto.getAttributes(anyString(), any(String[].class))).thenThrow(new NameNotFoundException());
        assertFalse(servico().aceitaEmail("gmail.commm"));
        verify(contexto).close();
    }

    @Test
    void propagaIndisponibilidadeEFechaContexto() throws Exception {
        when(contexto.getAttributes(anyString(), any(String[].class))).thenThrow(new CommunicationException());
        var servico = servico();
        assertThrows(CommunicationException.class, () -> servico.aceitaEmail("gmail.com"));
        verify(contexto).close();
    }
}
