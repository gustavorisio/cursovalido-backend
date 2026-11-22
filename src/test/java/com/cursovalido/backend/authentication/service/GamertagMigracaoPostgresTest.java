package com.cursovalido.backend.authentication.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.cursovalido.backend.forum.configuration.FlywayConfig;
import com.cursovalido.backend.forum.support.PostgresTeste;
import java.sql.DriverManager;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(PostgresTeste.CicloDeVida.class)
class GamertagMigracaoPostgresTest {
    @Test
    void migraTagsAntigasESincronizaForumPreservandoTagsValidas() throws Exception {
        var propriedades = new HashMap<String, Object>();
        PostgresTeste.configurar((nome, fornecedor) -> propriedades.put(nome, fornecedor.get()), "gamertag_migracao");
        var url = (String) propriedades.get("spring.datasource.url");
        var usuario = (String) propriedades.get("spring.datasource.username");
        var senha = (String) propriedades.get("spring.datasource.password");
        FlywayConfig.configuracao().dataSource(url, usuario, senha).target("1").load().migrate();
        try (var conexao = DriverManager.getConnection(url, usuario, senha);
                var sql = conexao.createStatement()) {
            sql.executeUpdate("""
                    INSERT INTO tb_usuarios (nome_completo, email, senha, matricula, perfil, gamertag,
                        declarou_maior_idade, aceitou_termos_lgpd)
                    SELECT 'Administrador', 'admin' || n || '@example.com', 'hash', 'ADMIN-' || n,
                        'ADMINISTRADOR', CASE WHEN n = 1 THEN '#1a3b5c7d' ELSE 'CV-' || n END, FALSE, FALSE
                    FROM generate_series(1, 500) n
                    """);
            sql.executeUpdate("""
                    INSERT INTO tb_forum_users (id, name, email, role)
                    SELECT id, nome_completo, email, perfil FROM tb_usuarios
                    """);
        }
        FlywayConfig.configuracao().dataSource(url, usuario, senha).load().migrate();
        try (var conexao = DriverManager.getConnection(url, usuario, senha);
                var sql = conexao.createStatement();
                var linhas = sql.executeQuery("""
                        SELECT count(*), count(DISTINCT u.gamertag),
                            bool_and(u.gamertag ~ '^#[a-z0-9]{8}$' AND u.gamertag ~ '[a-z]' AND u.gamertag ~ '[0-9]'),
                            bool_and(f.gamertag = u.gamertag),
                            bool_and(u.matricula <> 'ADMIN-1' OR u.gamertag = '#1a3b5c7d')
                        FROM tb_usuarios u JOIN tb_forum_users f ON f.id = u.id
                        """)) {
            assertThat(linhas.next()).isTrue();
            assertThat(linhas.getInt(1)).isEqualTo(500);
            assertThat(linhas.getInt(2)).isEqualTo(500);
            assertThat(linhas.getBoolean(3)).isTrue();
            assertThat(linhas.getBoolean(4)).isTrue();
            assertThat(linhas.getBoolean(5)).isTrue();
        }
    }
}
