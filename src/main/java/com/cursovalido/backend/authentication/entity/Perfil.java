package com.cursovalido.backend.authentication.entity;

public enum Perfil {
    ADMINISTRADOR,
    PROFESSOR,
    ALUNO;

    public boolean exigeTermoResponsabilidade() {
        return this == PROFESSOR || this == ADMINISTRADOR;
    }

    public boolean equipe() {
        return this == PROFESSOR || this == ADMINISTRADOR;
    }
}
