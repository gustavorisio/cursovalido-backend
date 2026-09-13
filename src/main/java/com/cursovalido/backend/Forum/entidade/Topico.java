package com.cursovalido.backend.Forum.entidade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "tb_topics")
public class Topico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "title", nullable = false)
    private String titulo;
    @NotBlank
    @Size(max = 5000)
    @Column(name = "description", length = 5000, nullable = false)
    private String descricao;

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

    @Transient
    private long quantidadeRespostas;
    @Column(name = "active", nullable = false)
    private boolean ativo = true;
    @Column(name = "closed", nullable = false)
    private Boolean fechado = false;

    public Topico() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
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

    public long getQuantidadeRespostas() {
        return quantidadeRespostas;
    }

    public void setQuantidadeRespostas(long quantidadeRespostas) {
        this.quantidadeRespostas = quantidadeRespostas;
    }

    public boolean isFechado() {
        return Boolean.TRUE.equals(fechado);
    }

    public void setFechado(boolean fechado) {
        this.fechado = fechado;
    }
}