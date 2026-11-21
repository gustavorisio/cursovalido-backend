package com.cursovalido.backend.forum.controller;

import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import com.cursovalido.backend.forum.dto.DenunciaDtos.DecisaoDenunciaRequest;
import com.cursovalido.backend.forum.dto.DenunciaDtos.DenunciaRegistradaResponse;
import com.cursovalido.backend.forum.dto.DenunciaDtos.DenunciaRequest;
import com.cursovalido.backend.forum.dto.DenunciaDtos.DenunciaResponse;
import com.cursovalido.backend.forum.entity.StatusDenuncia;
import com.cursovalido.backend.forum.entity.TipoConteudoForum;
import com.cursovalido.backend.forum.service.DenunciaForumService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class DenunciaForumController {
    private final DenunciaForumService denuncias;

    @PostMapping("/api/forum/topicos/{id}/denuncias")
    @ResponseStatus(HttpStatus.CREATED)
    public DenunciaRegistradaResponse denunciarTopico(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id, @Valid @RequestBody DenunciaRequest requisicao) {
        return denuncias.denunciar(TipoConteudoForum.TOPICO, id, requisicao, usuario.getId());
    }

    @PostMapping("/api/forum/comentarios/{id}/denuncias")
    @ResponseStatus(HttpStatus.CREATED)
    public DenunciaRegistradaResponse denunciarComentario(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id, @Valid @RequestBody DenunciaRequest requisicao) {
        return denuncias.denunciar(TipoConteudoForum.COMENTARIO, id, requisicao, usuario.getId());
    }

    @GetMapping("/api/administracao/forum/denuncias")
    public List<DenunciaResponse> listar(@AuthenticationPrincipal UsuarioAutenticado administrador,
            @RequestParam(required = false) StatusDenuncia status) {
        return denuncias.listar(administrador.getId(), status);
    }

    @PutMapping("/api/administracao/forum/denuncias/{id}")
    public DenunciaResponse decidir(@AuthenticationPrincipal UsuarioAutenticado administrador,
            @PathVariable Long id, @Valid @RequestBody DecisaoDenunciaRequest decisao) {
        return denuncias.decidir(id, decisao, administrador.getId());
    }
}
