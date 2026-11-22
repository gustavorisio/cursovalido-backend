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

class PoliticaControllerTest {
    private final PoliticaService termos = mock(PoliticaService.class);
    private final PrivacidadeService privacidade = mock(PrivacidadeService.class);
    private final PoliticaController controlador = new PoliticaController(termos, privacidade);

    @Test
    void ausenciaDePoliticaAtivaRetorna204EPoliticaExistenteRetorna200() {
        when(privacidade.buscarAtiva()).thenReturn(null);
        assertThat(controlador.privacidadeAtiva().getStatusCode().value()).isEqualTo(204);
        var documento = mock(PoliticaResponse.class);
        when(privacidade.buscarAtiva()).thenReturn(documento);
        var resposta = controlador.privacidadeAtiva();
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).isSameAs(documento);
    }

    @Test
    void historicoPreservaVersoesEOrdemDoServico() {
        var versao = mock(PoliticaResponse.class);
        when(termos.listarHistorico()).thenReturn(List.of(versao));
        assertThat(controlador.historicoTermos()).containsExactly(versao);
        verifyNoInteractions(privacidade);
    }
}
