package com.cursovalido.backend.aulas.controller;

import com.cursovalido.backend.aulas.dto.*;
import com.cursovalido.backend.aulas.service.QuestionarioService;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QuestionarioController {
    private final QuestionarioService servico;

    @GetMapping({"/professor/cursos/{idCurso}/diagnostico", "/administracao/cursos/{idCurso}/diagnostico", "/professor/cursos/{idCurso}/modulos/{idModulo}/questionario", "/administracao/cursos/{idCurso}/modulos/{idModulo}/questionario"})
    public QuestionarioGestaoResponse consultarGestao(@PathVariable Long idCurso, @PathVariable(required = false) Long idModulo, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.consultarGestao(idCurso, idModulo, usuario.getId());
    }

    @PutMapping({"/professor/cursos/{idCurso}/diagnostico", "/administracao/cursos/{idCurso}/diagnostico", "/professor/cursos/{idCurso}/modulos/{idModulo}/questionario", "/administracao/cursos/{idCurso}/modulos/{idModulo}/questionario"})
    public QuestionarioGestaoResponse salvar(@PathVariable Long idCurso, @PathVariable(required = false) Long idModulo, @Valid @RequestBody QuestionarioRequest dados, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.salvar(idCurso, idModulo, dados, usuario.getId());
    }

    @GetMapping({"/aluno/cursos/{idCurso}/diagnostico", "/aluno/cursos/{idCurso}/modulos/{idModulo}/questionario"})
    public QuestionarioAlunoResponse consultarAluno(@PathVariable Long idCurso, @PathVariable(required = false) Long idModulo, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.consultarAluno(idCurso, idModulo, usuario.getId());
    }

    @PostMapping({"/aluno/cursos/{idCurso}/diagnostico/respostas", "/aluno/cursos/{idCurso}/modulos/{idModulo}/questionario/respostas"})
    public ResultadoQuestionarioResponse responder(@PathVariable Long idCurso, @PathVariable(required = false) Long idModulo, @Valid @RequestBody RespostasQuestionarioRequest dados, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.responder(idCurso, idModulo, dados, usuario.getId());
    }

    @GetMapping("/aluno/cursos/{idCurso}/plano")
    public PlanoEstudoResponse plano(@PathVariable Long idCurso, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.plano(idCurso, usuario.getId());
    }
}
