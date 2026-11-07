package com.cursovalido.backend.authentication.controller;

import com.cursovalido.backend.authentication.dto.*;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import com.cursovalido.backend.authentication.service.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ModeracaoController {
    private final PrivilegiosAdministrativosService privilegios;
    private final ModeracaoUsuarioService moderacao;
    private final UsuarioRepository usuarios;

    @GetMapping("/api/administracao/permissoes")
    public Map<String, Object> permissoes(@AuthenticationPrincipal UsuarioAutenticado principal) {
        var administrador = privilegios.verificarAdministrador(principal.getId());
        return Map.of("principal", privilegios.ehPrincipal(administrador), "idUsuario", administrador.getId());
    }

    @GetMapping("/api/administracao/usuarios")
    public List<UsuarioAdministracaoResposta> listar(@AuthenticationPrincipal UsuarioAutenticado principal) {
        var administrador = privilegios.verificarAdministrador(principal.getId());
        return usuarios.findAll().stream().map(usuario -> {
            boolean podeModerar = !usuario.getId().equals(administrador.getId());
            if (privilegios.ehPrincipal(usuario)) podeModerar = false;
            if (usuario.getPerfil() == Perfil.ADMINISTRADOR) {
                if (!privilegios.ehPrincipal(administrador)) podeModerar = false;
            }
            return new UsuarioAdministracaoResposta(usuario.getId(), usuario.getNomeCompleto(), usuario.getEmail(),
                    usuario.getPerfil(), usuario.isAtivo(), podeModerar,
                    moderacao.vigentes(usuario.getId()).stream().map(SuspensaoResposta::de).toList());
        }).toList();
    }

    @PostMapping("/api/administracao/usuarios/{id}/suspensoes")
    @ResponseStatus(HttpStatus.CREATED)
    public SuspensaoResposta aplicar(@PathVariable Long id, @Valid @RequestBody SuspensaoRequisicao dados,
            @AuthenticationPrincipal UsuarioAutenticado principal) {
        return moderacao.aplicar(id, dados, principal.getId());
    }

    @DeleteMapping("/api/administracao/usuarios/{id}/suspensoes/{idSuspensao}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revogar(@PathVariable Long id, @PathVariable Long idSuspensao,
            @AuthenticationPrincipal UsuarioAutenticado principal) {
        moderacao.revogar(id, idSuspensao, principal.getId());
    }

    @GetMapping("/api/autenticacao/restricoes")
    public List<SuspensaoResposta> minhasRestricoes(@AuthenticationPrincipal UsuarioAutenticado principal) {
        return moderacao.vigentes(principal.getId()).stream().map(SuspensaoResposta::de).toList();
    }

    public record UsuarioAdministracaoResposta(Long id, String nome, String email, Perfil perfil,
            boolean ativo, boolean podeModerar, List<SuspensaoResposta> suspensoes) {
    }
}
