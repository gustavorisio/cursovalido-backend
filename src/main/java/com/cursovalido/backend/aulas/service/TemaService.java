package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.entity.Tema;
import com.cursovalido.backend.aulas.repository.CursoRepository;
import com.cursovalido.backend.aulas.repository.TemaRepository;
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
public class TemaService {
    private final TemaRepository temas;
    private final CursoRepository cursos;
    private final UsuarioRepository usuarios;
    private final LogAuditoriaService auditoria;

    public List<Tema> listar() {
        return temas.findAllByOrderByNomeAsc();
    }

    public Tema buscar(Long id) {
        return temas.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tema nao encontrado"));
    }

    @Transactional
    public Tema salvar(Long id, String nome, Long idUsuario) {
        verificarGestor(idUsuario);
        String nomeLimpo = nome.trim();
        if (temas.existsByNomeIgnoreCaseAndIdNot(nomeLimpo, id == null ? 0L : id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe um tema com este nome");
        }
        Tema tema = id == null ? new Tema() : buscar(id);
        tema.setNome(nomeLimpo);
        temas.save(tema);
        auditoria.registrar(idUsuario, id == null ? "CADASTRO_TEMA" : "EDICAO_TEMA", "Tema " + tema.getId(), null);
        return tema;
    }

    @Transactional
    public void excluir(Long id, Long idUsuario) {
        verificarGestor(idUsuario);
        Tema tema = buscar(id);
        cursos.removerTema(id);
        temas.delete(tema);
        auditoria.registrar(idUsuario, "EXCLUSAO_TEMA", "Tema " + id, null);
    }

    private void verificarGestor(Long id) {
        var usuario = usuarios.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario nao encontrado"));
        if (!usuario.isAtivo() || (usuario.getPerfil() != Perfil.PROFESSOR && usuario.getPerfil() != Perfil.ADMINISTRADOR)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso exclusivo de professor ou administrador ativo");
        }
    }
}
