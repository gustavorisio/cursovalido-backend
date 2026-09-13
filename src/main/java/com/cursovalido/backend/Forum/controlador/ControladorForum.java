package com.cursovalido.backend.Forum.controlador;

import com.cursovalido.backend.Forum.entidade.Comentario;
import com.cursovalido.backend.Forum.entidade.Topico;
import com.cursovalido.backend.Forum.dto.TopicoResumoDTO;
import com.cursovalido.backend.Forum.dto.RespostaUsuariosForum;
import com.cursovalido.backend.Forum.servico.ComentarioServico;
import com.cursovalido.backend.Forum.servico.TopicoServico;
import com.cursovalido.backend.Forum.servico.UsuarioForumServico;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/forum")
public class ControladorForum {
    private final TopicoServico servicoTopicos;
    private final ComentarioServico servicoComentarios;
    private final UsuarioForumServico servicoUsuarios;

    public ControladorForum(TopicoServico servicoTopicos, ComentarioServico servicoComentarios,
            UsuarioForumServico servicoUsuarios) {
        this.servicoTopicos = servicoTopicos;
        this.servicoComentarios = servicoComentarios;
        this.servicoUsuarios = servicoUsuarios;
    }

    @GetMapping("/topicos")
    public List<TopicoResumoDTO> listarTopicosRecentes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return servicoTopicos.listarRecentes(page, size);
    }

    @PostMapping("/topicos")
    public Topico criarTopico(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long idUsuario,
            @Valid @RequestBody Topico topico) {
        return servicoTopicos.criar(topico, idUsuario);
    }

    @PutMapping("/topicos/{id}")
    public Topico editarTopico(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long idUsuario,
            @PathVariable Long id,
            @Valid @RequestBody Topico dadosAtualizados) {
        return servicoTopicos.editar(id, dadosAtualizados, idUsuario);
    }

    @PutMapping("/topicos/{id}/arquivar")
    public void arquivarTopico(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long idUsuario,
            @PathVariable Long id) {
        servicoTopicos.arquivar(id, idUsuario);
    }

    @PutMapping("/topicos/{id}/fechar")
    public void fecharTopico(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long idUsuario,
            @PathVariable Long id) {
        servicoTopicos.fechar(id, idUsuario);
    }

    @DeleteMapping("/topicos/{id}")
    public void apagarTopico(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long idUsuario,
            @PathVariable Long id) {
        servicoTopicos.apagar(id, idUsuario);
    }

    @GetMapping("/topicos/{idTopico}/comentarios")
    public List<Comentario> listarComentarios(@PathVariable Long idTopico) {
        return servicoComentarios.listarPorTopico(idTopico);
    }

    @PostMapping("/topicos/{idTopico}/comentarios")
    public Comentario adicionarComentario(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long idUsuario,
            @PathVariable Long idTopico,
            @Valid @RequestBody Comentario comentario) {
        return servicoComentarios.criar(idTopico, comentario, idUsuario);
    }

    @PutMapping("/comentarios/{id}")
    public Comentario editarComentario(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long idUsuario,
            @PathVariable Long id,
            @Valid @RequestBody Comentario dadosAtualizados) {
        return servicoComentarios.editar(id, dadosAtualizados, idUsuario);
    }

    @DeleteMapping("/comentarios/{id}")
    public void apagarComentario(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long idUsuario,
            @PathVariable Long id) {
        servicoComentarios.apagar(id, idUsuario);
    }

    @DeleteMapping("/topicos/{idTopico}/comentarios/{id}")
    public void apagarComentarioDoTopico(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long idUsuario,
            @PathVariable Long idTopico, @PathVariable Long id) {
        servicoComentarios.apagarDoTopico(idTopico, id, idUsuario);
    }

    @GetMapping("/users")
    public RespostaUsuariosForum listarUsuarios() {
        return new RespostaUsuariosForum(servicoUsuarios.listarAtivos());
    }

}