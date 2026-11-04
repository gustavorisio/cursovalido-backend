package com.cursovalido.backend.authentication.service;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.DriverManager;
import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.extension.ExtendWith(com.cursovalido.backend.forum.support.PostgresTeste.CicloDeVida.class)
class AdministradorInicialSchemaTest {
    @Test
    void migracoesPermitemAdminSemDadosPessoaisMasPreservamObrigatoriedadeParaAluno() throws Exception {
        var propriedades = new java.util.HashMap<String, Object>();
        com.cursovalido.backend.forum.support.PostgresTeste.configurar(
                (nome, fornecedor) -> propriedades.put(nome, fornecedor.get()), "admin_schema");
        String url = (String) propriedades.get("spring.datasource.url");
        String usuario = (String) propriedades.get("spring.datasource.username");
        String senha = (String) propriedades.get("spring.datasource.password");
        Flyway.configure().dataSource(url, usuario, senha).load().migrate();
        try (var conexao = DriverManager.getConnection(url, usuario, senha)) {
            try (var inserir = conexao.prepareStatement("""
                    INSERT INTO tb_usuarios (nome_completo, email, senha, matricula, perfil,
                        declarou_maior_idade, aceitou_termos_lgpd, email_confirmado)
                    VALUES ('Administrador', ?, 'hash', ?, ?, FALSE, FALSE, TRUE)
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
