package com.cursovalido.backend.dashboard.service;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.dashboard.dto.DashboardResponse.*;
import com.cursovalido.backend.dashboard.repository.DashboardRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {
    private final UsuarioRepository usuarios;
    private final DashboardRepository dashboard;

    public Resumo resumo(Long id) {
        verificarAdministrador(id);
        return dashboard.resumo();
    }

    public List<MatriculasMes> matriculasPorMes(Long id, int meses) {
        verificarAdministrador(id);
        if (meses < 1 || meses > 24)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meses deve estar entre 1 e 24");
        LocalDate atual = LocalDate.now(ZoneId.of("America/Sao_Paulo")).withDayOfMonth(1);
        LocalDate inicio = atual.minusMonths(meses - 1L);
        var valores = dashboard.matriculasPorMes(inicio, atual.plusMonths(1)).stream()
                .collect(Collectors.toMap(MatriculasMes::mes, MatriculasMes::matriculas));
        List<MatriculasMes> resultado = new ArrayList<>();
        for (int i = 0; i < meses; i++) {
            String mes = inicio.plusMonths(i).toString().substring(0, 7);
            resultado.add(new MatriculasMes(mes, valores.getOrDefault(mes, 0L)));
        }
        return resultado;
    }

    public List<CursoPopular> cursosMaisProcurados(Long id) {
        verificarAdministrador(id);
        return dashboard.cursosMaisProcurados();
    }

    public List<UsuariosPerfil> usuariosPorPerfil(Long id) {
        verificarAdministrador(id);
        return dashboard.usuariosPorPerfil();
    }

    private void verificarAdministrador(Long id) {
        if (id == null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrador necessario");
        var usuario = usuarios.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrador nao encontrado"));
        if (!usuario.isAtivo() || usuario.getPerfil() != Perfil.ADMINISTRADOR)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso exclusivo de administrador ativo");
    }
}
