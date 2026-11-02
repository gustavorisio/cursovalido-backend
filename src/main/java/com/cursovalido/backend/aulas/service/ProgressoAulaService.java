package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.dto.ProgressoAulaRequest;
import com.cursovalido.backend.aulas.entity.Matricula;
import com.cursovalido.backend.aulas.entity.ProgressoAula;
import com.cursovalido.backend.aulas.repository.MatriculaRepository;
import com.cursovalido.backend.aulas.repository.ModuloRepository;
import com.cursovalido.backend.aulas.repository.ProgressoAulaRepository;
import com.cursovalido.backend.authentication.service.LogAuditoriaServico;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProgressoAulaService {
    private final AlunoAulasService alunoAulas;
    private final ModuloRepository modulos;
    private final MatriculaRepository matriculas;
    private final ProgressoAulaRepository progressos;
    private final LogAuditoriaServico auditoria;

    public ProgressoAula buscar(Long idAula, Long idAluno) {
        Matricula matricula = buscarMatriculaAutorizada(idAula, idAluno);
        return buscarOuPreparar(idAula, matricula.getId());
    }

    @Transactional
    public ProgressoAula salvarPosicao(Long idAula, ProgressoAulaRequest dados, Long idAluno) {
        Matricula matricula = buscarMatriculaAutorizada(idAula, idAluno);
        bloquearMatricula(matricula.getId());
        ProgressoAula progresso = buscarOuPreparar(idAula, matricula.getId());
        progresso.setPosicaoSegundos(dados.posicaoSegundos());
        progresso.setAtualizadoEm(LocalDateTime.now());
        return progressos.save(progresso);
    }

    @Transactional
    public ProgressoAula concluir(Long idAula, Long idAluno) {
        Matricula matricula = buscarMatriculaAutorizada(idAula, idAluno);
        bloquearMatricula(matricula.getId());
        ProgressoAula progresso = buscarOuPreparar(idAula, matricula.getId());
        if (progresso.isConcluida()) {
            return progresso;
        }
        LocalDateTime agora = LocalDateTime.now();
        progresso.setConcluida(true);
        progresso.setConcluidaEm(agora);
        progresso.setAtualizadoEm(agora);
        progressos.save(progresso);
        auditoria.registrar(idAluno, "CONCLUSAO_AULA", "Aula " + idAula, null);
        return progresso;
    }

    private Matricula buscarMatriculaAutorizada(Long idAula, Long idAluno) {
        var aula = alunoAulas.buscarAula(idAula, idAluno);
        var modulo = modulos.findById(aula.getIdModulo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modulo nao encontrado"));
        return matriculas.findByIdAlunoAndIdCurso(idAluno, modulo.getIdCurso())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Matricula nao encontrada"));
    }

    private void bloquearMatricula(Long idMatricula) {
        // Evita criar dois progressos quando chegam atualizacoes simultaneas.
        matriculas.bloquearPorId(idMatricula)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Matricula nao encontrada"));
    }

    private ProgressoAula buscarOuPreparar(Long idAula, Long idMatricula) {
        var existente = progressos.findByIdMatriculaAndIdAula(idMatricula, idAula);
        if (existente.isPresent()) {
            return existente.get();
        }
        ProgressoAula progresso = new ProgressoAula();
        progresso.setIdAula(idAula);
        progresso.setIdMatricula(idMatricula);
        return progresso;
    }
}
