package com.cursovalido.backend.authentication.controller;

import com.cursovalido.backend.authentication.dto.ConviteAcessoRequisicao;
import com.cursovalido.backend.authentication.dto.ConviteAcessoResposta;
import com.cursovalido.backend.authentication.dto.ConviteResumoResposta;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.service.ConviteAcessoServico;
import com.cursovalido.backend.authentication.service.LogAuditoriaServico;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/administracao/convites")
public class AdministracaoControlador {
    @Autowired
    private ConviteAcessoServico convites;
    @Autowired
    private LogAuditoriaServico auditoria;

    @GetMapping("/auditoria/integridade")
    public Map<String, Boolean> verificarIntegridadeAuditoria() {
        return Map.of("integra", auditoria.verificarIntegridade());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void criar(@AuthenticationPrincipal UsuarioAutenticado administrador,
            @Valid @RequestBody ConviteAcessoRequisicao requisicao) {
        convites.criar(requisicao.email(), requisicao.perfil(), administrador.getId());
    }

    @GetMapping("/pendentes")
    public List<ConviteAcessoResposta> listarPendentes() {
        return convites.pendentes().stream().map(this::converter).toList();
    }

    @GetMapping
    public List<ConviteResumoResposta> listarConvites() {
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

    private ConviteAcessoResposta converter(Usuario usuario) {
        return new ConviteAcessoResposta(usuario.getId(), usuario.getEmail(), usuario.getPerfil(),
                usuario.getStatusAprovacao(), usuario.getNomeCompleto(), usuario.getCpf(),
                usuario.getDataNascimento(), usuario.getTelefone());
    }
}
