package com.cursovalido.backend.authentication.controller;

import com.cursovalido.backend.authentication.dto.*;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.service.*;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.cursovalido.backend.authentication.support.UsuarioAutenticadoFixture.usuario;

class ResponsabilidadeControllerTest {
    @Test
    void consultaDocumentoCorretoParaCadaPerfil() {
        var servico = mock(ResponsabilidadeService.class);
        var documento = mock(TermoResponsabilidadeResponse.class);
        var controlador = new ResponsabilidadeController(servico);
        when(servico.documento(Perfil.PROFESSOR)).thenReturn(documento);
        assertThat(controlador.consultar(Perfil.PROFESSOR)).isSameAs(documento);
        verify(servico).documento(Perfil.PROFESSOR);
    }
}
