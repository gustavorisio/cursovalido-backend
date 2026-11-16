package com.cursovalido.backend.authentication.controller;

import com.cursovalido.backend.authentication.dto.SuspensaoRequest;
import com.cursovalido.backend.authentication.dto.SuspensaoResponse;
import com.cursovalido.backend.authentication.dto.UsuarioAdministracaoResposta;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import com.cursovalido.backend.authentication.service.ContaUsuarioService;
import com.cursovalido.backend.authentication.service.ModeracaoUsuarioService;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
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
    private final ContaUsuarioService contas;

    @GetMapping("/api/administracao/permissoes")
    public Map<String, Object> permissoes(@AuthenticationPrincipal UsuarioAutenticado principal) {
        var administrador = privilegios.verificarAdministrador(principal.getId());
        return Map.of("principal", privilegios.ehPrincipal(administrador), "idUsuario", administrador.getId());
    }

    @GetMapping("/api/administracao/usuarios")
    public List<UsuarioAdministracaoResposta> listar(@AuthenticationPrincipal UsuarioAutenticado principal,
            @RequestParam(required = false) Boolean restrito) {
        return moderacao.listarUsuarios(principal.getId(), restrito);
    }

    @DeleteMapping("/api/administracao/usuarios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal) {
        contas.excluirPelaAdministracao(id, principal.getId());
    }

    @PostMapping("/api/administracao/usuarios/{id}/suspensoes")
    @ResponseStatus(HttpStatus.CREATED)
    public SuspensaoResponse aplicar(@PathVariable Long id, @Valid @RequestBody SuspensaoRequest dados,
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
    public List<SuspensaoResponse> minhasRestricoes(@AuthenticationPrincipal UsuarioAutenticado principal) {
        return moderacao.vigentes(principal.getId()).stream().map(SuspensaoResponse::de).toList();
    }
}
