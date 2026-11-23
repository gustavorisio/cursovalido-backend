package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.forum.configuration.FlywayConfig;
import com.cursovalido.backend.forum.support.PostgresTeste;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(PostgresTeste.CicloDeVida.class)
class AdministradorInicialSchemaTest {
    @Test
    void migracoesPermitemAdminSemDadosPessoaisMasPreservamObrigatoriedadeParaAluno() throws Exception {
        var propriedades = new HashMap<String, Object>();
        PostgresTeste.configurar(
                (nome, fornecedor) -> propriedades.put(nome, fornecedor.get()), "admin_schema");
        String url = (String) propriedades.get("spring.datasource.url");
        String usuario = (String) propriedades.get("spring.datasource.username");
        String senha = (String) propriedades.get("spring.datasource.password");
        FlywayConfig.configuracao().dataSource(url, usuario, senha).load().migrate();
        try (var conexao = DriverManager.getConnection(url, usuario, senha)) {
            try (var inserir = conexao.prepareStatement("""
                    INSERT INTO tb_usuarios (nome_completo, email, senha, matricula, perfil, gamertag,
                        declarou_maior_idade, aceitou_termos_lgpd, email_confirmado)
                    VALUES ('Administrador', ?, 'hash', ?, ?, 'CV-AAAA2345BBBB', FALSE, FALSE, TRUE)
                    """)) {
                inserir.setString(1, "admin-configurado@example.com");
                inserir.setString(2, "ADMIN-INICIAL");
                inserir.setString(3, "ADMINISTRADOR");
                assertEquals(1, inserir.executeUpdate());

                inserir.setString(1, "aluno@example.com");
                inserir.setString(2, "12345678");
                inserir.setString(3, "ALUNO");
                assertThrows(SQLException.class, inserir::executeUpdate);
            }
        }
    }
}
