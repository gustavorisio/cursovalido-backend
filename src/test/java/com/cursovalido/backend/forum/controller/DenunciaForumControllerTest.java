package com.cursovalido.backend.forum.controller;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import com.cursovalido.backend.forum.dto.DenunciaDtos.DecisaoDenunciaRequest;
import com.cursovalido.backend.forum.dto.DenunciaDtos.DenunciaRegistradaResponse;
import com.cursovalido.backend.forum.dto.DenunciaDtos.DenunciaRequest;
import com.cursovalido.backend.forum.entity.MotivoDenuncia;
import com.cursovalido.backend.forum.entity.StatusDenuncia;
import com.cursovalido.backend.forum.entity.TipoConteudoForum;
import com.cursovalido.backend.forum.service.DenunciaForumService;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DenunciaForumControllerTest {
    private final DenunciaForumService servico = mock(DenunciaForumService.class);
    private final DenunciaForumController controlador = new DenunciaForumController(servico);
    private final UsuarioAutenticado aluno = new UsuarioAutenticado(1L, "a@x.com", "s", Perfil.ALUNO, true, true, true);
    private final UsuarioAutenticado admin = new UsuarioAutenticado(9L, "b@x.com", "s", Perfil.ADMINISTRADOR, true,
            true, true);

    @Test
    void encaminhaDenunciasDeTopicoEComentarioComOTipoCerto() {
        var pedido = new DenunciaRequest(MotivoDenuncia.SPAM, null);
        var resposta = new DenunciaRegistradaResponse(1L, StatusDenuncia.PENDENTE, "ok");
        when(servico.denunciar(any(), any(), any(), any())).thenReturn(resposta);

        assertThat(controlador.denunciarTopico(aluno, 5L, pedido)).isSameAs(resposta);
        assertThat(controlador.denunciarComentario(aluno, 9L, pedido)).isSameAs(resposta);
        verify(servico).denunciar(TipoConteudoForum.TOPICO, 5L, pedido, 1L);
        verify(servico).denunciar(TipoConteudoForum.COMENTARIO, 9L, pedido, 1L);
    }

    @Test
    void encaminhaListagemEDecisaoComOAdministradorAutenticado() {
        when(servico.listar(9L, StatusDenuncia.PENDENTE)).thenReturn(List.of());
        var decisao = new DecisaoDenunciaRequest(true, false, "ok");

        assertThat(controlador.listar(admin, StatusDenuncia.PENDENTE)).isEmpty();
        controlador.decidir(admin, 3L, decisao);
        verify(servico).decidir(3L, decisao, 9L);
    }
}
