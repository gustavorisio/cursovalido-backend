package com.cursovalido.backend.dashboard.controller;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import com.cursovalido.backend.dashboard.service.DashboardService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class DashboardControllerTest {
    @Test
    void todasAsConsultasUsamOAdministradorAutenticado() {
        var servico = mock(DashboardService.class);
        var controlador = new DashboardController(servico);
        var admin = new UsuarioAutenticado(7L, "a@x.com", "s", Perfil.ADMINISTRADOR, true, true, true);
        LocalDate dia = LocalDate.of(2026, 1, 10);

        controlador.resumo(admin);
        controlador.atividadeDia(admin, dia);
        controlador.matriculasPorMes(admin, 6);
        controlador.cursosMaisProcurados(admin);
        controlador.usuariosPorPerfil(admin);

        verify(servico).resumo(7L);
        verify(servico).atividadeDia(7L, dia);
        verify(servico).matriculasPorMes(7L, 6);
        verify(servico).cursosMaisProcurados(7L);
        verify(servico).usuariosPorPerfil(7L);
    }
}
