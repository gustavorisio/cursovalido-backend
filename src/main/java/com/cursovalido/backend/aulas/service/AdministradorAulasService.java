package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.entity.*;
import com.cursovalido.backend.aulas.repository.*;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import java.util.List;
import com.cursovalido.backend.aulas.dto.AulaRequest;
import com.cursovalido.backend.aulas.dto.CursoRequest;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdministradorAulasService {
    private final UsuarioRepository usuarios;
    private final CursoRepository cursos;
    private final ModuloRepository modulos;
    private final AulaRepository aulas;
    private final YoutubeService youtube;
    private final ProgressoAulaRepository progressos;
    private final LogAuditoriaService auditoria;
    private final CursoService gestaoCursos;

    @Transactional
    public Curso editarCurso(Long id, CursoRequest dados, Long idAdministrador) {
        Curso curso = buscarCurso(id, idAdministrador);
        gestaoCursos.atualizarDados(curso, dados);
        cursos.save(curso);
        auditoria.registrar(idAdministrador, "EDICAO_CURSO_ADMINISTRADOR", "Curso " + id, null);
        return curso;
    }

    @Transactional
    public void excluirCurso(Long id, Long idAdministrador) {
        Curso curso = buscarCurso(id, idAdministrador);
        curso.setAtivo(false);
        cursos.save(curso);
        auditoria.registrar(idAdministrador, "EXCLUSAO_CURSO_ADMINISTRADOR", "Curso " + id, null);
    }

    public List<Curso> listarCursos(Long idAdministrador) {
        verificarAdministrador(idAdministrador);
        return cursos.findByAtivoTrueOrderByIdDesc();
    }

    public Curso buscarCurso(Long id, Long idAdministrador) {
        verificarAdministrador(idAdministrador);
        Curso curso = cursos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso nao encontrado"));
        if (!curso.isAtivo())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso inativo");
        return curso;
    }

    public List<Modulo> listarModulos(Long idCurso, Long idAdministrador) {
        buscarCurso(idCurso, idAdministrador);
        return modulos.findByIdCursoAndAtivoTrueOrderByOrdemAscIdAsc(idCurso);
    }

    public Modulo buscarModulo(Long id, Long idAdministrador) {
        verificarAdministrador(idAdministrador);
        Modulo modulo = modulos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modulo nao encontrado"));
        if (!modulo.isAtivo())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Modulo inativo");
        buscarCurso(modulo.getIdCurso(), idAdministrador);
        return modulo;
    }

    public List<Aula> listarAulas(Long idModulo, Long idAdministrador) {
        buscarModulo(idModulo, idAdministrador);
        return aulas.findByIdModuloOrderByOrdemAscIdAsc(idModulo);
    }

    public Aula buscarAula(Long id, Long idAdministrador) {
        verificarAdministrador(idAdministrador);
        Aula aula = aulas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula nao encontrada"));

        buscarModulo(aula.getIdModulo(), idAdministrador);
        return aula;
    }

    @Transactional
    public Aula editarAula(Long id, AulaRequest dados, Long idAdministrador) {
        Aula aula = buscarAula(id, idAdministrador);
        String videoId = youtube.validarVideo(dados.youtubeUrl());
        if (!videoId.equals(aula.getYoutubeVideoId()))
            progressos.deleteByIdAula(id);
        aula.setTitulo(dados.titulo().trim());
        aula.setDescricao(dados.descricao().trim());
        aula.setOrdem(dados.ordem());
        aula.setYoutubeVideoId(videoId);
        aulas.save(aula);
        auditoria.registrar(idAdministrador, "EDICAO_AULA_ADMINISTRADOR", "Aula " + id, null);
        return aula;
    }

    @Transactional
    public Aula alterarDisponibilidade(Long id, boolean ativo, Long idAdministrador) {
        Aula aula = buscarAula(id, idAdministrador);
        if (aula.isAtivo() != ativo) {
            aula.setAtivo(ativo);
            aulas.save(aula);
            auditoria.registrar(idAdministrador, ativo ? "REABERTURA_AULA" : "ARQUIVAMENTO_AULA", "Aula " + id, null);
        }
        return aula;
    }

    private void verificarAdministrador(Long id) {
        var usuario = usuarios.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrador nao encontrado"));
        if (!usuario.isAtivo())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrador inativo");
        if (usuario.getPerfil() != Perfil.ADMINISTRADOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso exclusivo de administrador");
        }
    }
}
