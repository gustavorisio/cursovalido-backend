package com.cursovalido.backend.aulas.repository;

import com.cursovalido.backend.aulas.dto.RankingAlunoResponse;
import java.sql.ResultSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RankingAlunoJdbcTest {
    @Test
    void mapeiaPosicaoNivelEIndicadoresSemRecalcularRankingNoJava() throws Exception {
        var jdbc = mock(JdbcTemplate.class);
        var rs = mock(ResultSet.class);
        when(rs.getInt("posicao")).thenReturn(2);
        when(rs.getString("nome")).thenReturn("Ana Silva");
        when(rs.getInt("nivel")).thenReturn(3);
        when(rs.getLong("comentarios")).thenReturn(8L);
        when(rs.getLong("aulas_concluidas")).thenReturn(12L);
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<RankingAlunoResponse>>any()))
                .thenAnswer(invocacao -> List.of(invocacao.<RowMapper<RankingAlunoResponse>>getArgument(1).mapRow(rs, 0)));
        assertThat(new RankingAlunoRepository(jdbc).listar()).containsExactly(new RankingAlunoResponse(2, "Ana Silva", 3, 8, 12));
    }
}
