package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.dto.ProgressoAulaRequest;
import com.cursovalido.backend.aulas.entity.ProgressoAula;
import com.cursovalido.backend.aulas.repository.*;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.cursovalido.backend.aulas.service.AulasTestSupport.*;

@ExtendWith(MockitoExtension.class)
class ProgressoAulaServiceTest {
    @Mock
    private AlunoAulasService alunoAulas;
    @Mock
    private ModuloRepository modulos;
    @Mock
    private MatriculaRepository matriculas;
    @Mock
    private ProgressoAulaRepository progressos;
    @Mock
    private LogAuditoriaService auditoria;
    @InjectMocks
    private ProgressoAulaService servico;

    private void autorizar() {
        when(alunoAulas.buscarAula(30L, 1L)).thenReturn(aula());
        when(modulos.findById(20L)).thenReturn(Optional.of(modulo()));
        when(matriculas.findByIdAlunoAndIdCurso(1L, 10L)).thenReturn(Optional.of(matricula()));
    }

    private void permitirEscrita() {
        autorizar();
        when(matriculas.bloquearPorId(40L)).thenReturn(Optional.of(matricula()));
    }

    @Test
    void consultaInicialNaoGravaRegistro() {
        autorizar();
        var resultado = servico.buscar(30L, 1L);
        assertThat(resultado.getId()).isNull();
        assertThat(resultado.getIdAula()).isEqualTo(30L);
        assertThat(resultado.getIdMatricula()).isEqualTo(40L);
        assertThat(resultado.getPosicaoSegundos()).isZero();
        assertThat(resultado.isConcluida()).isFalse();
        assertThat(resultado.getAtualizadoEm()).isNull();
        verify(progressos, never()).save(any());
    }

    @Test
    void retornarProgressoExistente() {
        autorizar();
        var progresso = new ProgressoAula();
        progresso.setPosicaoSegundos(120);
        when(progressos.findByIdMatriculaAndIdAula(40L, 30L)).thenReturn(Optional.of(progresso));
        assertThat(servico.buscar(30L, 1L)).isSameAs(progresso);
    }

    @Test
    void salvarPosicaoCriaRegistroComData() {
        permitirEscrita();
        when(progressos.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        var resultado = servico.salvarPosicao(30L, new ProgressoAulaRequest(123L), 1L);
        assertThat(resultado.getPosicaoSegundos()).isEqualTo(123);
        assertThat(resultado.getAtualizadoEm()).isNotNull();
        assertThat(resultado.isConcluida()).isFalse();
        verifyNoInteractions(auditoria);
    }

    @Test
    void voltarVideoPreservaConclusaoAnterior() {
        permitirEscrita();
        var progresso = new ProgressoAula();
        var data = LocalDateTime.of(2026, 1, 1, 12, 0);
        progresso.setConcluida(true);
        progresso.setConcluidaEm(data);
        progresso.setPosicaoSegundos(200);
        when(progressos.findByIdMatriculaAndIdAula(40L, 30L)).thenReturn(Optional.of(progresso));
        when(progressos.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        var resultado = servico.salvarPosicao(30L, new ProgressoAulaRequest(20L), 1L);
        assertThat(resultado.getPosicaoSegundos()).isEqualTo(20);
        assertThat(resultado.isConcluida()).isTrue();
        assertThat(resultado.getConcluidaEm()).isEqualTo(data);
    }

    @Test
    void concluirSemExigirPosicaoMinima() {
        permitirEscrita();
        var resultado = servico.concluir(30L, 1L);
        assertThat(resultado.isConcluida()).isTrue();
        assertThat(resultado.getPosicaoSegundos()).isZero();
        assertThat(resultado.getConcluidaEm()).isEqualTo(resultado.getAtualizadoEm()).isNotNull();
        verify(progressos).save(resultado);
        verify(auditoria).registrar(1L, "CONCLUSAO_AULA", "Aula 30", null);
    }

    @Test
    void repetirConclusaoNaoAlteraDataNemAudita() {
        permitirEscrita();
        var progresso = new ProgressoAula();
        var data = LocalDateTime.of(2026, 1, 1, 12, 0);
        progresso.setConcluida(true);
        progresso.setConcluidaEm(data);
        when(progressos.findByIdMatriculaAndIdAula(40L, 30L)).thenReturn(Optional.of(progresso));
        assertThat(servico.concluir(30L, 1L).getConcluidaEm()).isEqualTo(data);
        verify(progressos, never()).save(any());
        verifyNoInteractions(auditoria);
    }

    @Test
    void acessoNegadoNaoConsultaProgresso() {
        when(alunoAulas.buscarAula(30L, 1L)).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        erro(() -> servico.buscar(30L, 1L), 403);
        verifyNoInteractions(modulos, matriculas, progressos);
    }

    @Test
    void moduloRemovidoRetorna404() {
        when(alunoAulas.buscarAula(30L, 1L)).thenReturn(aula());
        erro(() -> servico.buscar(30L, 1L), 404);
    }

    @Test
    void matriculaRemovidaRetorna403() {
        when(alunoAulas.buscarAula(30L, 1L)).thenReturn(aula());
        when(modulos.findById(20L)).thenReturn(Optional.of(modulo()));
        erro(() -> servico.buscar(30L, 1L), 403);
        verifyNoInteractions(progressos);
    }

    @Test
    void matriculaAusenteAoBloquearImpedeGravacao() {
        autorizar();
        erro(() -> servico.concluir(30L, 1L), 403);
        verifyNoInteractions(progressos, auditoria);
    }
}
