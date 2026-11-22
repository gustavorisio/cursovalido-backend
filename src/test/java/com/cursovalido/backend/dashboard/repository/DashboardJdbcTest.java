package com.cursovalido.backend.dashboard.repository;

import com.cursovalido.backend.dashboard.dto.DashboardResponse.*;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DashboardJdbcTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final DashboardRepository repositorio = new DashboardRepository(jdbc);

    @Test
    void resumoCalculaTaxaConclusaoEArredondaNivelSemDividirPorZero() throws Exception {
        var rs = mock(ResultSet.class);
        when(rs.getLong(anyString())).thenAnswer(invocacao -> Map.of("matriculas", 4L, "cursos_concluidos", 3L,
                "alunos", 8L, "alunos_engajados", 2L).getOrDefault(invocacao.getArgument(0), 0L));
        when(rs.getObject("nivel_medio")).thenReturn(2.25);
        when(jdbc.queryForObject(anyString(), org.mockito.ArgumentMatchers.<RowMapper<Resumo>>any()))
                .thenAnswer(invocacao -> invocacao.<RowMapper<Resumo>>getArgument(1).mapRow(rs, 0));
        var resumo = repositorio.resumo();
        assertThat(resumo.alunosAtivos()).isEqualTo(8);
        assertThat(resumo.alunosEngajados()).isEqualTo(2);
        assertThat(resumo.taxaConclusaoMatriculas()).isEqualTo(75.0);
        assertThat(resumo.nivelMedioAlunos()).isEqualTo(2.3);
        when(rs.getLong("matriculas")).thenReturn(0L);
        when(rs.getObject("nivel_medio")).thenReturn(null);
        var vazio = repositorio.resumo();
        assertThat(vazio.taxaConclusaoMatriculas()).isZero();
        assertThat(vazio.nivelMedioAlunos()).isNull();
    }

    @Test
    void atividadeDoDiaUsaIntervaloExclusivoParaTodosOsIndicadores() throws Exception {
        var rs = mock(ResultSet.class);
        when(rs.getLong("matriculas")).thenReturn(3L);
        when(rs.getLong("aulas_concluidas")).thenReturn(4L);
        var dia = LocalDate.of(2026, 11, 22);
        when(jdbc.queryForObject(anyString(), org.mockito.ArgumentMatchers.<RowMapper<AtividadeDia>>any(), any(Object[].class)))
                .thenAnswer(invocacao -> invocacao.<RowMapper<AtividadeDia>>getArgument(1).mapRow(rs, 0));
        var resultado = repositorio.atividadeDia(dia, dia.atStartOfDay(), dia.plusDays(1).atStartOfDay());
        assertThat(resultado.data()).isEqualTo(dia);
        assertThat(resultado.matriculas()).isEqualTo(3);
        assertThat(resultado.aulasConcluidas()).isEqualTo(4);
        var inicio = Timestamp.valueOf(dia.atStartOfDay());
        var fim = Timestamp.valueOf(dia.plusDays(1).atStartOfDay());
        verify(jdbc).queryForObject(anyString(), org.mockito.ArgumentMatchers.<RowMapper<AtividadeDia>>any(),
                eq(inicio), eq(fim), eq(inicio), eq(fim), eq(inicio), eq(fim), eq(inicio), eq(fim), eq(inicio), eq(fim), eq(inicio), eq(fim));
    }
}
