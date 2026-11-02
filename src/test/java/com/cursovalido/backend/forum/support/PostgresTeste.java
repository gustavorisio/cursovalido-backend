package com.cursovalido.backend.forum.support;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Banco exclusivo dos testes; nunca le DB_HOST, DB_NAME ou credenciais de desenvolvimento. */
public final class PostgresTeste {
    private static final Banco BANCO = iniciar();

    private PostgresTeste() { }

    public static void configurar(DynamicPropertyRegistry registry, String schema) {
        if (!schema.matches("[a-z_]+")) throw new IllegalArgumentException("Schema de teste invalido");
        try (var connection = DriverManager.getConnection(BANCO.url(), BANCO.usuario(), BANCO.senha());
                var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
        } catch (Exception e) {
            throw new IllegalStateException("Nao foi possivel preparar o schema de teste", e);
        }
        registry.add("spring.datasource.url", () -> BANCO.url() + (BANCO.url().contains("?") ? "&" : "?") + "currentSchema=" + schema);
        registry.add("spring.datasource.username", BANCO::usuario);
        registry.add("spring.datasource.password", BANCO::senha);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    private static Banco iniciar() {
        String modo = System.getProperty("test.postgres.mode", "auto");
        if (!List.of("auto", "local", "container").contains(modo)) throw new IllegalArgumentException("Modo de PostgreSQL invalido: " + modo);
        Path bin = localizarBinarios();
        if (!modo.equals("container") && bin != null) return iniciarLocal(bin);
        if (modo.equals("local")) throw new IllegalStateException("Configure TEST_POSTGRES_BIN com a pasta bin do PostgreSQL");
        var container = new PostgreSQLContainer("postgres:18-alpine")
                .withDatabaseName("forum_testes")
                .withUsername("teste").withPassword("teste")
                .withStartupTimeout(Duration.ofSeconds(60));
        container.start();
        return new Banco(container.getJdbcUrl(), container.getUsername(), container.getPassword(), container::stop);
    }

    private static Path localizarBinarios() {
        String configurado = System.getenv("TEST_POSTGRES_BIN");
        if (configurado != null && !configurado.isBlank()) return Path.of(configurado);
        if (System.getProperty("os.name").startsWith("Windows")) {
            Path raiz = Path.of(System.getenv().getOrDefault("ProgramFiles", "C:\\Program Files"), "PostgreSQL");
            if (Files.isDirectory(raiz)) {
                try (var versoes = Files.list(raiz)) {
                    return versoes.sorted(Comparator.reverseOrder()).map(p -> p.resolve("bin"))
                            .filter(p -> Files.isRegularFile(p.resolve("initdb.exe")))
                            .findFirst().orElse(null);
                } catch (IOException e) {
                    throw new IllegalStateException("Nao foi possivel localizar PostgreSQL", e);
                }
            }
        }
        return null;
    }

    private static Banco iniciarLocal(Path bin) {
        try {
            Path temporario = Files.createTempDirectory("cursovalido-pg-teste-").toAbsolutePath();
            Path dados = temporario.resolve("data");
            String sufixo = System.getProperty("os.name").startsWith("Windows") ? ".exe" : "";
            String initdb = bin.resolve("initdb" + sufixo).toString();
            String pgctl = bin.resolve("pg_ctl" + sufixo).toString();
            int porta;
            try (ServerSocket socket = new ServerSocket(0)) { porta = socket.getLocalPort(); }
            executar(temporario, initdb, "-D", dados.toString(), "-U", "teste", "-A", "trust", "--encoding=UTF8", "--no-locale");
            executar(temporario, pgctl, "-D", dados.toString(), "-l", temporario.resolve("postgres.log").toString(),
                    "-o", "-h 127.0.0.1 -p " + porta + " -F", "-w", "start");
            Runnable encerrar = () -> {
                try {
                    executar(temporario, pgctl, "-D", dados.toString(), "-m", "immediate", "-w", "stop");
                    // Somente a pasta exclusiva criada por createTempDirectory acima.
                    try (var paths = Files.walk(temporario)) {
                        for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
                    }
                } catch (Exception e) { throw new IllegalStateException("Falha ao encerrar PostgreSQL de teste", e); }
            };
            return new Banco("jdbc:postgresql://127.0.0.1:" + porta + "/postgres", "teste", "", encerrar);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao iniciar PostgreSQL temporario. Use -Dtest.postgres.mode=container com Docker ou configure TEST_POSTGRES_BIN", e);
        }
    }

    private static void executar(Path temporario, String... comando) throws Exception {
        Path log = temporario.resolve("processo.log");
        var processo = new ProcessBuilder(new ArrayList<>(List.of(comando)))
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!processo.waitFor(45, TimeUnit.SECONDS)) {
            processo.destroyForcibly();
            throw new IllegalStateException("Tempo excedido iniciando banco de teste");
        }
        if (processo.exitValue() != 0) throw new IllegalStateException(Files.readString(log));
    }

    /** Fecha o servidor antes do encerramento da JVM, depois de todos os testes. */
    public static class CicloDeVida implements BeforeAllCallback {
        @Override
        public void beforeAll(ExtensionContext context) {
            context.getRoot().getStore(ExtensionContext.Namespace.create(PostgresTeste.class))
                    .getOrComputeIfAbsent("postgres", key -> (ExtensionContext.Store.CloseableResource)
                            () -> BANCO.encerrar().run());
        }
    }

    private record Banco(String url, String usuario, String senha, Runnable encerrar) { }
}
