package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.dto.*;
import com.cursovalido.backend.aulas.entity.*;
import com.cursovalido.backend.aulas.repository.*;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RoadmapService {
    private final QuestionarioCaminhosRepository questionarios;
    private final SecaoCaminhosRepository secoes;
    private final RespostaAlunoCaminhosRepository respostas;
    private final UsuarioRepository usuarios;
    private final LogAuditoriaService auditoria;

    public PaginaCaminhosResponse consultar() {
        return new PaginaCaminhosResponse(questionarios.findByAtivoTrueAndPublicadoTrueOrderByIdDesc().stream().map(this::resumo).toList(),
                secoes.findByPublicadoTrueOrderByOrdemAscIdAsc());
    }

    public PaginaCaminhosResponse consultarAdministracao(Long idUsuario) {
        verificarPerfil(idUsuario, Perfil.ADMINISTRADOR);
        return new PaginaCaminhosResponse(questionarios.findByAtivoTrueOrderByIdDesc().stream().map(this::resumo).toList(),
                secoes.findAllByOrderByOrdemAscIdAsc());
    }

    @Transactional(readOnly = true)
    public CaminhosResponse consultarQuestionario(Long id, Long idAdministrador) {
        if (idAdministrador != null) verificarPerfil(idAdministrador, Perfil.ADMINISTRADOR);
        var questionario = buscar(id);
        if (idAdministrador == null && !questionario.isPublicado()) throw erro(HttpStatus.NOT_FOUND, "Questionario nao publicado");
        return converter(questionario, idAdministrador != null);
    }

    @Transactional
    public CaminhosResponse salvarQuestionario(Long id, CaminhosRequest dados, Long idAdministrador) {
        verificarPerfil(idAdministrador, Perfil.ADMINISTRADOR);
        ValidacaoCaminhos.validar(dados);
        var questionario = id == null ? new QuestionarioCaminhos() : buscarParaAlterar(id);
        if (id != null && dados.versao() != questionario.getVersao()) throw erro(HttpStatus.CONFLICT, "O questionario foi alterado. Recarregue antes de salvar");
        if (id != null) questionario.setVersao(questionario.getVersao() + 1);
        questionario.setTitulo(dados.titulo().trim());
        questionario.setDescricao(dados.descricao().trim());
        questionario.setPublicado(dados.publicado());
        questionario.setPerguntaInicial(dados.perguntaInicial());
        questionario.getPerguntas().clear();
        for (var dadosPergunta : dados.perguntas()) {
            var pergunta = new PerguntaCaminhos();
            pergunta.setQuestionario(questionario);
            pergunta.setChave(dadosPergunta.chave());
            pergunta.setEnunciado(dadosPergunta.enunciado().trim());
            pergunta.setOrdem(questionario.getPerguntas().size());
            for (var dadosOpcao : dadosPergunta.opcoes()) {
                var opcao = new OpcaoCaminhos();
                opcao.setTexto(dadosOpcao.texto().trim());
                opcao.setProximaPergunta(dadosOpcao.proximaPergunta());
                opcao.setResultado(dadosOpcao.resultado());
                pergunta.getOpcoes().add(opcao);
            }
            questionario.getPerguntas().add(pergunta);
        }
        questionario.getResultados().clear();
        for (var dadosResultado : dados.resultados()) {
            var resultado = new ResultadoCaminhos();
            resultado.setQuestionario(questionario);
            resultado.setChave(dadosResultado.chave());
            resultado.setTitulo(dadosResultado.titulo().trim());
            resultado.setOrientacao(dadosResultado.orientacao().trim());
            resultado.setTemasPesquisa(new ArrayList<>(dadosResultado.temasPesquisa().stream().map(String::trim).toList()));
            resultado.setOrdem(questionario.getResultados().size());
            questionario.getResultados().add(resultado);
        }
        questionarios.saveAndFlush(questionario);
        auditoria.registrar(idAdministrador, "EDICAO_CAMINHOS", "Questionario " + questionario.getId(), null);
        return converter(questionario, true);
    }

    @Transactional
    public void excluirQuestionario(Long id, Long idAdministrador) {
        verificarPerfil(idAdministrador, Perfil.ADMINISTRADOR);
        var questionario = buscarParaAlterar(id);
        questionario.setAtivo(false);
        questionario.setPublicado(false);
        auditoria.registrar(idAdministrador, "EXCLUSAO_CAMINHOS", "Questionario " + id, null);
    }

    @Transactional
    public SecaoCaminhos salvarSecao(Long id, SecaoCaminhosRequest dados, Long idAdministrador) {
        verificarPerfil(idAdministrador, Perfil.ADMINISTRADOR);
        var secao = id == null ? new SecaoCaminhos() : secoes.findById(id).orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Secao nao encontrada"));
        secao.setTitulo(dados.titulo().trim());
        secao.setConteudo(dados.conteudo().trim());
        secao.setOrdem(dados.ordem());
        secao.setPublicado(dados.publicado());
        secoes.save(secao);
        auditoria.registrar(idAdministrador, "EDICAO_SECAO_CAMINHOS", "Secao " + secao.getId(), null);
        return secao;
    }

    @Transactional
    public void excluirSecao(Long id, Long idAdministrador) {
        verificarPerfil(idAdministrador, Perfil.ADMINISTRADOR);
        var secao = secoes.findById(id).orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Secao nao encontrada"));
        secoes.delete(secao);
        auditoria.registrar(idAdministrador, "EXCLUSAO_SECAO_CAMINHOS", "Secao " + id, null);
    }

    @Transactional
    public ResultadoCaminhosResponse calcular(Long id, RespostasCaminhosRequest dados, Long idAluno) {
        if (idAluno != null) verificarPerfil(idAluno, Perfil.ALUNO);
        var questionario = buscarParaAlterar(id);
        if (!questionario.isPublicado()) throw erro(HttpStatus.NOT_FOUND, "Questionario nao publicado");
        if (dados.versao() != questionario.getVersao()) throw erro(HttpStatus.CONFLICT, "O questionario foi atualizado. Recarregue para recomecar");
        var resultado = avaliar(questionario, dados.respostas());
        LocalDateTime atualizadoEm = null;
        if (idAluno != null) {
            var resposta = respostas.findByIdAlunoAndIdQuestionario(idAluno, id).orElseGet(RespostaAlunoCaminhos::new);
            resposta.setIdAluno(idAluno);
            resposta.setIdQuestionario(id);
            resposta.setVersao(questionario.getVersao());
            resposta.setResultado(resultado.getChave());
            resposta.setAtualizadoEm(LocalDateTime.now());
            resposta.getRespostas().clear();
            for (var dadosEscolha : dados.respostas()) {
                var escolha = new EscolhaCaminhos();
                escolha.setPergunta(dadosEscolha.pergunta());
                escolha.setOpcao(dadosEscolha.opcao());
                resposta.getRespostas().add(escolha);
            }
            respostas.save(resposta);
            atualizadoEm = resposta.getAtualizadoEm();
        }
        return new ResultadoCaminhosResponse(questionario.getVersao(), dados.respostas(), converterResultado(resultado), atualizadoEm);
    }

    @Transactional(readOnly = true)
    public ResultadoCaminhosResponse consultarResultado(Long id, Long idAluno) {
        verificarPerfil(idAluno, Perfil.ALUNO);
        var questionario = buscar(id);
        if (!questionario.isPublicado()) throw erro(HttpStatus.NOT_FOUND, "Questionario nao publicado");
        var resposta = respostas.findByIdAlunoAndIdQuestionario(idAluno, id).orElse(null);
        if (resposta == null || resposta.getVersao() != questionario.getVersao()) return null;
        var resultado = questionario.getResultados().stream().filter(r -> r.getChave().equals(resposta.getResultado())).findFirst().orElseThrow();
        return new ResultadoCaminhosResponse(resposta.getVersao(), resposta.getRespostas().stream()
                .map(e -> new RespostasCaminhosRequest.Escolha(e.getPergunta(), e.getOpcao())).toList(), converterResultado(resultado), resposta.getAtualizadoEm());
    }

    private ResultadoCaminhos avaliar(QuestionarioCaminhos questionario, List<RespostasCaminhosRequest.Escolha> escolhas) {
        Map<String, PerguntaCaminhos> perguntas = new HashMap<>();
        questionario.getPerguntas().forEach(p -> perguntas.put(p.getChave(), p));
        String atual = questionario.getPerguntaInicial();
        for (int i = 0; i < escolhas.size(); i++) {
            var escolha = escolhas.get(i);
            var pergunta = perguntas.get(atual);
            if (pergunta == null || !atual.equals(escolha.pergunta()) || escolha.opcao() >= pergunta.getOpcoes().size()) {
                throw erro(HttpStatus.BAD_REQUEST, "As respostas nao seguem o caminho do questionario");
            }
            var opcao = pergunta.getOpcoes().get(escolha.opcao());
            if (opcao.getResultado() != null) {
                if (i != escolhas.size() - 1) throw erro(HttpStatus.BAD_REQUEST, "Existem respostas depois do resultado final");
                return questionario.getResultados().stream().filter(r -> r.getChave().equals(opcao.getResultado())).findFirst().orElseThrow();
            }
            atual = opcao.getProximaPergunta();
        }
        throw erro(HttpStatus.BAD_REQUEST, "Responda ate chegar a um resultado antes de finalizar");
    }

    private QuestionarioCaminhos buscar(Long id) {
        return questionarios.findById(id).filter(QuestionarioCaminhos::isAtivo).orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Questionario nao encontrado"));
    }

    private QuestionarioCaminhos buscarParaAlterar(Long id) {
        return questionarios.buscarParaAlterar(id).filter(QuestionarioCaminhos::isAtivo).orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Questionario nao encontrado"));
    }

    private PaginaCaminhosResponse.Resumo resumo(QuestionarioCaminhos q) {
        return new PaginaCaminhosResponse.Resumo(q.getId(), q.getTitulo(), q.getDescricao(), q.isPublicado(), q.getVersao());
    }

    private CaminhosResponse converter(QuestionarioCaminhos q, boolean gestao) {
        return new CaminhosResponse(q.getId(), q.getTitulo(), q.getDescricao(), q.isPublicado(), q.getVersao(), q.getPerguntaInicial(),
                q.getPerguntas().stream().map(p -> new CaminhosRequest.Pergunta(p.getChave(), p.getEnunciado(), p.getOpcoes().stream()
                        .map(o -> new CaminhosRequest.Opcao(o.getTexto(), o.getProximaPergunta(), o.getResultado())).toList())).toList(),
                gestao ? q.getResultados().stream().map(this::converterResultado).toList() : List.of());
    }

    private CaminhosRequest.Resultado converterResultado(ResultadoCaminhos r) {
        return new CaminhosRequest.Resultado(r.getChave(), r.getTitulo(), r.getOrientacao(), List.copyOf(r.getTemasPesquisa()));
    }

    private void verificarPerfil(Long id, Perfil perfil) {
        var usuario = usuarios.findById(id).orElseThrow(() -> erro(HttpStatus.FORBIDDEN, "Usuario nao encontrado"));
        if (!usuario.isAtivo() || usuario.getPerfil() != perfil || (perfil == Perfil.ALUNO && (!usuario.isEmailConfirmado() || !Boolean.TRUE.equals(usuario.getAceitouTermosLgpd())))) {
            throw erro(HttpStatus.FORBIDDEN, "Perfil sem permissao para esta operacao");
        }
    }

    private ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
