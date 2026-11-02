package com.cursovalido.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Politica;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.UsuarioRepositorio;
import com.cursovalido.backend.authentication.repository.PoliticaRepositorio;
import com.cursovalido.backend.authentication.service.EmailServico;
import com.cursovalido.backend.forum.entity.Topico;
import com.cursovalido.backend.forum.repository.ComentarioRepositorio;
import com.cursovalido.backend.forum.repository.TopicoRepositorio;
import com.cursovalido.backend.forum.repository.UsuarioForumRepositorio;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ForumPoliticasIntegracaoTest {
    @LocalServerPort
    private int portaServidor;
    @Autowired
    private UsuarioRepositorio repositorioUsuarios;
    @Autowired
    private TopicoRepositorio repositorioTopicos;
    @Autowired
    private ComentarioRepositorio repositorioComentarios;
    @Autowired
    private PoliticaRepositorio repositorioPoliticas;
    @Autowired
    private UsuarioForumRepositorio repositorioUsuariosForum;
    // Impede o envio pela Resend; o código 2FA ainda é gerado e validado pela
    // aplicação.
    @MockitoBean
    private EmailServico servicoEmail;
    private final JsonMapper leitorJson = JsonMapper.builder().build();
    private RestClient clienteHttp;
    private final Random geradorDigitosCpf = new Random();
    private String versaoPoliticaCadastro;
    private String tokenProfessor;
    private String tokenAdministrador;

    @BeforeEach
    void configurarClienteHttpEAutenticarProfessorEAdministrador() {
        // A porta aleatória permite executar estes testes com a aplicação aberta na
        // porta 8080.
        clienteHttp = RestClient.builder()
                .baseUrl("http://localhost:" + portaServidor)
                .defaultStatusHandler(status -> true, (requisicao, resposta) -> {
                    // Respostas 400, 401, 403, 404 e 409 serão conferidas nas asserções dos testes.
                })
                .build();
        // O primeiro cadastro precisa de uma política vigente antes de existir um
        // administrador autenticado.
        versaoPoliticaCadastro = "cadastro-" + UUID.randomUUID().toString().substring(0, 20);
        repositorioPoliticas.saveAndFlush(new Politica("Politica de privacidade do Curso Valido",
                versaoPoliticaCadastro, "Os dados identificam os participantes do forum."));
        tokenProfessor = criarUsuarioEObterTokenPorLogin(Perfil.PROFESSOR);
        tokenAdministrador = criarUsuarioEObterTokenPorLogin(Perfil.ADMINISTRADOR);
    }

    private String criarUsuarioEObterTokenPorLogin(Perfil perfil) {
        String email = UUID.randomUUID() + "@teste.local";
        String nomeCompleto = perfil == Perfil.PROFESSOR ? "Marina Costa" : "Rafael Almeida";
        String cpf;
        do {
            cpf = gerarCpfParaCadastro();
        } while (repositorioUsuarios.existsByCpf(cpf));
        Map<String, Object> cadastro = Map.of(
                "nomeCompleto", nomeCompleto,
                "email", email,
                "senha", "SenhaTeste123!",
                "cpf", cpf,
                "dataNascimento", "1990-01-01",
                "telefone", "11987654321",
                "endereco", Map.of("cep", "08780220", "logradouro", "Rua Doutor Deodato Wertheimer",
                        "bairro", "Centro", "cidade", "Mogi das Cruzes", "estado", "SP"),
                "declarouMaiorIdade", true,
                "aceitouTermosLgpd", true,
                "versaoTermos", versaoPoliticaCadastro);

        // O cadastro e a confirmação do e-mail passam pelos mesmos endpoints usados
        // pelo aluno.
        clearInvocations(servicoEmail);
        ResponseEntity<String> respostaCadastro = enviarRequisicao(HttpMethod.POST,
                "/api/autenticacao/cadastro", null, cadastro);
        assertThat(respostaCadastro.getStatusCode().value()).isEqualTo(201);
        ArgumentCaptor<String> tokenConfirmacao = ArgumentCaptor.forClass(String.class);
        verify(servicoEmail).enviarConfirmacao(any(Usuario.class), tokenConfirmacao.capture());
        ResponseEntity<String> respostaConfirmacaoEmail = enviarRequisicao(HttpMethod.POST,
                "/api/autenticacao/confirmar-email", null, Map.of("token", tokenConfirmacao.getValue()));
        assertThat(respostaConfirmacaoEmail.getStatusCode().value()).isEqualTo(200);

        // O cadastro público cria ALUNO. Não existe uma rota pública para promover
        // professor ou administrador.
        Usuario usuario = repositorioUsuarios.findByEmail(email).orElseThrow();
        assertThat(usuario.isEmailConfirmado()).isTrue();
        usuario.setPerfil(perfil);
        repositorioUsuarios.saveAndFlush(usuario);

        // Captura o código destinado a este login, sem depender de uma caixa de e-mail
        // real.
        clearInvocations(servicoEmail);
        ArgumentCaptor<String> codigoEnviadoPorEmail = ArgumentCaptor.forClass(String.class);
        ResponseEntity<String> respostaLogin = enviarRequisicao(HttpMethod.POST, "/api/autenticacao/login", null,
                Map.of("email", usuario.getEmail(), "senha", "SenhaTeste123!"));
        assertThat(respostaLogin.getStatusCode().value()).isEqualTo(200);
        assertThat(lerCorpoJson(respostaLogin).path("requerCodigo2fa").asBoolean()).isTrue();
        verify(servicoEmail).enviarCodigo2fa(any(Usuario.class), codigoEnviadoPorEmail.capture());

        ResponseEntity<String> respostaConfirmacao2fa = enviarRequisicao(
                HttpMethod.POST, "/api/autenticacao/confirmar-2fa", null,
                Map.of("email", usuario.getEmail(), "codigo", codigoEnviadoPorEmail.getValue()));
        assertThat(respostaConfirmacao2fa.getStatusCode().value()).isEqualTo(200);
        String token = lerCorpoJson(respostaConfirmacao2fa).path("token").asText();
        assertThat(token).isNotBlank();
        // A confirmação do cadastro insere o aluno no fórum; o 2FA atualiza o mesmo
        // registro com o novo perfil.
        assertThat(repositorioUsuariosForum.findById(usuario.getId()).orElseThrow().getPerfil())
                .isEqualTo(perfil.name());
        return token;
    }

    private String gerarCpfParaCadastro() {
        StringBuilder cpf = new StringBuilder();
        for (int posicao = 0; posicao < 9; posicao++) {
            cpf.append(geradorDigitosCpf.nextInt(10));
        }
        // Calcula os dois dígitos verificadores para enviar um CPF com formato válido
        // ao cadastro.
        for (int quantidadeDigitos = 9; quantidadeDigitos <= 10; quantidadeDigitos++) {
            int soma = 0;
            for (int posicao = 0; posicao < quantidadeDigitos; posicao++) {
                int digito = Character.getNumericValue(cpf.charAt(posicao));
                soma += digito * (quantidadeDigitos + 1 - posicao);
            }
            int verificador = 11 - soma % 11;
            if (verificador >= 10) {
                verificador = 0;
            }
            cpf.append(verificador);
        }
        return cpf.toString();
    }

    private ResponseEntity<String> enviarRequisicao(HttpMethod metodo, String rota, String token,
            Object corpoRequisicao) {
        RestClient.RequestBodySpec requisicao = clienteHttp.method(metodo).uri(rota)
                .contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            requisicao.headers(cabecalhos -> cabecalhos.setBearerAuth(token));
        }
        if (corpoRequisicao != null) {
            requisicao.body(corpoRequisicao);
        }
        return requisicao.retrieve().toEntity(String.class);
    }

    private JsonNode lerCorpoJson(ResponseEntity<String> resposta) {
        assertThat(resposta.getBody()).isNotBlank();
        return leitorJson.readTree(resposta.getBody());
    }

    private long criarTopicoSobreTestesIntegracao() {
        ResponseEntity<String> resposta = enviarRequisicao(HttpMethod.POST, "/api/forum/topicos", tokenProfessor,
                Map.of("titulo", "Como testar topicos usando H2? " + UUID.randomUUID(),
                        "descricao", "Quero verificar se o topico fica salvo no banco H2."));
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(lerCorpoJson(resposta).path("descricao").asText())
                .isEqualTo("Quero verificar se o topico fica salvo no banco H2.");
        assertThat(lerCorpoJson(resposta).path("idAutor").asLong()).isPositive();
        long idTopico = lerCorpoJson(resposta).path("id").asLong();
        assertThat(idTopico).isPositive();
        return idTopico;
    }

    private long criarComentarioNoTopico(long idTopico) {
        ResponseEntity<String> resposta = enviarRequisicao(
                HttpMethod.POST, "/api/forum/topicos/" + idTopico + "/comentarios", tokenProfessor,
                Map.of("conteudo", "Consulte o topico pelo repositorio depois da requisicao HTTP."));
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(lerCorpoJson(resposta).path("conteudo").asText())
                .isEqualTo("Consulte o topico pelo repositorio depois da requisicao HTTP.");
        return lerCorpoJson(resposta).path("id").asLong();
    }

    private Map<String, String> montarPoliticaPrivacidade() {
        // A versão fica abaixo do limite de 30 caracteres e não repete publicações
        // anteriores.
        return Map.of("nome", "Politica de privacidade do Curso Valido",
                "versao", UUID.randomUUID().toString().substring(0, 25),
                "conteudo", "Os dados de cadastro identificam o autor de topicos e comentarios.");
    }

    private void conferirErro(ResponseEntity<String> resposta, int statusEsperado, String mensagemEsperada) {
        assertThat(resposta.getStatusCode().value()).isEqualTo(statusEsperado);
        assertThat(lerCorpoJson(resposta).toString()).contains(mensagemEsperada);
    }

    // A descrição é obrigatória; a recusa não pode deixar um tópico incompleto no banco.
    @Test
    void criarTopico_comDescricaoVazia_deveRetornar400SemPersistir() {
        long quantidadeTopicosAntes = repositorioTopicos.count();
        ResponseEntity<String> resposta = enviarRequisicao(HttpMethod.POST, "/api/forum/topicos",
                tokenProfessor, Map.of("titulo", "Como consultar comentarios do forum?", "descricao", ""));

        conferirErro(resposta, 400, "Bad Request");
        assertThat(repositorioTopicos.count()).isEqualTo(quantidadeTopicosAntes);
    }

    // O limite da descrição é 5000 caracteres, portanto 5001 devem ser rejeitados.
    @Test
    void criarTopico_comDescricaoAcimaDe5000Caracteres_deveRetornar400SemPersistir() {
        long quantidadeTopicosAntes = repositorioTopicos.count();
        String descricaoAcimaDoLimite = "Explicacao sobre a consulta de comentarios. ".repeat(150).substring(0, 5001);
        assertThat(descricaoAcimaDoLimite.length()).isGreaterThan(5000);
        ResponseEntity<String> resposta = enviarRequisicao(HttpMethod.POST, "/api/forum/topicos",
                tokenProfessor, Map.of("titulo", "Como paginar os comentarios?", "descricao", descricaoAcimaDoLimite));

        conferirErro(resposta, 400, "Bad Request");
        assertThat(repositorioTopicos.count()).isEqualTo(quantidadeTopicosAntes);
    }

    // Um comentário vazio não deve aumentar a quantidade de respostas persistidas.
    @Test
    void adicionarComentario_comConteudoVazio_deveRetornar400SemPersistir() {
        long idTopico = criarTopicoSobreTestesIntegracao();
        long quantidadeComentariosAntes = repositorioComentarios.count();
        ResponseEntity<String> resposta = enviarRequisicao(HttpMethod.POST,
                "/api/forum/topicos/" + idTopico + "/comentarios", tokenProfessor, Map.of("conteudo", ""));

        conferirErro(resposta, 400, "Bad Request");
        assertThat(repositorioComentarios.count()).isEqualTo(quantidadeComentariosAntes);
    }

    // O conteúdo do comentário tem o mesmo limite de 5000 caracteres da descrição do tópico.
    @Test
    void adicionarComentario_comConteudoAcimaDe5000Caracteres_deveRetornar400SemPersistir() {
        long idTopico = criarTopicoSobreTestesIntegracao();
        long quantidadeComentariosAntes = repositorioComentarios.count();
        String conteudoAcimaDoLimite = "Consulte os comentarios pelo identificador do topico. ".repeat(150).substring(0, 5001);
        assertThat(conteudoAcimaDoLimite.length()).isGreaterThan(5000);
        ResponseEntity<String> resposta = enviarRequisicao(HttpMethod.POST,
                "/api/forum/topicos/" + idTopico + "/comentarios", tokenProfessor,
                Map.of("conteudo", conteudoAcimaDoLimite));

        conferirErro(resposta, 400, "Bad Request");
        assertThat(repositorioComentarios.count()).isEqualTo(quantidadeComentariosAntes);
    }

    // O administrador pode excluir tópicos, mas a edição do texto continua restrita ao autor.
    @Test
    void editarTopico_comAdministradorQueNaoEAutor_deveRetornar403SemAlterar() {
        long idTopico = criarTopicoSobreTestesIntegracao();
        String tituloOriginal = repositorioTopicos.findById(idTopico).orElseThrow().getTitulo();
        String descricaoOriginal = repositorioTopicos.findById(idTopico).orElseThrow().getDescricao();
        ResponseEntity<String> resposta = enviarRequisicao(HttpMethod.PUT, "/api/forum/topicos/" + idTopico,
                tokenAdministrador, Map.of("titulo", "Titulo alterado pelo administrador",
                        "descricao", "Tentativa de editar a duvida de outra pessoa."));

        conferirErro(resposta, 403, "Somente o autor pode editar o topico");
        Topico topicoPersistido = repositorioTopicos.findById(idTopico).orElseThrow();
        assertThat(topicoPersistido.getTitulo()).isEqualTo(tituloOriginal);
        assertThat(topicoPersistido.getDescricao()).isEqualTo(descricaoOriginal);
    }

    // O perfil de administrador não permite modificar o conteúdo de uma resposta de outro autor.
    @Test
    void editarComentario_comAdministradorQueNaoEAutor_deveRetornar403SemAlterar() {
        long idTopico = criarTopicoSobreTestesIntegracao();
        long idComentario = criarComentarioNoTopico(idTopico);
        String conteudoOriginal = repositorioComentarios.findById(idComentario).orElseThrow().getConteudo();
        ResponseEntity<String> resposta = enviarRequisicao(HttpMethod.PUT, "/api/forum/comentarios/" + idComentario,
                tokenAdministrador, Map.of("conteudo", "Tentativa de substituir a resposta do professor."));

        conferirErro(resposta, 403, "Somente o autor pode editar o comentario");
        assertThat(repositorioComentarios.findById(idComentario).orElseThrow().getConteudo())
                .isEqualTo(conteudoOriginal);
    }

    // A edição de um tópico ausente precisa retornar 404 sem criar um registro substituto.
    @Test
    void editarTopico_comIdentificadorInexistente_deveRetornar404SemPersistir() {
        long quantidadeTopicosAntes = repositorioTopicos.count();
        ResponseEntity<String> resposta = enviarRequisicao(HttpMethod.PUT, "/api/forum/topicos/" + Long.MAX_VALUE,
                tokenProfessor, Map.of("titulo", "Como listar minhas duvidas?", "descricao", "Consulta de topicos por autor."));

        conferirErro(resposta, 404, "Topico nao encontrado");
        assertThat(repositorioTopicos.count()).isEqualTo(quantidadeTopicosAntes);
    }

    // Informar um comentário inexistente no PUT não pode cadastrar uma nova resposta.
    @Test
    void editarComentario_comIdentificadorInexistente_deveRetornar404SemPersistir() {
        long quantidadeComentariosAntes = repositorioComentarios.count();
        ResponseEntity<String> resposta = enviarRequisicao(HttpMethod.PUT,
                "/api/forum/comentarios/" + Long.MAX_VALUE, tokenProfessor,
                Map.of("conteudo", "Esta resposta ainda nao existe no forum."));

        conferirErro(resposta, 404, "Comentario nao encontrado");
        assertThat(repositorioComentarios.count()).isEqualTo(quantidadeComentariosAntes);
    }

    // O tópico arquivado permanece salvo, mas suas respostas não ficam disponíveis para consulta.
    @Test
    void listarComentarios_comTopicoArquivado_deveRetornar404SemExcluirResposta() {
        long idTopico = criarTopicoSobreTestesIntegracao();
        long idComentario = criarComentarioNoTopico(idTopico);
        ResponseEntity<String> respostaArquivamento = enviarRequisicao(HttpMethod.PUT,
                "/api/forum/topicos/" + idTopico + "/arquivar", tokenAdministrador, null);
        assertThat(respostaArquivamento.getStatusCode().value()).isEqualTo(200);

        ResponseEntity<String> respostaConsulta = enviarRequisicao(HttpMethod.GET,
                "/api/forum/topicos/" + idTopico + "/comentarios", tokenProfessor, null);

        conferirErro(respostaConsulta, 404, "Topico arquivado");
        assertThat(repositorioTopicos.findById(idTopico).orElseThrow().isAtivo()).isFalse();
        assertThat(repositorioComentarios.findById(idComentario).orElseThrow().isAtivo()).isTrue();
    }

    // A publicação substitui a política vigente e a versão anterior deixa a listagem pública.
    @Test
    void publicarPolitica_comNovaVersao_deveDesativarAnteriorEListarSomenteAtual() {
        Map<String, String> novaPolitica = montarPoliticaPrivacidade();
        ResponseEntity<String> respostaPublicacao = enviarRequisicao(HttpMethod.POST, "/api/politicas",
                tokenAdministrador, novaPolitica);
        assertThat(respostaPublicacao.getStatusCode().value()).isEqualTo(201);
        assertThat(lerCorpoJson(respostaPublicacao).path("versao").asText()).isEqualTo(novaPolitica.get("versao"));

        ResponseEntity<String> respostaConsulta = enviarRequisicao(HttpMethod.GET, "/api/politicas/ativas", null, null);

        assertThat(respostaConsulta.getStatusCode().value()).isEqualTo(200);
        assertThat(lerCorpoJson(respostaConsulta).size()).isEqualTo(1);
        assertThat(lerCorpoJson(respostaConsulta).get(0).path("versao").asText())
                .isEqualTo(novaPolitica.get("versao"));
        assertThat(repositorioPoliticas.findByVersao(versaoPoliticaCadastro).orElseThrow().isAtiva()).isFalse();
        assertThat(repositorioPoliticas.findByVersao(novaPolitica.get("versao")).orElseThrow().isAtiva()).isTrue();
    }

    // O tópico precisa continuar ativo e guardar o autor depois que a requisição de
    // criação termina.
    @Test
    void criarTopico_comTituloEDescricao_devePersistirAutorEConteudo() {
        long idTopico = criarTopicoSobreTestesIntegracao();
        Topico topicoPersistido = repositorioTopicos.findById(idTopico).orElseThrow();
        assertThat(topicoPersistido.getDescricao()).isEqualTo("Quero verificar se o topico fica salvo no banco H2.");
        assertThat(topicoPersistido.getIdAutor()).isNotNull();
        assertThat(topicoPersistido.isAtivo()).isTrue();
    }

    // A busca usa o título único do tópico para não encontrar dúvidas criadas por
    // outros testes.
    @Test
    void listarTopicos_comBusca_deveRetornarTopicoCriado() {
        long idTopico = criarTopicoSobreTestesIntegracao();
        String titulo = repositorioTopicos.findById(idTopico).orElseThrow().getTitulo();
        ResponseEntity<String> resposta = enviarRequisicao(
                HttpMethod.GET, "/api/forum/topicos?busca=" + titulo, tokenProfessor, null);
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(lerCorpoJson(resposta).get(0).path("id").asLong()).isEqualTo(idTopico);
        assertThat(lerCorpoJson(resposta).get(0).path("titulo").asText()).isEqualTo(titulo);
    }

    // A edição feita pelo autor precisa aparecer na resposta e na consulta ao
    // banco.
    @Test
    void editarTopico_comAutor_deveRetornarEPersistirNovoTitulo() {
        long idTopico = criarTopicoSobreTestesIntegracao();
        ResponseEntity<String> resposta = enviarRequisicao(
                HttpMethod.PUT, "/api/forum/topicos/" + idTopico, tokenProfessor,
                Map.of("titulo", "Como consultar o topico salvo no H2?",
                        "descricao", "Preciso consultar o identificador retornado pela API."));
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(lerCorpoJson(resposta).path("titulo").asText()).isEqualTo("Como consultar o topico salvo no H2?");
        assertThat(repositorioTopicos.findById(idTopico).orElseThrow().getTitulo())
                .isEqualTo("Como consultar o topico salvo no H2?");
    }

    // O arquivamento preserva o registro do tópico e altera somente seu estado
    // ativo.
    @Test
    void arquivarTopico_comAdministrador_deveDesativarSemExcluir() {
        long idTopico = criarTopicoSobreTestesIntegracao();
        ResponseEntity<String> resposta = enviarRequisicao(
                HttpMethod.PUT, "/api/forum/topicos/" + idTopico + "/arquivar", tokenAdministrador, null);
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).isNullOrEmpty();
        assertThat(repositorioTopicos.findById(idTopico).orElseThrow().isAtivo()).isFalse();
    }

    // O fechamento mantém o tópico no banco e impede a criação de novas respostas.
    @Test
    void fecharTopico_comAutor_devePersistirFechamento() {
        long idTopico = criarTopicoSobreTestesIntegracao();
        ResponseEntity<String> resposta = enviarRequisicao(
                HttpMethod.PUT, "/api/forum/topicos/" + idTopico + "/fechar", tokenProfessor, null);
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).isNullOrEmpty();
        assertThat(repositorioTopicos.findById(idTopico).orElseThrow().isFechado()).isTrue();
    }

    // A exclusão pelo administrador é lógica: o tópico continua salvo com ativo
    // igual a false.
    @Test
    void apagarTopico_comAdministrador_deveDesativarSemExcluir() {
        long idTopico = criarTopicoSobreTestesIntegracao();
        ResponseEntity<String> resposta = enviarRequisicao(
                HttpMethod.DELETE, "/api/forum/topicos/" + idTopico, tokenAdministrador, null);
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).isNullOrEmpty();
        assertThat(repositorioTopicos.findById(idTopico).orElseThrow().isAtivo()).isFalse();
    }

    // A resposta deve aparecer na listagem do tópico ao qual foi vinculada no
    // banco.
    @Test
    void listarComentarios_comTopicoEResposta_deveRetornarRelacionamento() {
        long idTopico = criarTopicoSobreTestesIntegracao();
        long idComentario = criarComentarioNoTopico(idTopico);
        ResponseEntity<String> resposta = enviarRequisicao(
                HttpMethod.GET, "/api/forum/topicos/" + idTopico + "/comentarios", tokenProfessor, null);
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(lerCorpoJson(resposta).get(0).path("id").asLong()).isEqualTo(idComentario);
        assertThat(lerCorpoJson(resposta).get(0).path("conteudo").asText())
                .isEqualTo("Consulte o topico pelo repositorio depois da requisicao HTTP.");
        assertThat(repositorioComentarios.findById(idComentario).orElseThrow().getTopico().getId()).isEqualTo(idTopico);
    }

    // O comentário criado pela API precisa guardar o mesmo conteúdo na tabela de
    // comentários.
    @Test
    void adicionarComentario_comRespostaSobreH2_devePersistirConteudo() {
        long idTopico = criarTopicoSobreTestesIntegracao();
        long idComentario = criarComentarioNoTopico(idTopico);
        assertThat(repositorioComentarios.findById(idComentario).orElseThrow().getConteudo())
                .isEqualTo("Consulte o topico pelo repositorio depois da requisicao HTTP.");
    }

    // O autor altera seu comentário e o texto atualizado precisa permanecer no
    // banco.
    @Test
    void editarComentario_comAutor_deveRetornarEPersistirConteudo() {
        long idTopico = criarTopicoSobreTestesIntegracao();
        long idComentario = criarComentarioNoTopico(idTopico);
        ResponseEntity<String> resposta = enviarRequisicao(
                HttpMethod.PUT, "/api/forum/comentarios/" + idComentario, tokenProfessor,
                Map.of("conteudo", "Use findById com o identificador retornado pela API."));
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(lerCorpoJson(resposta).path("conteudo").asText())
                .isEqualTo("Use findById com o identificador retornado pela API.");
        assertThat(repositorioComentarios.findById(idComentario).orElseThrow().getConteudo())
                .isEqualTo("Use findById com o identificador retornado pela API.");
    }

    // A exclusão do comentário pelo autor preserva o registro para manter o
    // histórico do fórum.
    @Test
    void apagarComentario_comAutor_deveDesativarSemExcluir() {
        long idTopico = criarTopicoSobreTestesIntegracao();
        long idComentario = criarComentarioNoTopico(idTopico);
        ResponseEntity<String> resposta = enviarRequisicao(
                HttpMethod.DELETE, "/api/forum/comentarios/" + idComentario, tokenProfessor, null);
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).isNullOrEmpty();
        assertThat(repositorioComentarios.findById(idComentario).orElseThrow().isAtivo()).isFalse();
    }

    // A exclusão pela rota do tópico só deve desativar o comentário que pertence a
    // ele.
    @Test
    void apagarComentarioDoTopico_comVinculoCorreto_deveDesativar() {
        long idTopico = criarTopicoSobreTestesIntegracao();
        long idComentario = criarComentarioNoTopico(idTopico);
        ResponseEntity<String> resposta = enviarRequisicao(
                HttpMethod.DELETE, "/api/forum/topicos/" + idTopico + "/comentarios/" + idComentario,
                tokenAdministrador, null);
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).isNullOrEmpty();
        assertThat(repositorioComentarios.findById(idComentario).orElseThrow().isAtivo()).isFalse();
    }

    // A listagem do fórum expõe o nome dos participantes sem devolver suas senhas.
    @Test
    void listarUsuarios_comUsuariosAutenticados_deveRetornarDadosPublicos() {
        ResponseEntity<String> resposta = enviarRequisicao(HttpMethod.GET, "/api/forum/users", tokenProfessor, null);
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(lerCorpoJson(resposta).path("value").isArray()).isTrue();
        assertThat(lerCorpoJson(resposta).path("value").size()).isGreaterThanOrEqualTo(2);
        assertThat(lerCorpoJson(resposta).path("value").get(0).path("nome").asText()).isNotBlank();
        assertThat(resposta.getBody()).doesNotContain("senha");
    }

    // O título vazio deve ser rejeitado antes que um novo tópico seja gravado.
    @Test
    void criarTopico_comTituloVazio_deveRetornar400SemPersistir() {
        long quantidadeTopicosAntes = repositorioTopicos.count();
        ResponseEntity<String> resposta = enviarRequisicao(
                HttpMethod.POST, "/api/forum/topicos", tokenProfessor, Map.of("titulo", "",
                        "descricao", "O titulo da duvida ficou vazio."));
        conferirErro(resposta, 400, "Bad Request");
        assertThat(repositorioTopicos.count()).isEqualTo(quantidadeTopicosAntes);
    }

    // Um identificador sem tópico correspondente deve produzir 404 com a mensagem
    // de recurso ausente.
    @Test
    void listarComentarios_comTopicoInexistente_deveRetornar404() {
        long idTopicoInexistente = Long.MAX_VALUE;
        ResponseEntity<String> resposta = enviarRequisicao(HttpMethod.GET,
                "/api/forum/topicos/" + idTopicoInexistente + "/comentarios", tokenProfessor, null);

        conferirErro(resposta, 404, "Topico nao encontrado");
    }

    // O tópico é fechado antes da tentativa de resposta; a recusa não pode gravar
    // um comentário.
    @Test
    void adicionarComentario_comTopicoFechado_deveRetornar409SemPersistir() {
        long idTopico = criarTopicoSobreTestesIntegracao();
        ResponseEntity<String> respostaFechamento = enviarRequisicao(HttpMethod.PUT,
                "/api/forum/topicos/" + idTopico + "/fechar", tokenProfessor, null);
        assertThat(respostaFechamento.getStatusCode().value()).isEqualTo(200);
        long quantidadeComentariosAntes = repositorioComentarios.count();
        ResponseEntity<String> respostaComentario = enviarRequisicao(HttpMethod.POST,
                "/api/forum/topicos/" + idTopico + "/comentarios", tokenProfessor,
                Map.of("conteudo", "Tentativa de responder depois do fechamento."));

        conferirErro(respostaComentario, 409, "Topico fechado para novos comentarios");
        assertThat(repositorioComentarios.count()).isEqualTo(quantidadeComentariosAntes);
    }

    // A exclusão informa outro tópico na URL; o comentário deve continuar ativo no
    // tópico de origem.
    @Test
    void apagarComentarioDoTopico_comVinculoIncorreto_deveRetornar404SemAlterar() {
        long idTopicoDoComentario = criarTopicoSobreTestesIntegracao();
        long idTopicoSemComentario = criarTopicoSobreTestesIntegracao();
        long idComentario = criarComentarioNoTopico(idTopicoDoComentario);
        ResponseEntity<String> respostaExclusao = enviarRequisicao(HttpMethod.DELETE,
                "/api/forum/topicos/" + idTopicoSemComentario + "/comentarios/" + idComentario,
                tokenAdministrador, null);

        conferirErro(respostaExclusao, 404, "Comentario nao pertence ao topico");
        assertThat(repositorioComentarios.findById(idComentario).orElseThrow().isAtivo()).isTrue();
    }

    // A publicação exige administrador e deve devolver a versão e o conteúdo que
    // foram persistidos.
    @Test
    void publicarPolitica_comAdministrador_deveRetornar201EPersistir() {
        Map<String, String> dadosPolitica = montarPoliticaPrivacidade();
        ResponseEntity<String> resposta = enviarRequisicao(
                HttpMethod.POST, "/api/politicas", tokenAdministrador, dadosPolitica);
        assertThat(resposta.getStatusCode().value()).isEqualTo(201);
        assertThat(lerCorpoJson(resposta).path("versao").asText()).isEqualTo(dadosPolitica.get("versao"));
        assertThat(lerCorpoJson(resposta).path("conteudo").asText()).isEqualTo(dadosPolitica.get("conteudo"));
        assertThat(resposta.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(repositorioPoliticas.findByVersao(dadosPolitica.get("versao"))).isPresent();
    }

    // A política recém-publicada deve estar disponível para leitura sem um token
    // JWT.
    @Test
    void listarPoliticas_comPublicacao_deveRetornarVersaoAtivaSemToken() {
        Map<String, String> dadosPolitica = montarPoliticaPrivacidade();
        ResponseEntity<String> respostaPublicacao = enviarRequisicao(HttpMethod.POST,
                "/api/politicas", tokenAdministrador, dadosPolitica);
        assertThat(respostaPublicacao.getStatusCode().value()).isEqualTo(201);
        ResponseEntity<String> resposta = enviarRequisicao(HttpMethod.GET, "/api/politicas/ativas", null, null);
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(lerCorpoJson(resposta).get(0).path("versao").asText()).isEqualTo(dadosPolitica.get("versao"));
        assertThat(lerCorpoJson(resposta).get(0).path("conteudo").asText()).isEqualTo(dadosPolitica.get("conteudo"));
    }

    // A publicação de políticas exige autenticação mesmo quando os campos enviados
    // são válidos.
    @Test
    void publicarPolitica_semToken_deveRetornar401() {
        ResponseEntity<String> resposta = enviarRequisicao(
                HttpMethod.POST, "/api/politicas", null, montarPoliticaPrivacidade());
        assertThat(resposta.getStatusCode().value()).isEqualTo(401);
    }

    // O professor está autenticado, mas seu perfil não permite publicar políticas
    // de privacidade.
    @Test
    void publicarPolitica_comProfessor_deveRetornar403SemPersistir() {
        Map<String, String> dadosPolitica = montarPoliticaPrivacidade();
        ResponseEntity<String> resposta = enviarRequisicao(HttpMethod.POST,
                "/api/politicas", tokenProfessor, dadosPolitica);

        assertThat(resposta.getStatusCode().value()).isEqualTo(403);
        assertThat(repositorioPoliticas.findByVersao(dadosPolitica.get("versao"))).isEmpty();
    }
}
