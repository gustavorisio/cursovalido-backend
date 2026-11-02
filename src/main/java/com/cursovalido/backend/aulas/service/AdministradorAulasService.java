package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.entity.*;
import com.cursovalido.backend.aulas.repository.*;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.repository.UsuarioRepositorio;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdministradorAulasService {
    private final UsuarioRepositorio usuarios;
    private final CursoRepository cursos;
    private final ModuloRepository modulos;
    private final AulaRepository aulas;

    public List<Curso> listarCursos(Long idAdministrador) {
        verificarAdministrador(idAdministrador);
        return cursos.findByAtivoTrueOrderByIdDesc();
    }

    public Curso buscarCurso(Long id, Long idAdministrador) {
        verificarAdministrador(idAdministrador);
        Curso curso = cursos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso nao encontrado"));
        if (!curso.isAtivo()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso inativo");
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
        if (!modulo.isAtivo()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Modulo inativo");
        buscarCurso(modulo.getIdCurso(), idAdministrador);
        return modulo;
    }

    public List<Aula> listarAulas(Long idModulo, Long idAdministrador) {
        buscarModulo(idModulo, idAdministrador);
        return aulas.findByIdModuloAndAtivoTrueOrderByOrdemAscIdAsc(idModulo);
    }

    public Aula buscarAula(Long id, Long idAdministrador) {
        verificarAdministrador(idAdministrador);
        Aula aula = aulas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula nao encontrada"));
        if (!aula.isAtivo()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula inativa");
        buscarModulo(aula.getIdModulo(), idAdministrador);
        return aula;
    }

    private void verificarAdministrador(Long id) {
        var usuario = usuarios.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrador nao encontrado"));
        if (!usuario.isAtivo()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrador inativo");
        if (usuario.getPerfil() != Perfil.ADMINISTRADOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso exclusivo de administrador");
        }
    }
}
