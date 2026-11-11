package com.cursovalido.backend.dashboard.controller;

import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import com.cursovalido.backend.dashboard.dto.DashboardResponse.*;
import com.cursovalido.backend.dashboard.service.DashboardService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/administracao/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService servico;

    @GetMapping("/resumo")
    public Resumo resumo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.resumo(usuario.getId());
    }

    @GetMapping("/matriculas-por-mes")
    public List<MatriculasMes> matriculasPorMes(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(defaultValue = "12") int meses) {
        return servico.matriculasPorMes(usuario.getId(), meses);
    }

    @GetMapping("/cursos-mais-procurados")
    public List<CursoPopular> cursosMaisProcurados(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.cursosMaisProcurados(usuario.getId());
    }

    @GetMapping("/usuarios-por-perfil")
    public List<UsuariosPerfil> usuariosPorPerfil(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.usuariosPorPerfil(usuario.getId());
    }
}
