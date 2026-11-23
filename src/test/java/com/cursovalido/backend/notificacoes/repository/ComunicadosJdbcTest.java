package com.cursovalido.backend.notificacoes.repository;

import com.cursovalido.backend.notificacoes.dto.ComunicadoResponse;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ComunicadosJdbcTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ComunicadoRepository repositorio = new ComunicadoRepository(jdbc);
    private final LocalDateTime data = LocalDateTime.of(2026, 11, 22, 10, 0);

    private ResultSet linha() throws Exception {
        var rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(5L);
        when(rs.getString("titulo")).thenReturn("Aviso");
        when(rs.getString("conteudo")).thenReturn("Texto");
        when(rs.getTimestamp("criado_em")).thenReturn(Timestamp.valueOf(data));
        return rs;
    }

    @Test
    void consultaPaginadaMantemUsuarioLimiteOffsetEEstadoDeLeitura() throws Exception {
        var rs = linha(); when(rs.getBoolean("lida")).thenReturn(true);
        when(jdbc.query(anyString(), ArgumentMatchers.<RowMapper<ComunicadoResponse>>any(), eq(7L), eq(20), eq(40L)))
                .thenAnswer(invocacao -> List.of(invocacao.<RowMapper<ComunicadoResponse>>getArgument(1).mapRow(rs, 0)));
        var itens = repositorio.listar(7L, 2, 20);
        assertThat(itens).containsExactly(new ComunicadoResponse(5L, "Aviso", "Texto", data, true));
        var sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(), ArgumentMatchers.<RowMapper<ComunicadoResponse>>any(), eq(7L), eq(20), eq(40L));
        assertThat(sql.getValue()).contains("l.usuario_id = ?", "LIMIT ? OFFSET ?");
    }

    @Test
    void editarDevolveRegistroOuAusenciaSemInventarUmaPublicacao() throws Exception {
        var rs = linha();
        when(jdbc.query(anyString(), ArgumentMatchers.<RowMapper<ComunicadoResponse>>any(), eq("Aviso"), eq("Texto"), eq(5L)))
                .thenAnswer(invocacao -> List.of(invocacao.<RowMapper<ComunicadoResponse>>getArgument(1).mapRow(rs, 0)));
        assertThat(repositorio.editar(5L, "Aviso", "Texto")).contains(new ComunicadoResponse(5L, "Aviso", "Texto", data, false));
        assertThat(repositorio.editar(99L, "Aviso", "Texto")).isEmpty();
    }

    @Test
    void exclusaoEConsultasDeContagemPreservamResultadoDoBanco() {
        when(jdbc.update(anyString(), eq(5L))).thenReturn(1, 0);
        assertThat(repositorio.excluir(5L)).isTrue();
        assertThat(repositorio.excluir(5L)).isFalse();
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(30L);
        assertThat(repositorio.contar()).isEqualTo(30);
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq(7L))).thenReturn(3L);
        assertThat(repositorio.contarNaoLidas(7L)).isEqualTo(3);
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq(5L))).thenReturn(1L, 0L);
        assertThat(repositorio.existe(5L)).isTrue();
        assertThat(repositorio.existe(5L)).isFalse();
    }

    @Test
    void controleDePublicacaoUsaBloqueioETimestampDoLimite() {
        repositorio.bloquearPublicacoes();
        verify(jdbc).execute("LOCK TABLE tb_comunicados IN SHARE ROW EXCLUSIVE MODE");
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(7L), eq(Timestamp.valueOf(data)))).thenReturn(true);
        assertThat(repositorio.publicouRecentemente(7L, data)).isTrue();
    }
}
