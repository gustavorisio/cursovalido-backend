package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.dto.ModuloRequest;
import com.cursovalido.backend.aulas.entity.Modulo;
import com.cursovalido.backend.aulas.repository.ModuloRepository;
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
public class ModuloService {
    private final ModuloRepository repositorio;
    private final CursoService cursos;
    private final LogAuditoriaService auditoria;

    public List<Modulo> listar(Long idCurso, Long idProfessor) {
        cursos.buscarAtivo(idCurso, idProfessor);
        return repositorio.findByIdCursoAndAtivoTrueOrderByOrdemAscIdAsc(idCurso);
    }

    public Modulo buscarAtivo(Long id, Long idProfessor) {
        Modulo modulo = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modulo nao encontrado"));
        if (!modulo.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Modulo inativo");
        }
        cursos.buscarAtivo(modulo.getIdCurso(), idProfessor);
        return modulo;
    }

    @Transactional
    public Modulo criar(Long idCurso, ModuloRequest dados, Long idProfessor) {
        cursos.buscarAtivo(idCurso, idProfessor);
        Modulo modulo = new Modulo();
        modulo.setIdCurso(idCurso);
        modulo.setCriadoEm(LocalDateTime.now());
        preencher(modulo, dados);
        repositorio.save(modulo);
        auditoria.registrar(idProfessor, "CADASTRO_MODULO", "Modulo " + modulo.getId(), null);
        return modulo;
    }

    @Transactional
    public Modulo editar(Long id, ModuloRequest dados, Long idProfessor) {
        Modulo modulo = buscarAtivo(id, idProfessor);
        preencher(modulo, dados);
        repositorio.save(modulo);
        auditoria.registrar(idProfessor, "EDICAO_MODULO", "Modulo " + id, null);
        return modulo;
    }

    @Transactional
    public void apagar(Long id, Long idProfessor) {
        Modulo modulo = buscarAtivo(id, idProfessor);
        modulo.setAtivo(false);
        repositorio.save(modulo);
        auditoria.registrar(idProfessor, "EXCLUSAO_MODULO", "Modulo " + id, null);
    }

    private void preencher(Modulo modulo, ModuloRequest dados) {
        modulo.setTitulo(dados.titulo().trim());
        modulo.setDescricao(dados.descricao().trim());
        modulo.setOrdem(dados.ordem());
    }
}
