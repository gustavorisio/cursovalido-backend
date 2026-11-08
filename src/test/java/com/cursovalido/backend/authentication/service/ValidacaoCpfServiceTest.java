package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.exception.MenorDeIdadeException;
import com.cursovalido.backend.authentication.exception.ValidacaoCpfException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class ValidacaoCpfServiceTest {
    private HttpServer servidor;
    private final AtomicInteger chamadas = new AtomicInteger();
    private final AtomicReference<String> caminho = new AtomicReference<>();
    private static final String CPF = "52998224725";
    private static final LocalDate NASCIMENTO = LocalDate.of(1990, 1, 1);

    @AfterEach void encerrar() { if (servidor != null) servidor.stop(0); }

    private ValidacaoCpfService cliente(int status, String corpo) throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", troca -> {
            chamadas.incrementAndGet(); caminho.set(troca.getRequestURI().getPath());
            byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().set("Content-Type", "application/json");
            troca.sendResponseHeaders(status, bytes.length);
            try (var saida = troca.getResponseBody()) { saida.write(bytes); }
        });
        servidor.start();
        return new ValidacaoCpfService(url(), "segredo-ficticio", 1000, 1000);
    }

    private String url() { return "http://127.0.0.1:" + servidor.getAddress().getPort(); }
    private String sucesso(String nascimento) {
        return "{\"status\":1,\"cpf\":\"529.982.247-25\",\"nome\":\"Nome\","
                + "\"nascimento\":\"" + nascimento + "\",\"situacao\":\"Pendente de Regularização\",\"pacoteUsado\":26}";
    }

    @Test void consultaPacote26NoBackendEConfereNascimentoSemExigirRegularidadeFiscal() throws Exception {
        cliente(200, sucesso("01/01/1990")).validar(CPF, "Nome", NASCIMENTO);
        assertThat(caminho.get()).isEqualTo("/segredo-ficticio/26/52998224725");
        assertThat(chamadas.get()).isEqualTo(1);
    }

    @Test void nascimentoDivergenteImpedeCadastro() throws Exception {
        var servico = cliente(200, sucesso("02/01/1990"));
        assertThatThrownBy(() -> servico.validar(CPF, "Nome", NASCIMENTO))
                .isInstanceOfSatisfying(ValidacaoCpfException.class,
                        erro -> assertThat(erro.getStatusCode().value()).isEqualTo(422));
    }

    @ParameterizedTest @ValueSource(strings = {"Maria da Silva", "João Silva", "João da Silva Junior"})
    void nomeDiferenteAbreviadoOuIncompletoImpedeCadastro(String nome) throws Exception {
        var servico = cliente(200, sucesso("01/01/1990").replace("\"Nome\"", "\"João da Silva\""));
        assertThatThrownBy(() -> servico.validar(CPF, nome, NASCIMENTO))
                .isInstanceOfSatisfying(ValidacaoCpfException.class, erro -> {
                    assertThat(erro.getStatusCode().value()).isEqualTo(422);
                    assertThat(erro.getCodigo()).isEqualTo("CPF_DADOS_INVALIDOS");
                });
    }

    @ParameterizedTest @ValueSource(strings = {"João da Silva", "JOAO DA SILVA", "  joão   da silva  "})
    void nomeToleraApenasAcentuacaoCaixaEEspacos(String nome) throws Exception {
        cliente(200, sucesso("01/01/1990").replace("\"Nome\"", "\"João da Silva\""))
                .validar(CPF, nome, NASCIMENTO);
    }

    @ParameterizedTest @ValueSource(strings = {"null", "\"\"", "123"})
    void nomeAusenteOuInvalidoNoFornecedorNaoLiberaCadastro(String nome) throws Exception {
        assertIndisponivel(cliente(200, sucesso("01/01/1990").replace("\"nome\":\"Nome\"", "\"nome\":" + nome)));
    }

    @Test void nascimentoRetornadoDeMenorImpedeCadastro() throws Exception {
        String data = LocalDate.now().minusYears(17).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu"));
        var servico = cliente(200, sucesso(data));
        assertThatThrownBy(() -> servico.validar(CPF, "Nome", NASCIMENTO)).isInstanceOf(MenorDeIdadeException.class);
    }

    @Test void menorDeclaradoNaoConsomeConsulta() throws Exception {
        var servico = cliente(200, sucesso("01/01/1990"));
        assertThatThrownBy(() -> servico.validar(CPF, "Nome", LocalDate.now().minusYears(18).plusDays(1)))
                .isInstanceOf(MenorDeIdadeException.class);
        assertThat(chamadas.get()).isZero();
    }

    @ParameterizedTest @ValueSource(ints = {100, 101, 102})
    void errosDeDocumentoSaoDistintosDeFalhasOperacionais(int codigo) throws Exception {
        var servico = cliente(200, "{\"status\":0,\"erroCodigo\":" + codigo + "}");
        assertThatThrownBy(() -> servico.validar(CPF, "Nome", NASCIMENTO))
                .isInstanceOfSatisfying(ValidacaoCpfException.class, erro -> {
                    assertThat(erro.getStatusCode().value()).isEqualTo(422);
                    assertThat(erro.getCodigo()).isEqualTo("CPF_DADOS_INVALIDOS");
                });
    }

    @ParameterizedTest @ValueSource(ints = {1000, 1001, 1002, 1003, 1004, 1006, 1007, 1010})
    void tokenSaldoELimitesNaoSaoApresentadosComoCpfInvalido(int codigo) throws Exception {
        var servico = cliente(200, "{\"status\":0,\"erro\":\"segredo-ficticio\",\"erroCodigo\":" + codigo + "}");
        assertIndisponivel(servico);
    }

    @ParameterizedTest @ValueSource(ints = {401, 403, 429, 500, 503})
    void falhasHttpNaoExpoemTokenNemCpfENaoSaoRepetidas(int status) throws Exception {
        assertIndisponivel(cliente(status, "{\"erro\":\"segredo-ficticio\"}"));
        assertThat(chamadas.get()).isEqualTo(1);
    }

    @ParameterizedTest @ValueSource(strings = {"{", "null", "{}", "[]", "{\"status\":true}",
            "{\"status\":1,\"pacoteUsado\":26}",
            "{\"status\":1,\"pacoteUsado\":7,\"cpf\":\"52998224725\",\"nascimento\":\"01/01/1990\"}",
            "{\"status\":1,\"pacoteUsado\":26,\"cpf\":\"11144477735\",\"nascimento\":\"01/01/1990\"}",
            "{\"status\":1,\"pacoteUsado\":26,\"cpf\":\"52998224725\",\"nascimento\":\"31/02/1990\"}"})
    void respostaIncompletaInvalidaOuDeOutroCpfNaoLiberaCadastro(String corpo) throws Exception {
        assertIndisponivel(cliente(200, corpo));
    }

    @ParameterizedTest @ValueSource(strings = {"", "5ae973d7a997af13f0aaf2bf60e65803"})
    void semTokenRealNaoLiberaCadastroNemConsultaFornecedor(String token) throws Exception {
        cliente(200, sucesso("01/01/1990"));
        assertIndisponivel(new ValidacaoCpfService(url(), token, 1000, 1000));
        assertThat(chamadas.get()).isZero();
    }

    @Test void timeoutNaoLiberaCadastro() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var liberar = new CountDownLatch(1);
        servidor.createContext("/", troca -> {
            try { liberar.await(5, TimeUnit.SECONDS); }
            catch (InterruptedException erro) { Thread.currentThread().interrupt(); }
            finally { troca.close(); }
        });
        servidor.start();
        try { assertIndisponivel(new ValidacaoCpfService(url(), "segredo-ficticio", 1000, 150)); }
        finally { liberar.countDown(); }
    }

    private void assertIndisponivel(ValidacaoCpfService servico) {
        assertThatThrownBy(() -> servico.validar(CPF, "Nome", NASCIMENTO))
                .isInstanceOfSatisfying(ValidacaoCpfException.class, erro -> {
                    assertThat(erro.getStatusCode().value()).isEqualTo(503);
                    assertThat(erro.getCodigo()).isEqualTo("CPF_CONSULTA_INDISPONIVEL");
                    assertThat(erro.getCause()).isNull();
                    assertThat(erro.toString()).doesNotContain("segredo-ficticio", CPF);
                });
    }
}
