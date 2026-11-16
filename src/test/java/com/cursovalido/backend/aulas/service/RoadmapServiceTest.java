package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.cursovalido.backend.aulas.dto.CaminhosRequest;
import com.cursovalido.backend.aulas.dto.CaminhosRequest.Opcao;
import com.cursovalido.backend.aulas.dto.CaminhosRequest.Pergunta;
import com.cursovalido.backend.aulas.dto.CaminhosRequest.Resultado;
import com.cursovalido.backend.aulas.dto.RespostasCaminhosRequest;
import com.cursovalido.backend.aulas.dto.RespostasCaminhosRequest.Escolha;
import com.cursovalido.backend.aulas.dto.SecaoCaminhosRequest;
import com.cursovalido.backend.aulas.entity.QuestionarioCaminhos;
import com.cursovalido.backend.aulas.entity.RespostaAlunoCaminhos;
import com.cursovalido.backend.aulas.entity.SecaoCaminhos;
import com.cursovalido.backend.aulas.repository.QuestionarioCaminhosRepository;
import com.cursovalido.backend.aulas.repository.RespostaAlunoCaminhosRepository;
import com.cursovalido.backend.aulas.repository.SecaoCaminhosRepository;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoadmapServiceTest {

    private final QuestionarioCaminhosRepository questionarios = mock(QuestionarioCaminhosRepository.class);
    private final SecaoCaminhosRepository secoes = mock(SecaoCaminhosRepository.class);
    private final RespostaAlunoCaminhosRepository respostas = mock(RespostaAlunoCaminhosRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final RoadmapService service = new RoadmapService(
            questionarios, secoes, respostas, usuarios, mock(LogAuditoriaService.class));

    @Test
    void deveSalvarQuestionarioComPerguntasEResultados() {
        cadastrarAdministrador();

        var questionario = service.salvarQuestionario(null, questionarioJava(true, 0), 1L);

        assertEquals("Trilha Java", questionario.titulo());
        assertEquals("Qual seu nível?", questionario.perguntas().getFirst().enunciado());
        assertEquals(List.of("Variáveis"), questionario.resultados().getFirst().temasPesquisa());
        verify(questionarios).saveAndFlush(any(QuestionarioCaminhos.class));
    }

    @Test
    void deveRecusarEdicaoDeVersaoAntiga() {
        QuestionarioCaminhos questionario = salvarQuestionario(true);
        when(questionarios.buscarParaAlterar(5L)).thenReturn(Optional.of(questionario));

        var erro = assertThrows(ResponseStatusException.class,
                () -> service.salvarQuestionario(5L, questionarioJava(true, questionario.getVersao() - 1), 1L));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    @Test
    void deveOcultarResultadosNaConsultaPublica() {
        QuestionarioCaminhos questionario = salvarQuestionario(true);
        when(questionarios.findById(5L)).thenReturn(Optional.of(questionario));

        var resposta = service.consultarQuestionario(5L, null);

        assertEquals("inicio", resposta.perguntaInicial());
        assertTrue(resposta.resultados().isEmpty());
    }

    @Test
    void deveCalcularResultadoEGuardarRespostaDoAluno() {
        QuestionarioCaminhos questionario = salvarQuestionario(true);
        when(questionarios.buscarParaAlterar(5L)).thenReturn(Optional.of(questionario));
        when(usuarios.findById(2L)).thenReturn(Optional.of(AulasTestSupport.usuario(Perfil.ALUNO)));
        var dados = new RespostasCaminhosRequest(questionario.getVersao(), List.of(new Escolha("inicio", 0)));

        var resultado = service.calcular(5L, dados, 2L);

        assertEquals("java", resultado.resultado().chave());
        var captura = ArgumentCaptor.forClass(RespostaAlunoCaminhos.class);
        verify(respostas).save(captura.capture());
        assertEquals(2L, captura.getValue().getIdAluno());
        assertEquals("java", captura.getValue().getResultado());
    }

    @Test
    void deveRecusarRespostaForaDoCaminho() {
        QuestionarioCaminhos questionario = salvarQuestionario(true);
        when(questionarios.buscarParaAlterar(5L)).thenReturn(Optional.of(questionario));
        var dados = new RespostasCaminhosRequest(questionario.getVersao(), List.of(new Escolha("outra", 0)));

        var erro = assertThrows(ResponseStatusException.class, () -> service.calcular(5L, dados, null));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verifyNoInteractions(respostas);
    }

    @Test
    void deveDespublicarQuestionarioAoExcluir() {
        QuestionarioCaminhos questionario = salvarQuestionario(true);
        when(questionarios.buscarParaAlterar(5L)).thenReturn(Optional.of(questionario));

        service.excluirQuestionario(5L, 1L);

        assertFalse(questionario.isPublicado());
        assertFalse(questionario.isAtivo());
    }

    @Test
    void deveSalvarSecaoComTituloLimpoEOrdem() {
        cadastrarAdministrador();

        var secao = service.salvarSecao(null, new SecaoCaminhosRequest(" Dicas ", " Leia com calma ", 2, true), 1L);

        assertEquals("Dicas", secao.getTitulo());
        assertEquals(2, secao.getOrdem());
        verify(secoes).save(secao);
    }

    @Test
    void deveExcluirSecaoExistente() {
        cadastrarAdministrador();
        SecaoCaminhos secao = new SecaoCaminhos();
        when(secoes.findById(5L)).thenReturn(Optional.of(secao));

        service.excluirSecao(5L, 1L);

        verify(secoes).delete(secao);
    }

    private void cadastrarAdministrador() {
        when(usuarios.findById(1L)).thenReturn(Optional.of(AulasTestSupport.usuario(Perfil.ADMINISTRADOR)));
    }

    private QuestionarioCaminhos salvarQuestionario(boolean publicado) {
        cadastrarAdministrador();
        service.salvarQuestionario(null, questionarioJava(publicado, 0), 1L);
        var captura = ArgumentCaptor.forClass(QuestionarioCaminhos.class);
        verify(questionarios).saveAndFlush(captura.capture());
        return captura.getValue();
    }

    private CaminhosRequest questionarioJava(boolean publicado, int versao) {
        var resultado = new Resultado("java", "Java", "Aprenda a sintaxe", List.of(" Variáveis "));
        var pergunta = new Pergunta("inicio", " Qual seu nível? ",
                List.of(new Opcao("Iniciante", null, "java"), new Opcao("Intermediário", null, "java")));
        return new CaminhosRequest(" Trilha Java ", "", publicado, versao, "inicio",
                List.of(pergunta), List.of(resultado));
    }
}
