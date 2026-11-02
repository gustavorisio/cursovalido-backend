package com.cursovalido.backend.forum.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
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
    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private Boolean fechado = false;

    public String getNomeAutor() {
        return usuarioAutor != null ? usuarioAutor.getNome() : nomeAutor;
    }

    public String getPapelAutor() {
        return usuarioAutor != null ? usuarioAutor.getPerfil() : papelAutor;
    }

    public boolean isFechado() {
        return Boolean.TRUE.equals(fechado);
    }

    public void setFechado(boolean fechado) {
        this.fechado = fechado;
    }
}
