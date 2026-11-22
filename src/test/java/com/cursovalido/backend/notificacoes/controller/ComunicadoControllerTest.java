package com.cursovalido.backend.notificacoes.controller;

import com.cursovalido.backend.notificacoes.service.ComunicadoService;
import com.cursovalido.backend.notificacoes.dto.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.cursovalido.backend.authentication.support.UsuarioAutenticadoFixture.usuario;

class ComunicadoControllerTest {
    private final ComunicadoService servico = mock(ComunicadoService.class);
    private final ComunicadoController controlador = new ComunicadoController(servico);

    @Test
    void publicacaoEEdicaoUsamAdministradorAutenticado() {
        var dados = new ComunicadoRequest("Aviso", "Texto");
        var retorno = new ComunicadoResponse(5L, "Aviso", "Texto", null, false);
        when(servico.publicar(7L, dados)).thenReturn(retorno);
        when(servico.editar(7L, 5L, dados)).thenReturn(retorno);
        assertThat(controlador.publicar(usuario(), dados)).isSameAs(retorno);
        assertThat(controlador.editar(usuario(), 5L, dados)).isSameAs(retorno);
        verify(servico).publicar(7L, dados);
        verify(servico).editar(7L, 5L, dados);
    }

    @Test
    void exclusaoEContagemNaoAceitamIdentidadeDoCorpoDaRequisicao() {
        controlador.excluir(usuario(), 5L);
        verify(servico).excluir(7L, 5L);
        when(servico.contarNaoLidas(7L)).thenReturn(3L);
        assertThat(controlador.contarNaoLidas(usuario())).containsEntry("quantidade", 3L);
    }
}
