package com.cursovalido.backend.authentication.controller;

import com.cursovalido.backend.authentication.dto.ConviteAcessoRequest;
import com.cursovalido.backend.authentication.dto.ConviteAcessoResponse;
import com.cursovalido.backend.authentication.dto.ConviteResumoResponse;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import com.cursovalido.backend.authentication.service.ConviteAcessoService;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/administracao/convites")
@RequiredArgsConstructor
public class AdministracaoController {
    private final ConviteAcessoService convites;
    private final LogAuditoriaService auditoria;

    @GetMapping("/auditoria/integridade")
    public Map<String, Boolean> verificarIntegridadeAuditoria() {
        return Map.of("integra", auditoria.verificarIntegridade());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void criar(@AuthenticationPrincipal UsuarioAutenticado administrador,
            @Valid @RequestBody ConviteAcessoRequest requisicao) {
        convites.criar(requisicao.email(), requisicao.perfil(), administrador.getId());
    }

    @GetMapping("/pendentes")
    public List<ConviteAcessoResponse> listarPendentes() {
        return convites.pendentes().stream().map(this::converter).toList();
    }

    @GetMapping
    public List<ConviteResumoResponse> listarConvites() {
        return convites.convitesPendentes();
    }

    @PostMapping("/{id}/aprovar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void aprovar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado administrador) {
        convites.aprovar(id, administrador.getId());
    }

    @PostMapping("/{id}/rejeitar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejeitar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado administrador) {
        convites.rejeitar(id, administrador.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado administrador) {
        convites.remover(id, administrador.getId());
    }

    @PostMapping("/{id}/reenviar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reenviar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado administrador) {
        convites.reenviar(id, administrador.getId());
    }

    @DeleteMapping("/{id}/cancelar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado administrador) {
        convites.cancelarConvite(id, administrador.getId());
    }

    private ConviteAcessoResponse converter(Usuario usuario) {
        return new ConviteAcessoResponse(usuario.getId(), usuario.getEmail(), usuario.getPerfil(),
                usuario.getStatusAprovacao(), usuario.getNomeCompleto(), usuario.getCpf(),
                usuario.getDataNascimento());
    }
}
