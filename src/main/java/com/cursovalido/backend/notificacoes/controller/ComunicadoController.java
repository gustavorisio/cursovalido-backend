package com.cursovalido.backend.notificacoes.controller;

import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import com.cursovalido.backend.notificacoes.dto.*;
import com.cursovalido.backend.notificacoes.service.ComunicadoService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ComunicadoController {
    private final ComunicadoService servico;

    @PostMapping("/administracao/comunicados")
    @ResponseStatus(HttpStatus.CREATED)
    public ComunicadoResponse publicar(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ComunicadoRequest dados) {
        return servico.publicar(usuario.getId(), dados);
    }

    @PutMapping("/administracao/comunicados/{id}")
    public ComunicadoResponse editar(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id, @Valid @RequestBody ComunicadoRequest dados) {
        return servico.editar(usuario.getId(), id, dados);
    }

    @DeleteMapping("/administracao/comunicados/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        servico.excluir(usuario.getId(), id);
    }

    @GetMapping("/administracao/comunicados")
    public PaginaComunicadosResponse listarAdministracao(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return servico.listarAdministracao(usuario.getId(), page, size);
    }

    @GetMapping({ "/notificacoes", "/aluno/notificacoes" })
    public PaginaComunicadosResponse listarNotificacoes(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return servico.listarNotificacoes(usuario.getId(), page, size);
    }

    @GetMapping({ "/notificacoes/nao-lidas", "/aluno/notificacoes/nao-lidas" })
    public Map<String, Long> contarNaoLidas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return Map.of("quantidade", servico.contarNaoLidas(usuario.getId()));
    }

    @PutMapping({ "/notificacoes/{id}/lida", "/aluno/notificacoes/{id}/lida" })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarLida(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        servico.marcarLida(usuario.getId(), id);
    }
}
