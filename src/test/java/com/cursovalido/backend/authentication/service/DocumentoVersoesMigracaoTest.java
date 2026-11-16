package com.cursovalido.backend.authentication.service;

import static org.junit.jupiter.api.Assertions.*;

import com.cursovalido.backend.forum.configuration.FlywayConfig;
import com.cursovalido.backend.forum.support.PostgresTeste;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(PostgresTeste.CicloDeVida.class)
class DocumentoVersoesMigracaoTest {
    @Test
    void migraHistoricoLegadoSemReescreverVersoesOuConteudo() throws Exception {
        var propriedades = new HashMap<String, Object>();
        PostgresTeste.configurar((nome, fornecedor) -> propriedades.put(nome, fornecedor.get()), "versoes_migracao");
        String url = (String) propriedades.get("spring.datasource.url");
        String usuario = (String) propriedades.get("spring.datasource.username");
        String senha = (String) propriedades.get("spring.datasource.password");
        FlywayConfig.configuracao().dataSource(url, usuario, senha).target("3").load().migrate();
        try (var conexao = DriverManager.getConnection(url, usuario, senha); var sql = conexao.createStatement()) {
            sql.execute("INSERT INTO tb_politicas(nome, versao, conteudo, ativa, data_publicacao) "
                    + "VALUES ('Legado', '7.9', 'Conteudo preservado', false, CURRENT_TIMESTAMP), "
                    + "('Rotulo', 'teste-v99', 'Outro conteudo', false, CURRENT_TIMESTAMP)");
            FlywayConfig.configuracao().dataSource(url, usuario, senha).load().migrate();
            try (var resultado = sql.executeQuery("SELECT numero FROM tb_versoes_documentos WHERE tipo='TERMOS'")) {
                assertTrue(resultado.next());
                long numero = resultado.getLong(1);
                assertEquals(35, numero);
                assertEquals("v8.0",
                        new com.cursovalido.backend.authentication.entity.VersaoDocumento("TERMOS", numero)
                                .incrementar());
            }
            try (var resultado = sql.executeQuery("SELECT conteudo FROM tb_politicas WHERE versao='7.9'")) {
                assertTrue(resultado.next());
                assertEquals("Conteudo preservado", resultado.getString(1));
            }
            assertThrows(SQLException.class, () -> sql.execute("INSERT INTO tb_politicas "
                    + "(nome, versao, conteudo, ativa, data_publicacao) "
                    + "VALUES ('Duplicado', '7.9', 'Texto', false, CURRENT_TIMESTAMP)"));
        }
    }
}
