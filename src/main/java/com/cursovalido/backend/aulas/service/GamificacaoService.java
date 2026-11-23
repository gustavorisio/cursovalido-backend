package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.dto.GamificacaoResponse;
import com.cursovalido.backend.aulas.dto.RecompensaQuestionarioResponse;
import com.cursovalido.backend.aulas.entity.Questionario;
import com.cursovalido.backend.aulas.entity.RecompensaQuestionario;
import com.cursovalido.backend.aulas.repository.RecompensaQuestionarioRepository;
import com.cursovalido.backend.aulas.entity.ConclusaoCurso;
import com.cursovalido.backend.aulas.entity.Curso;
import com.cursovalido.backend.aulas.entity.GamificacaoAluno;
import com.cursovalido.backend.aulas.entity.Matricula;
import com.cursovalido.backend.aulas.repository.ConclusaoCursoRepository;
import com.cursovalido.backend.aulas.repository.CursoRepository;
import com.cursovalido.backend.aulas.repository.GamificacaoAlunoRepository;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class GamificacaoService {
    public static final int XP_POR_QUESTIONARIO = 100;

    private final RecompensaQuestionarioRepository recompensas;
    private final GamificacaoAlunoRepository gamificacoes;
    private final ConclusaoCursoRepository conclusoes;
    private final CursoRepository cursos;
    private final UsuarioRepository usuarios;
    private final LogAuditoriaService auditoria;

    @Transactional(readOnly = true)
    public GamificacaoResponse consultar(Long idAluno) {
        verificarAluno(idAluno);
        return resumo(idAluno);
    }

    @Transactional(readOnly = true)
    public GamificacaoResponse resumo(Long idAluno) {
        var registro = gamificacoes.findById(idAluno);
        int nivel = registro.map(GamificacaoAluno::getNivel).orElse(1);
        long xpNivel = registro.map(GamificacaoAluno::getXpNivel).orElse(0L);
        long xpTotal = registro.map(GamificacaoAluno::getXpTotal).orElse(0L);
        return montar(nivel, xpNivel, xpTotal, conclusoes.countByIdAluno(idAluno), recompensas.countByIdAluno(idAluno));
    }

    // Mantém a conclusão do curso para as métricas, sem conceder XP.
    @Transactional
    public boolean registrarConclusao(Long idAluno, Long idCurso, Long idMatricula) {
        if (conclusoes.existsByIdAlunoAndIdCurso(idAluno, idCurso)
                || cursos.findById(idCurso).filter(Curso::isAtivo).isEmpty()) return false;
        long totalAulas = conclusoes.contarAulasAtivas(idCurso);
        if (totalAulas == 0 || conclusoes.contarAulasConcluidas(idMatricula, idCurso) < totalAulas) return false;
        usuarios.buscarParaAtualizar(idAluno);
        if (conclusoes.existsByIdAlunoAndIdCurso(idAluno, idCurso)) return false;
        conclusoes.save(new ConclusaoCurso(idAluno, idCurso, 0));
        auditoria.registrar(idAluno, "CONCLUSAO_CURSO", "Curso " + idCurso, null);
        return true;
    }

    @Transactional
    public RecompensaQuestionarioResponse registrarAprovacao(Long idAluno, Questionario questionario, int acertos) {
        int total = questionario.getPerguntas().size();
        if (total == 0 || acertos != total || !questionario.isPublicado() || questionario.getIdModulo() == null) return null;
        verificarAluno(idAluno);
        // A trava do aluno e a chave única impedem crédito duplicado entre requisições e instâncias.
        usuarios.buscarParaAtualizar(idAluno).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Aluno não encontrado"));
        if (recompensas.existsByIdAlunoAndIdQuestionario(idAluno, questionario.getId())) return null;
        gamificacoes.criarSeAusente(idAluno);
        GamificacaoAluno registro = gamificacoes.bloquear(idAluno)
                .orElseThrow(() -> new IllegalStateException("Registro de gamificacao indisponivel"));
        int nivelAnterior = registro.getNivel();
        var progresso = ProgressaoNivel.aplicar(registro.getNivel(), registro.getXpNivel(), XP_POR_QUESTIONARIO);
        registro.atualizar(progresso.nivel(), progresso.xpNivel(), registro.getXpTotal() + XP_POR_QUESTIONARIO);
        gamificacoes.save(registro);
        recompensas.saveAndFlush(new RecompensaQuestionario(idAluno, questionario.getId(), XP_POR_QUESTIONARIO));
        auditoria.registrar(idAluno, "APROVACAO_QUESTIONARIO", "Questionario " + questionario.getId()
                + "; XP " + XP_POR_QUESTIONARIO + "; nivel " + registro.getNivel(), null);
        return new RecompensaQuestionarioResponse(questionario.getId(), XP_POR_QUESTIONARIO, nivelAnterior,
                registro.getNivel(), registro.getNivel() > nivelAnterior, resumo(idAluno));
    }

    @Transactional(readOnly = true)
    public List<Matricula> matriculasPendentesDeConclusao() {
        return conclusoes.matriculasSemConclusao();
    }

    private GamificacaoResponse montar(int nivel, long xpNivel, long xpTotal, long cursosConcluidos, long questionariosConcluidos) {
        boolean nivelMaximo = nivel >= ProgressaoNivel.NIVEL_MAXIMO;
        long meta = ProgressaoNivel.meta(nivel);
        long faltam = nivelMaximo ? 0 : Math.max(0, meta - xpNivel);
        int percentual = nivelMaximo ? 100 : (int) Math.min(100, Math.floor(xpNivel * 100.0 / meta));
        return new GamificacaoResponse(nivel, xpNivel, meta, faltam, percentual, xpTotal, cursosConcluidos, questionariosConcluidos,
                nivelMaximo);
    }

    private void verificarAluno(Long idAluno) {
        var aluno = usuarios.findById(idAluno)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Aluno nao encontrado"));
        if (!aluno.isAtivo() || aluno.getPerfil() != Perfil.ALUNO)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso exclusivo de aluno");
    }
}
