package com.cursovalido.backend.aulas.controller;

import com.cursovalido.backend.aulas.dto.GamificacaoResponse;
import com.cursovalido.backend.aulas.service.GamificacaoService;
import com.cursovalido.backend.aulas.service.RankingAlunoService;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GamificacaoControllerTest {
    @Test
    void consultaAGamificacaoDoAlunoAutenticado() {
        var servico = mock(GamificacaoService.class);
        var resposta = new GamificacaoResponse(2, 50, 150, 100, 33, 150, 1, 1, false);
        when(servico.consultar(1L)).thenReturn(resposta);

        var aluno = new UsuarioAutenticado(1L, "a@x.com", "s", Perfil.ALUNO, true, true, true);
        assertThat(new GamificacaoController(servico, mock(RankingAlunoService.class)).consultar(aluno))
                .isSameAs(resposta);
    }
}
