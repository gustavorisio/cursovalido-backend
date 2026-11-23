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
public class QuestionarioService {
    private final QuestionarioRepository questionarios;
    private final TentativaQuestionarioRepository tentativas;
    private final CursoRepository cursos;
    private final ModuloRepository modulos;
    private final UsuarioRepository usuarios;
    private final CursoService gestaoCursos;
    private final AdministradorAulasService administracao;
    private final AlunoAulasService aluno;
    private final LogAuditoriaService auditoria;

    @Transactional(readOnly = true)
    public QuestionarioGestaoResponse consultarGestao(Long idCurso, Long idModulo, Long idUsuario) {
        verificarGestor(idCurso, idModulo, idUsuario);
        return questionarios.findByIdCursoAndIdModulo(idCurso, idModulo).map(this::gestao).orElse(null);
    }

    @Transactional
    public QuestionarioGestaoResponse salvar(Long idCurso, Long idModulo, QuestionarioRequest dados, Long idUsuario) {
        verificarGestor(idCurso, idModulo, idUsuario);
        cursos.buscarParaMatricula(idCurso)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Curso nao encontrado"));
        for (var pergunta : dados.perguntas()) {
            if (!pergunta.isExplicacoesValidas()) {
                throw erro(HttpStatus.BAD_REQUEST, "Informe uma explicação para cada alternativa incorreta e uma alternativa correta válida");
            }
        }
        Questionario questionario = questionarios.findByIdCursoAndIdModulo(idCurso, idModulo)
                .orElseGet(Questionario::new);
        if (questionario.getId() != null)
            questionario.setVersao(questionario.getVersao() + 1);
        questionario.setIdCurso(idCurso);
        questionario.setIdModulo(idModulo);
        questionario.setTitulo(dados.titulo().trim());
        questionario.setPublicado(dados.publicado());
        questionario.getPerguntas().clear();
        for (var dadosPergunta : dados.perguntas()) {
            Questao pergunta = new Questao();
            pergunta.setQuestionario(questionario);
            pergunta.setOrdem(questionario.getPerguntas().size());
            pergunta.setEnunciado(dadosPergunta.enunciado().trim());
            pergunta.setAlternativas(new ArrayList<>(dadosPergunta.alternativas().stream().map(String::trim).toList()));
            pergunta.setAlternativaCorreta(dadosPergunta.alternativaCorreta());
            pergunta.setExplicacoes(new ArrayList<>(dadosPergunta.explicacoes().stream().map(String::trim).toList()));
            questionario.getPerguntas().add(pergunta);
        }
        questionarios.saveAndFlush(questionario);
        auditoria.registrar(idUsuario, "EDICAO_QUESTIONARIO", "Questionario " + questionario.getId(), null);
        return gestao(questionario);
    }

    @Transactional(readOnly = true)
    public QuestionarioAlunoResponse consultarAluno(Long idCurso, Long idModulo, Long idAluno) {
        verificarAluno(idCurso, idModulo, idAluno);
        var questionario = questionarios.findByIdCursoAndIdModulo(idCurso, idModulo)
                .filter(this::disponivel).orElse(null);
        if (questionario == null)
            return null;
        var tentativa = ultima(questionario, idAluno);
        return new QuestionarioAlunoResponse(questionario.getId(), questionario.getTitulo(), questionario.getVersao(),
                questionario.getPerguntas().stream()
                        .map(p -> new QuestionarioAlunoResponse.Pergunta(p.getEnunciado(),
                                List.copyOf(p.getAlternativas())))
                        .toList(),
                tentativa == null ? null : resultado(questionario, tentativa));
    }

    @Transactional
    public ResultadoQuestionarioResponse responder(Long idCurso, Long idModulo, RespostasQuestionarioRequest dados,
            Long idAluno) {
        verificarAluno(idCurso, idModulo, idAluno);
        cursos.buscarParaMatricula(idCurso)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Curso nao encontrado"));
        verificarAluno(idCurso, idModulo, idAluno);
        var questionario = questionarios.findByIdCursoAndIdModulo(idCurso, idModulo)
                .filter(this::disponivel).orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Questionario indisponivel"));
        if (dados.versao() != questionario.getVersao())
            throw erro(HttpStatus.CONFLICT, "O questionario foi atualizado. Recarregue antes de responder");
        if (dados.respostas().size() != questionario.getPerguntas().size())
            throw erro(HttpStatus.BAD_REQUEST, "Responda todas as perguntas");
        int acertos = 0;
        for (int i = 0; i < dados.respostas().size(); i++) {
            Questao pergunta = questionario.getPerguntas().get(i);
            int resposta = dados.respostas().get(i);
            if (resposta < 0 || resposta >= pergunta.getAlternativas().size())
                throw erro(HttpStatus.BAD_REQUEST, "Alternativa invalida");
            if (resposta == pergunta.getAlternativaCorreta())
                acertos++;
        }
        TentativaQuestionario tentativa = new TentativaQuestionario();
        tentativa.setIdAluno(idAluno);
        tentativa.setIdQuestionario(questionario.getId());
        tentativa.setVersao(questionario.getVersao());
        tentativa.setAcertos(acertos);
        tentativa.setTotal(dados.respostas().size());
        tentativa.setRespostas(new ArrayList<>(dados.respostas()));
        tentativa.setCriadoEm(LocalDateTime.now());
        tentativas.saveAndFlush(tentativa);
        return resultado(questionario, tentativa);
    }

