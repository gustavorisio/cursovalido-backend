package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.dto.GamificacaoResponse;
import com.cursovalido.backend.aulas.dto.RecompensaCursoResponse;
import com.cursovalido.backend.aulas.entity.ConclusaoCurso;
import com.cursovalido.backend.aulas.entity.Curso;
import com.cursovalido.backend.aulas.entity.GamificacaoAluno;
import com.cursovalido.backend.aulas.entity.Matricula;
import com.cursovalido.backend.aulas.repository.ConclusaoCursoRepository;
import com.cursovalido.backend.aulas.repository.CursoRepository;
import com.cursovalido.backend.aulas.repository.GamificacaoAlunoRepository;
import com.cursovalido.backend.aulas.repository.MatriculaRepository;
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
    public static final int XP_BASE_CURSO = 100;
    public static final int XP_POR_AULA = 10;
    public static final int XP_MAXIMO_CURSO = 500;

    private final GamificacaoAlunoRepository gamificacoes;
    private final ConclusaoCursoRepository conclusoes;
    private final MatriculaRepository matriculas;
    private final CursoRepository cursos;
    private final UsuarioRepository usuarios;
    private final LogAuditoriaService auditoria;

    // Atualiza cursos concluidos que ainda nao renderam XP e devolve a situacao atual.
    @Transactional
    public GamificacaoResponse consultar(Long idAluno) {
        verificarAluno(idAluno);
        for (Matricula matricula : matriculas.findByIdAlunoOrderByIdDesc(idAluno)) {
            registrarConclusao(idAluno, matricula.getIdCurso(), matricula.getId());
        }
        return resumo(idAluno);
    }

    @Transactional(readOnly = true)
    public GamificacaoResponse resumo(Long idAluno) {
        var registro = gamificacoes.findById(idAluno);
        int nivel = registro.map(GamificacaoAluno::getNivel).orElse(1);
        long xpNivel = registro.map(GamificacaoAluno::getXpNivel).orElse(0L);
        long xpTotal = registro.map(GamificacaoAluno::getXpTotal).orElse(0L);
        return montar(nivel, xpNivel, xpTotal, conclusoes.countByIdAluno(idAluno));
    }

    // Concede XP uma unica vez quando todas as aulas ativas do curso foram concluidas.
    @Transactional
    public RecompensaCursoResponse registrarConclusao(Long idAluno, Long idCurso, Long idMatricula) {
        if (conclusoes.existsByIdAlunoAndIdCurso(idAluno, idCurso))
            return null;
        if (cursos.findById(idCurso).filter(Curso::isAtivo).isEmpty())
            return null;
        long totalAulas = conclusoes.contarAulasAtivas(idCurso);
        if (totalAulas == 0 || conclusoes.contarAulasConcluidas(idMatricula, idCurso) < totalAulas)
            return null;

        // Serializa a criacao da linha de XP por aluno sem depender de sintaxe exclusiva do PostgreSQL.
        usuarios.buscarParaAtualizar(idAluno);
        gamificacoes.criarSeAusente(idAluno);
        GamificacaoAluno registro = gamificacoes.bloquear(idAluno)
                .orElseThrow(() -> new IllegalStateException("Registro de gamificacao indisponivel"));
        if (conclusoes.existsByIdAlunoAndIdCurso(idAluno, idCurso))
            return null;

        int xpGanho = calcularXpCurso(totalAulas);
        int nivelAnterior = registro.getNivel();
        var progresso = ProgressaoNivel.aplicar(registro.getNivel(), registro.getXpNivel(), xpGanho);
        registro.atualizar(progresso.nivel(), progresso.xpNivel(), registro.getXpTotal() + xpGanho);
        gamificacoes.save(registro);
        conclusoes.save(new ConclusaoCurso(idAluno, idCurso, xpGanho));
        auditoria.registrar(idAluno, "CONCLUSAO_CURSO", "Curso " + idCurso + "; XP " + xpGanho
                + "; nivel " + registro.getNivel(), null);

        var situacao = montar(registro.getNivel(), registro.getXpNivel(), registro.getXpTotal(),
                conclusoes.countByIdAluno(idAluno));
        return new RecompensaCursoResponse(idCurso, xpGanho, nivelAnterior, registro.getNivel(),
                registro.getNivel() > nivelAnterior, situacao);
    }

    @Transactional(readOnly = true)
    public List<Matricula> matriculasPendentesDeConclusao() {
        return conclusoes.matriculasSemConclusao();
    }

    public static int calcularXpCurso(long totalAulas) {
        long xp = XP_BASE_CURSO + XP_POR_AULA * Math.max(0, totalAulas);
        return (int) Math.min(xp, XP_MAXIMO_CURSO);
    }

    private GamificacaoResponse montar(int nivel, long xpNivel, long xpTotal, long cursosConcluidos) {
        boolean nivelMaximo = nivel >= ProgressaoNivel.NIVEL_MAXIMO;
        long meta = ProgressaoNivel.meta(nivel);
        long faltam = nivelMaximo ? 0 : Math.max(0, meta - xpNivel);
        int percentual = nivelMaximo ? 100 : (int) Math.min(100, Math.floor(xpNivel * 100.0 / meta));
        return new GamificacaoResponse(nivel, xpNivel, meta, faltam, percentual, xpTotal, cursosConcluidos,
                nivelMaximo);
    }

    private void verificarAluno(Long idAluno) {
        var aluno = usuarios.findById(idAluno)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Aluno nao encontrado"));
        if (!aluno.isAtivo() || aluno.getPerfil() != Perfil.ALUNO)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso exclusivo de aluno");
    }
}
