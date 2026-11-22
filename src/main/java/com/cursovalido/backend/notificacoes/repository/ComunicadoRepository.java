package com.cursovalido.backend.notificacoes.repository;

import com.cursovalido.backend.notificacoes.dto.ComunicadoResponse;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ComunicadoRepository {
    private final JdbcTemplate jdbc;

    // Serializa publicações de todos os administradores para proteger o máximo global.
    public void bloquearPublicacoes() {
        jdbc.execute("LOCK TABLE tb_comunicados IN SHARE ROW EXCLUSIVE MODE");
    }

    public boolean publicouRecentemente(Long autor, LocalDateTime desde) {
        return jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM tb_publicacoes_comunicados
                WHERE autor_id = ? AND ultima_publicacao_em > ?)
                """, Boolean.class, autor, Timestamp.valueOf(desde));
    }

    public void registrarPublicacao(Long autor, LocalDateTime data) {
        jdbc.update("""
                INSERT INTO tb_publicacoes_comunicados (autor_id, ultima_publicacao_em) VALUES (?, ?)
                ON CONFLICT (autor_id) DO UPDATE SET ultima_publicacao_em = EXCLUDED.ultima_publicacao_em
                """, autor, Timestamp.valueOf(data));
    }

    public ComunicadoResponse criar(String titulo, String conteudo, Long autor) {
        var chave = new GeneratedKeyHolder();
        var data = LocalDateTime.now();
        jdbc.update(conexao -> {
            var comando = conexao.prepareStatement(
                    "INSERT INTO tb_comunicados (titulo, conteudo, autor_id, criado_em) VALUES (?, ?, ?, ?)",
                    new String[] { "id" });
            comando.setString(1, titulo);
            comando.setString(2, conteudo);
            comando.setLong(3, autor);
            comando.setTimestamp(4, Timestamp.valueOf(data));
            return comando;
        }, chave);
        return new ComunicadoResponse(chave.getKey().longValue(), titulo, conteudo, data, false);
    }

    public List<ComunicadoResponse> listar(Long usuario, int pagina, int tamanho) {
        return jdbc.query("""
                SELECT c.id, c.titulo, c.conteudo, c.criado_em, (l.usuario_id IS NOT NULL) AS lida
                FROM tb_comunicados c
                LEFT JOIN tb_comunicados_leituras l ON l.comunicado_id = c.id AND l.usuario_id = ?
                ORDER BY c.criado_em DESC, c.id DESC LIMIT ? OFFSET ?
                """, (rs, linha) -> new ComunicadoResponse(rs.getLong("id"), rs.getString("titulo"),
                rs.getString("conteudo"), rs.getTimestamp("criado_em").toLocalDateTime(),
                rs.getBoolean("lida")), usuario, tamanho, (long) pagina * tamanho);
    }

    public java.util.Optional<ComunicadoResponse> editar(Long id, String titulo, String conteudo) {
        return jdbc.query("""
                UPDATE tb_comunicados SET titulo = ?, conteudo = ? WHERE id = ?
                RETURNING id, titulo, conteudo, criado_em
                """, (rs, linha) -> new ComunicadoResponse(rs.getLong("id"), rs.getString("titulo"),
                rs.getString("conteudo"), rs.getTimestamp("criado_em").toLocalDateTime(), false),
                titulo, conteudo, id).stream().findFirst();
    }

    public boolean excluir(Long id) {
        return jdbc.update("DELETE FROM tb_comunicados WHERE id = ?", id) > 0;
    }

    public long contar() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tb_comunicados", Long.class);
    }

    public long contarNaoLidas(Long usuario) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM tb_comunicados c WHERE NOT EXISTS (
                    SELECT 1 FROM tb_comunicados_leituras l WHERE l.comunicado_id = c.id AND l.usuario_id = ?)
                """, Long.class, usuario);
    }

    public boolean existe(Long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tb_comunicados WHERE id = ?", Long.class, id) > 0;
    }

    public void marcarLida(Long comunicado, Long usuario) {
        jdbc.update("""
                INSERT INTO tb_comunicados_leituras (comunicado_id, usuario_id) VALUES (?, ?)
                ON CONFLICT (usuario_id, comunicado_id) DO NOTHING
                """, comunicado, usuario);
    }
}
