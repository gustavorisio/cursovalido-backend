package com.cursovalido.backend.aulas.repository;

import com.cursovalido.backend.aulas.dto.RankingAlunoResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RankingAlunoRepository {
    private final JdbcTemplate jdbc;

    public List<RankingAlunoResponse> listar() {
        return jdbc.query("""
                SELECT ROW_NUMBER() OVER (ORDER BY nivel DESC, xp_total DESC, aulas_concluidas DESC,
                    comentarios DESC, nome ASC) AS posicao,
                    nome, nivel, comentarios, aulas_concluidas
                FROM (
                    SELECT u.nome_completo AS nome,
                        COALESCE(g.nivel, 1) AS nivel,
                        COALESCE(g.xp_total, 0) AS xp_total,
                        (SELECT COUNT(*) FROM tb_comments c
                            WHERE c.author_id = u.id AND c.active = true) AS comentarios,
                        (SELECT COUNT(*) FROM tb_progresso_aulas p
                            JOIN tb_matriculas m ON m.id = p.matricula_id
                            WHERE m.aluno_id = u.id AND p.concluida = true) AS aulas_concluidas
                    FROM tb_usuarios u
                    LEFT JOIN tb_gamificacao_alunos g ON g.aluno_id = u.id
                    WHERE u.perfil = 'ALUNO' AND u.ativo = true AND u.excluido_em IS NULL
                ) AS pontuacoes
                ORDER BY posicao
                LIMIT 20
                """, (rs, row) -> new RankingAlunoResponse(rs.getInt("posicao"), rs.getString("nome"),
                rs.getInt("nivel"), rs.getLong("comentarios"), rs.getLong("aulas_concluidas")));
    }
}
