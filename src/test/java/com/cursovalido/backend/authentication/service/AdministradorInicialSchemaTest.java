package com.cursovalido.backend.authentication.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class AdministradorInicialSchemaTest {
    @Test
    void migracoesPermitemAdminSemDadosPessoaisMasPreservamObrigatoriedadeParaAluno() throws Exception {
        try (var conexao = DriverManager.getConnection("jdbc:h2:mem:admin_schema;MODE=PostgreSQL")) {
            for (String script : new String[] { "V1__criar_schema_inicial.sql", "V2__criar_estrutura_aulas.sql" }) {
                try (var leitor = new InputStreamReader(
                        new ClassPathResource("db/migration/" + script).getInputStream(), StandardCharsets.UTF_8)) {
                    RunScript.execute(conexao, leitor);
                }
            }
            try (var inserir = conexao.prepareStatement("""
                    INSERT INTO tb_usuarios (nome_completo, email, senha, matricula, perfil,
                        declarou_maior_idade, aceitou_termos_lgpd, email_confirmado)
                    VALUES ('Administrador', ?, 'hash', ?, ?, FALSE, FALSE, TRUE)
                    """)) {
                inserir.setString(1, AdministradorInicialServico.EMAIL);
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
