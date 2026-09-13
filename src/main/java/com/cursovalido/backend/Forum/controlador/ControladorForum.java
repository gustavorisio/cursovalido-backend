package com.cursovalido.backend.Forum.controlador;

import com.cursovalido.backend.Forum.entidade.Comentario;
import com.cursovalido.backend.Forum.entidade.Topico;
import com.cursovalido.backend.Forum.entidade.UsuarioForum;
import com.cursovalido.backend.Forum.dto.RespostaUsuariosForum;
import com.cursovalido.backend.Forum.servico.ComentarioServico;
import com.cursovalido.backend.Forum.servico.TopicoServico;
import com.cursovalido.backend.Forum.servico.UsuarioForumServico;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/forum")
public class ControladorForum {
    @Autowired
    private TopicoServico servicoTopicos;
    @Autowired
    private ComentarioServico servicoComentarios;
    @Autowired
    private UsuarioForumServico servicoUsuarios;

    @GetMapping("/topicos")
    public List<Topico> listarTopicosRecentes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return servicoTopicos.listarRecentes(page, size);
    }

    @PostMapping("/topicos")
    public Topico criarTopico(@RequestHeader("X-User-Id") Long idUsuario, @Valid @RequestBody Topico topico) {
        return servicoTopicos.criar(topico, idUsuario);
    }

    @PutMapping("/topicos/{id}")
    public Topico editarTopico(@RequestHeader("X-User-Id") Long idUsuario, @PathVariable Long id,
            @Valid @RequestBody Topico dadosAtualizados) {
        return servicoTopicos.editar(id, dadosAtualizados, idUsuario);
    }

    @PutMapping("/topicos/{id}/arquivar")
    public void arquivarTopico(@RequestHeader("X-User-Id") Long idUsuario, @PathVariable Long id) {
        servicoTopicos.arquivar(id, idUsuario);
    }

    @PutMapping("/topicos/{id}/fechar")
    public void fecharTopico(@RequestHeader("X-User-Id") Long idUsuario, @PathVariable Long id) {
        servicoTopicos.fechar(id, idUsuario);
    }

    @DeleteMapping("/topicos/{id}")
    public void apagarTopico(@RequestHeader("X-User-Id") Long idUsuario, @PathVariable Long id) {
        servicoTopicos.apagar(id, idUsuario);
    }

    @GetMapping("/topicos/{idTopico}/comentarios")
    public List<Comentario> listarComentarios(@PathVariable Long idTopico) {
        return servicoComentarios.listarPorTopico(idTopico);
    }

    @PostMapping("/topicos/{idTopico}/comentarios")
    public Comentario adicionarComentario(@RequestHeader("X-User-Id") Long idUsuario,
            @PathVariable Long idTopico,
            @Valid @RequestBody Comentario comentario) {
        return servicoComentarios.criar(idTopico, comentario, idUsuario);
    }

    @PutMapping("/comentarios/{id}")
    public Comentario editarComentario(@RequestHeader("X-User-Id") Long idUsuario, @PathVariable Long id,
            @Valid @RequestBody Comentario dadosAtualizados) {
        return servicoComentarios.editar(id, dadosAtualizados, idUsuario);
    }

    @DeleteMapping("/comentarios/{id}")
    public void apagarComentario(@RequestHeader("X-User-Id") Long idUsuario, @PathVariable Long id) {
        servicoComentarios.apagar(id, idUsuario);
    }

    @DeleteMapping("/topicos/{idTopico}/comentarios/{id}")
    public void apagarComentarioDoTopico(@RequestHeader("X-User-Id") Long idUsuario,
            @PathVariable Long idTopico, @PathVariable Long id) {
        servicoComentarios.apagarDoTopico(idTopico, id, idUsuario);
    }

    @GetMapping("/users")
    public RespostaUsuariosForum listarUsuarios() {
        return new RespostaUsuariosForum(servicoUsuarios.listarAtivos());
    }

}