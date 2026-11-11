package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.dto.CursoRequest;
import com.cursovalido.backend.aulas.entity.Curso;
import com.cursovalido.backend.aulas.repository.CursoRepository;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CursoService {
    private final CursoRepository repositorio;
    private final UsuarioRepository usuarios;
    private final LogAuditoriaService auditoria;

    public List<Curso> listarPublicos() {
        return repositorio.findByAtivoTrueOrderByIdDesc();
    }

    public List<Curso> listar(Long idProfessor) {
        verificarProfessor(idProfessor);
        return repositorio.findByIdProfessorAndAtivoTrueOrderByIdDesc(idProfessor);
    }

    public Curso buscarAtivo(Long id, Long idProfessor) {
        verificarProfessor(idProfessor);
        Curso curso = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso nao encontrado"));
        if (!curso.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso inativo");
        }
        if (!idProfessor.equals(curso.getIdProfessor())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente o professor responsavel pode acessar o curso");
        }
        return curso;
    }

    @Transactional
    public Curso criar(CursoRequest dados, Long idProfessor) {
        verificarProfessor(idProfessor);
        Curso curso = new Curso();
        curso.setTitulo(dados.titulo().trim());
        curso.setDescricao(dados.descricao().trim());
        curso.setIdProfessor(idProfessor);
        curso.setCriadoEm(LocalDateTime.now());
        repositorio.save(curso);
        auditoria.registrar(idProfessor, "CADASTRO_CURSO", "Curso " + curso.getId(), null);
        return curso;
    }

    @Transactional
    public Curso editar(Long id, CursoRequest dados, Long idProfessor) {
        Curso curso = buscarAtivo(id, idProfessor);
        curso.setTitulo(dados.titulo().trim());
        curso.setDescricao(dados.descricao().trim());
        repositorio.save(curso);
        auditoria.registrar(idProfessor, "EDICAO_CURSO", "Curso " + id, null);
        return curso;
    }

    @Transactional
    public void apagar(Long id, Long idProfessor) {
        Curso curso = buscarAtivo(id, idProfessor);
        curso.setAtivo(false);
        repositorio.save(curso);
        auditoria.registrar(idProfessor, "EXCLUSAO_CURSO", "Curso " + id, null);
    }

    private void verificarProfessor(Long idProfessor) {
        var professor = usuarios.findById(idProfessor)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Professor nao encontrado"));
        if (!professor.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso exclusivo de professor ativo");
        }
        if (professor.getPerfil() != Perfil.PROFESSOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso exclusivo de professor ativo");
        }
    }
}
