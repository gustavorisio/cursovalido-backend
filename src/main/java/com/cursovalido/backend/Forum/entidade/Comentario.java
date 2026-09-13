package com.cursovalido.backend.Forum.entidade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "tb_comments")
public class Comentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 5000)
    @Column(name = "content", length = 5000, nullable = false)
    private String conteudo;
    @Transient
    private String nomeAutor;
    @Transient
    private String papelAutor;
    @Column(name = "author_id", nullable = false)
    private Long idAutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", insertable = false, updatable = false, nullable = false)
    @JsonIgnore
    private UsuarioForum usuarioAutor;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime criadoEm;
    @Column(name = "active", nullable = false)
    private boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    @JsonIgnore
    private Topico topico;

    public Comentario() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public String getNomeAutor() {
        return usuarioAutor != null ? usuarioAutor.getNome() : nomeAutor;
    }

    public void setNomeAutor(String nomeAutor) {
        this.nomeAutor = nomeAutor;
    }

    public String getPapelAutor() {
        return usuarioAutor != null ? usuarioAutor.getPerfil() : papelAutor;
    }

    public void setPapelAutor(String papelAutor) {
        this.papelAutor = papelAutor;
    }

    public Long getIdAutor() {
        return idAutor;
    }

    public void setIdAutor(Long idAutor) {
        this.idAutor = idAutor;
    }

    public UsuarioForum getUsuarioAutor() {
        return usuarioAutor;
    }

    public void setUsuarioAutor(UsuarioForum usuarioAutor) {
        this.usuarioAutor = usuarioAutor;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Topico getTopico() {
        return topico;
    }

    public void setTopico(Topico topico) {
        this.topico = topico;
    }
}