package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.cursovalido.backend.aulas.dto.QuestionarioRequest;
import com.cursovalido.backend.aulas.dto.RespostasQuestionarioRequest;
import com.cursovalido.backend.aulas.entity.Questionario;
import com.cursovalido.backend.aulas.entity.Questao;
import com.cursovalido.backend.aulas.repository.CursoRepository;
import com.cursovalido.backend.aulas.repository.ModuloRepository;
import com.cursovalido.backend.aulas.repository.QuestionarioRepository;
import com.cursovalido.backend.aulas.repository.TentativaQuestionarioRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuestionarioServiceTest {

    private final QuestionarioRepository questionarios = mock(QuestionarioRepository.class);
    private final TentativaQuestionarioRepository tentativas = mock(TentativaQuestionarioRepository.class);
    private final CursoRepository cursos = mock(CursoRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final ModuloRepository modulos = mock(ModuloRepository.class);
    @org.junit.jupiter.api.BeforeEach
    void configurarModulo() {
        when(modulos.findById(20L)).thenReturn(Optional.of(AulasTestSupport.modulo()));
        when(modulos.findByIdCursoAndAtivoTrueOrderByOrdemAscIdAsc(10L)).thenReturn(List.of(AulasTestSupport.modulo()));
    }
    private final QuestionarioService service = new QuestionarioService(questionarios, tentativas, cursos,
            modulos, usuarios, mock(CursoService.class), mock(AdministradorAulasService.class),
            mock(AlunoAulasService.class), mock(LogAuditoriaService.class));

    @Test
    void deveSalvarPerguntasComTextoLimpo() {
        when(usuarios.findById(1L)).thenReturn(Optional.of(AulasTestSupport.usuario(Perfil.PROFESSOR)));
        when(cursos.buscarParaMatricula(10L)).thenReturn(Optional.of(AulasTestSupport.curso()));
        var pergunta = new QuestionarioRequest.Pergunta(" Quanto é 2 + 2? ", List.of(" 3 ", " 4 "), 1, List.of(" Soma ", ""), null);

        var resposta = service.salvar(10L, 20L, new QuestionarioRequest(" Avaliação ", true, List.of(pergunta)), 1L);

        assertEquals("Avaliação", resposta.titulo());
        assertEquals("Quanto é 2 + 2?", resposta.perguntas().getFirst().enunciado());
        assertEquals(List.of("3", "4"), resposta.perguntas().getFirst().alternativas());
        verify(questionarios).saveAndFlush(any(Questionario.class));
    }

    @Test
    void deveRecusarQuestionarioForaDeModulo() {
        when(usuarios.findById(1L)).thenReturn(Optional.of(AulasTestSupport.usuario(Perfil.PROFESSOR)));
        var pergunta = new QuestionarioRequest.Pergunta("Questão", List.of("A", "B"), 1, List.of("Explicação", ""), null);
        var dados = new QuestionarioRequest("Avaliação", true, java.util.Collections.nCopies(11, pergunta));

        var erro = assertThrows(ResponseStatusException.class, () -> service.salvar(10L, null, dados, 1L));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verifyNoInteractions(questionarios);
    }

    @Test
    void deveCorrigirEResponderComPercentualDeAcertos() {
        Questionario questionario = questionarioPublicado();
        when(cursos.buscarParaMatricula(10L)).thenReturn(Optional.of(AulasTestSupport.curso()));
        when(questionarios.findByIdCursoAndIdModulo(10L, 20L)).thenReturn(Optional.of(questionario));

        var resultado = service.responder(10L, 20L,
                new RespostasQuestionarioRequest(questionario.getVersao(), List.of(1)), 2L);

        assertEquals(1, resultado.acertos());
        assertEquals(100, resultado.percentual());
        assertTrue(resultado.correcoes().getFirst().acertou());
        verify(tentativas).saveAndFlush(any());
    }

    @Test
    void deveRecusarRespostaParaVersaoAntiga() {
        Questionario questionario = questionarioPublicado();
        when(cursos.buscarParaMatricula(10L)).thenReturn(Optional.of(AulasTestSupport.curso()));
        when(questionarios.findByIdCursoAndIdModulo(10L, 20L)).thenReturn(Optional.of(questionario));

        var erro = assertThrows(ResponseStatusException.class, () -> service.responder(10L, 20L,
                new RespostasQuestionarioRequest(questionario.getVersao() - 1, List.of(1)), 2L));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verifyNoInteractions(tentativas);
    }

    @Test
    void deveExibirPerguntasDisponiveisParaAluno() {
        when(questionarios.findByIdCursoAndIdModulo(10L, 20L)).thenReturn(Optional.of(questionarioPublicado()));

        var resposta = service.consultarAluno(10L, 20L, 2L);

        assertEquals(List.of("3", "4"), resposta.perguntas().getFirst().alternativas());
    }

    @Test
    void deveIncluirAvaliacaoPublicadaNoPlanoDeEstudo() {
        when(questionarios.findByIdCursoAndPublicadoTrueOrderByIdAsc(10L))
                .thenReturn(List.of(questionarioPublicado()));

        var plano = service.plano(10L, 2L);

        assertEquals(1, plano.avaliacoes().size());
        assertEquals("Avaliação", plano.avaliacoes().getFirst().titulo());
    }

    @Test
    void erroRetornaPorcentagemEExplicacaoSemGabarito() {
        var questionario = questionarioPublicado();
        when(cursos.buscarParaMatricula(10L)).thenReturn(Optional.of(AulasTestSupport.curso()));
        when(questionarios.findByIdCursoAndIdModulo(10L, 20L)).thenReturn(Optional.of(questionario));
        var errado = service.responder(10L, 20L, new RespostasQuestionarioRequest(2, List.of(0)), 2L);
        assertEquals(0, errado.acertos());
        assertEquals(0, errado.percentual());
        assertNull(errado.correcoes().getFirst().alternativaCorreta());
        assertFalse(errado.correcoes().getFirst().acertou());
        assertEquals("Some os valores", errado.correcoes().getFirst().explicacao());
        var certo = service.responder(10L, 20L, new RespostasQuestionarioRequest(2, List.of(1)), 2L);
        assertEquals(100, certo.percentual());
        assertNull(certo.correcoes().getFirst().explicacao());
        verify(tentativas, times(2)).saveAndFlush(any());
    }

    @Test
    void consultaAposRecarregarExibePorcentagemSemGabarito() {
        var questionario = questionarioPublicado();
        when(questionarios.findByIdCursoAndIdModulo(10L, 20L)).thenReturn(Optional.of(questionario));
        var tentativa = new com.cursovalido.backend.aulas.entity.TentativaQuestionario();
        tentativa.setAcertos(0);
        tentativa.setTotal(1);
        tentativa.setRespostas(List.of(0));
        when(tentativas.findFirstByIdAlunoAndIdQuestionarioAndVersaoOrderByIdDesc(2L, 5L, 2))
                .thenReturn(Optional.of(tentativa));
        var resultado = service.consultarAluno(10L, 20L, 2L).resultado();
        assertEquals(0, resultado.percentual());
        assertNull(resultado.correcoes().getFirst().alternativaCorreta());
        when(questionarios.findByIdCursoAndPublicadoTrueOrderByIdAsc(10L)).thenReturn(List.of(questionario));
        assertEquals(0, service.plano(10L, 2L).avaliacoes().getFirst().percentual());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({ "0,3,0", "1,3,33", "1,2,50", "3,4,75", "4,4,100" })
    void porcentagemIndependeDeAcertarTodas(int acertos, int total, int esperado) {
        var q = questionarioPublicado();
        var pergunta = q.getPerguntas().getFirst();
        q.getPerguntas().clear();
        for (int i = 0; i < total; i++) q.getPerguntas().add(pergunta);
        when(cursos.buscarParaMatricula(10L)).thenReturn(Optional.of(AulasTestSupport.curso()));
        when(questionarios.findByIdCursoAndIdModulo(10L, 20L)).thenReturn(Optional.of(q));
        var respostas = new java.util.ArrayList<Integer>();
        for (int i = 0; i < total; i++) respostas.add(i < acertos ? 1 : 0);
        assertEquals(esperado, service.responder(10L, 20L, new RespostasQuestionarioRequest(2, respostas), 2L).percentual());
    }

    private Questionario questionarioPublicado() {
        Questao pergunta = new Questao();
        pergunta.setEnunciado("Quanto é 2 + 2?");
        pergunta.setAlternativas(List.of("3", "4"));
        pergunta.setAlternativaCorreta(1);
        pergunta.setExplicacoes(List.of("Some os valores", ""));
        Questionario questionario = new Questionario();
        questionario.setId(5L);
        questionario.setIdModulo(20L);
        questionario.setTitulo("Avaliação");
        questionario.setPublicado(true);
        questionario.setVersao(2);
        questionario.getPerguntas().add(pergunta);
        return questionario;
    }
}