    @Transactional(readOnly = true)
    public PlanoEstudoResponse plano(Long idCurso, Long idAluno) {
        aluno.buscarCurso(idCurso, idAluno);
        Set<Long> modulosAtivos = new HashSet<>();
        modulos.findByIdCursoAndAtivoTrueOrderByOrdemAscIdAsc(idCurso).forEach(m -> modulosAtivos.add(m.getId()));
        List<PlanoEstudoResponse.Avaliacao> avaliacoes = new ArrayList<>();
        for (var questionario : questionarios.findByIdCursoAndPublicadoTrueOrderByIdAsc(idCurso)) {
            if (!disponivel(questionario)
                    || !modulosAtivos.contains(questionario.getIdModulo()))
                continue;
            var tentativa = ultima(questionario, idAluno);
            avaliacoes.add(new PlanoEstudoResponse.Avaliacao(questionario.getIdModulo(), questionario.getTitulo(),
                    tentativa == null ? null : percentual(tentativa), questionario.getVersao()));
        }
        return new PlanoEstudoResponse(avaliacoes, List.of());
    }

    private void verificarGestor(Long idCurso, Long idModulo, Long idUsuario) {
        var usuario = usuarios.findById(idUsuario)
                .orElseThrow(() -> erro(HttpStatus.FORBIDDEN, "Usuario nao encontrado"));
        if (usuario.getPerfil() == Perfil.ADMINISTRADOR)
            administracao.buscarCurso(idCurso, idUsuario);
        else
            gestaoCursos.buscarAtivo(idCurso, idUsuario);
        verificarModulo(idCurso, idModulo);
    }

    private void verificarAluno(Long idCurso, Long idModulo, Long idAluno) {
        aluno.buscarCurso(idCurso, idAluno);
        verificarModulo(idCurso, idModulo);
    }

    private void verificarModulo(Long idCurso, Long idModulo) {
        if (idModulo == null)
            throw erro(HttpStatus.BAD_REQUEST, "Selecione um módulo para o questionário.");
        Modulo modulo = modulos.findById(idModulo)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Modulo nao encontrado"));
        if (!modulo.isAtivo() || !idCurso.equals(modulo.getIdCurso()))
            throw erro(HttpStatus.NOT_FOUND, "Modulo nao pertence ao curso");
    }

    private boolean disponivel(Questionario questionario) {
        return questionario.isPublicado() && !questionario.getPerguntas().isEmpty();
    }

    private TentativaQuestionario ultima(Questionario q, Long idAluno) {
        return tentativas.findFirstByIdAlunoAndIdQuestionarioAndVersaoOrderByIdDesc(idAluno, q.getId(), q.getVersao())
                .orElse(null);
    }

    private ResultadoQuestionarioResponse resultado(Questionario q, TentativaQuestionario tentativa) {
        boolean concluido = tentativa.getAcertos() == tentativa.getTotal();
        List<ResultadoQuestionarioResponse.Correcao> correcoes = new ArrayList<>();
        for (int i = 0; i < q.getPerguntas().size(); i++) {
            var p = q.getPerguntas().get(i);
            boolean acertou = tentativa.getRespostas().get(i) == p.getAlternativaCorreta();
            correcoes.add(new ResultadoQuestionarioResponse.Correcao(i, acertou,
                    concluido ? p.getAlternativaCorreta() : null, acertou ? null : p.getExplicacoes().get(tentativa.getRespostas().get(i))));
        }
        return new ResultadoQuestionarioResponse(tentativa.getAcertos(), tentativa.getTotal(),
                percentual(tentativa),
                tentativa.getCriadoEm(), correcoes, List.of());
    }

    private int percentual(TentativaQuestionario tentativa) {
        return (int) Math.round(100.0 * tentativa.getAcertos() / tentativa.getTotal());
    }

    private QuestionarioGestaoResponse gestao(Questionario q) {
        return new QuestionarioGestaoResponse(q.getId(), q.getTitulo(), q.isPublicado(), q.getVersao(),
                q.getPerguntas().stream()
                        .map(p -> new QuestionarioRequest.Pergunta(p.getEnunciado(), List.copyOf(p.getAlternativas()),
                                p.getAlternativaCorreta(), List.copyOf(p.getExplicacoes()),
                                null))
                        .toList());
    }

    private ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
