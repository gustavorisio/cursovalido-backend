package com.cursovalido.backend.authentication.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "nome_completo", nullable = false)
    private String nomeCompleto;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false, length = 60)
    private String senha;
    @Column(nullable = false, unique = true)
    private String cpf;
    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;
    @Column(nullable = false)
    private String telefone;
    @Column(nullable = false, unique = true)
    private String matricula;
    @Embedded
    private Endereco endereco;
    @Column(name = "declarou_maior_idade", nullable = false)
    private Boolean declarouMaiorIdade;
    @Column(name = "aceitou_termos_lgpd", nullable = false)
    private Boolean aceitouTermosLgpd;
    @Column(name = "data_hora_aceite", nullable = false)
    private LocalDateTime dataHoraAceite;
    @Column(name = "versao_termos", nullable = false)
    private String versaoTermos;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Perfil perfil;
    @Column(nullable = false)
    private boolean ativo = true;
    @Column(name = "email_confirmado", nullable = false)
    private boolean emailConfirmado = false;
    @Enumerated(EnumType.STRING)
    @Column(name = "status_aprovacao", nullable = false)
    private StatusAprovacao statusAprovacao = StatusAprovacao.APROVADO;

    public Long getId() {
        return id;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public String getEmail() {
        return email;
    }

    public String getSenha() {
        return senha;
    }

    public String getCpf() {
        return cpf;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getMatricula() {
        return matricula;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public Endereco getEndereco() {
        return endereco;
    }

    public Boolean getDeclarouMaiorIdade() {
        return declarouMaiorIdade;
    }

    public Boolean getAceitouTermosLgpd() {
        return aceitouTermosLgpd;
    }

    public LocalDateTime getDataHoraAceite() {
        return dataHoraAceite;
    }

    public String getVersaoTermos() {
        return versaoTermos;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public boolean isEmailConfirmado() {
        return emailConfirmado;
    }

    public StatusAprovacao getStatusAprovacao() {
        return statusAprovacao;
    }

    public void setNomeCompleto(String valor) {
        nomeCompleto = valor;
    }

    public void setEmail(String valor) {
        email = valor;
    }

    public void setSenha(String valor) {
        senha = valor;
    }

    public void setCpf(String valor) {
        cpf = valor;
    }

    public void setDataNascimento(LocalDate valor) {
        dataNascimento = valor;
    }

    public void setTelefone(String valor) {
        telefone = valor;
    }

    public void setMatricula(String valor) {
        matricula = valor;
    }

    public void setEndereco(Endereco valor) {
        endereco = valor;
    }

    public void setDeclarouMaiorIdade(Boolean valor) {
        declarouMaiorIdade = valor;
    }

    public void setAceitouTermosLgpd(Boolean valor) {
        aceitouTermosLgpd = valor;
    }

    public void setDataHoraAceite(LocalDateTime valor) {
        dataHoraAceite = valor;
    }

    public void setVersaoTermos(String valor) {
        versaoTermos = valor;
    }

    public void setPerfil(Perfil valor) {
        perfil = valor;
    }

    public void setEmailConfirmado(boolean valor) {
        emailConfirmado = valor;
    }

    public void setStatusAprovacao(StatusAprovacao valor) {
        statusAprovacao = valor;
    }

    public void setAtivo(boolean valor) {
        ativo = valor;
    }
}