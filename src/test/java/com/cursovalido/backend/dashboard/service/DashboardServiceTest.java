package com.cursovalido.backend.dashboard.service;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.dashboard.dto.DashboardResponse.AtividadeDia;
import com.cursovalido.backend.dashboard.dto.DashboardResponse.CursoPopular;
import com.cursovalido.backend.dashboard.dto.DashboardResponse.MatriculasMes;
import com.cursovalido.backend.dashboard.dto.DashboardResponse.Resumo;
import com.cursovalido.backend.dashboard.dto.DashboardResponse.UsuariosPerfil;
import com.cursovalido.backend.dashboard.repository.DashboardRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardServiceTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final DashboardRepository dashboard = mock(DashboardRepository.class);
    private final DashboardService service = new DashboardService(usuarios, dashboard);

    @Test
    void deveRetornarResumoParaAdministrador() {
        cadastrarUsuario(Perfil.ADMINISTRADOR);
        var resumo = new Resumo(3, 2, 1, 7, 3, 1, 0, 5, 1, 2, 33.3, 1.5);
        when(dashboard.resumo()).thenReturn(resumo);

        assertEquals(resumo, service.resumo(1L));
    }

    @Test
    void deveConsultarAtividadeNoIntervaloDoDiaEmSaoPaulo() {
        cadastrarUsuario(Perfil.ADMINISTRADOR);
        LocalDate dia = LocalDate.of(2025, 1, 10);
        ZoneId saoPaulo = ZoneId.of("America/Sao_Paulo");
        var inicio = dia.atStartOfDay(saoPaulo).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        when(dashboard.atividadeDia(dia, inicio, inicio.plusDays(1)))
                .thenReturn(new AtividadeDia(dia, 2, 0, 0, 0, 0, 0));

        assertEquals(2, service.atividadeDia(1L, dia).matriculas());
    }

    @Test
    void devePreencherMesSemMatriculasComZero() {
        cadastrarUsuario(Perfil.ADMINISTRADOR);
        LocalDate mesAtual = LocalDate.now(ZoneId.of("America/Sao_Paulo")).withDayOfMonth(1);
        String mesAnterior = YearMonth.from(mesAtual.minusMonths(1)).toString();
        String mes = YearMonth.from(mesAtual).toString();
        when(dashboard.matriculasPorMes(mesAtual.minusMonths(1), mesAtual.plusMonths(1)))
                .thenReturn(List.of(new MatriculasMes(mes, 4)));

        assertEquals(List.of(new MatriculasMes(mesAnterior, 0), new MatriculasMes(mes, 4)),
                service.matriculasPorMes(1L, 2));
    }

    @Test
    void deveRetornarRankingDeCursos() {
        cadastrarUsuario(Perfil.ADMINISTRADOR);
        var ranking = List.of(new CursoPopular(2L, "Java", 10, 30, 2, 20.0));
        when(dashboard.cursosMaisProcurados()).thenReturn(ranking);

        assertEquals(ranking, service.cursosMaisProcurados(1L));
    }

    @Test
    void deveRetornarQuantidadeDeUsuariosPorPerfil() {
        cadastrarUsuario(Perfil.ADMINISTRADOR);
        var totais = List.of(new UsuariosPerfil("ALUNO", 3));
        when(dashboard.usuariosPorPerfil()).thenReturn(totais);

        assertEquals(totais, service.usuariosPorPerfil(1L));
    }

    @Test
    void deveImpedirConsultaPorAluno() {
        cadastrarUsuario(Perfil.ALUNO);

        var erro = assertThrows(ResponseStatusException.class, () -> service.resumo(1L));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
        verifyNoInteractions(dashboard);
    }

    @Test
    void validaPeriodoDeMesesEDataFutura() {
        cadastrarUsuario(Perfil.ADMINISTRADOR);

        assertThrows(ResponseStatusException.class, () -> service.matriculasPorMes(1L, 0));
        assertThrows(ResponseStatusException.class, () -> service.matriculasPorMes(1L, 25));
        assertThrows(ResponseStatusException.class,
                () -> service.atividadeDia(1L, LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(2)));
    }

    @Test
    void recusaAdministradorInativoAusenteOuSemIdentificacao() {
        Usuario inativo = new Usuario();
        inativo.setPerfil(Perfil.ADMINISTRADOR);
        inativo.setAtivo(false);
        when(usuarios.findById(1L)).thenReturn(Optional.of(inativo));
        when(usuarios.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.resumo(1L));
        assertThrows(ResponseStatusException.class, () -> service.resumo(2L));
        assertThrows(ResponseStatusException.class, () -> service.resumo(null));
        verifyNoInteractions(dashboard);
    }

    private void cadastrarUsuario(Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));
    }
}
