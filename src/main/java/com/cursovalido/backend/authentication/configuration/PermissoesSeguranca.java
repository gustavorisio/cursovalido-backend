package com.cursovalido.backend.authentication.configuration;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seguranca.permissoes")
public class PermissoesSeguranca {
    private List<String> administrador = new ArrayList<>();
    private List<String> leituraProfessor = new ArrayList<>();
    private List<String> escritaProfessor = new ArrayList<>();
    private List<String> leituraAluno = new ArrayList<>();
    private List<String> escritaAluno = new ArrayList<>();

    public List<String> getAdministrador() {
        return administrador;
    }

    public List<String> getLeituraProfessor() {
        return leituraProfessor;
    }

    public List<String> getEscritaProfessor() {
        return escritaProfessor;
    }

    public List<String> getLeituraAluno() {
        return leituraAluno;
    }

    public List<String> getEscritaAluno() {
        return escritaAluno;
    }

    public void setAdministrador(List<String> valor) {
        administrador = valor;
    }

    public void setLeituraProfessor(List<String> valor) {
        leituraProfessor = valor;
    }

    public void setEscritaProfessor(List<String> valor) {
        escritaProfessor = valor;
    }

    public void setLeituraAluno(List<String> valor) {
        leituraAluno = valor;
    }

    public void setEscritaAluno(List<String> valor) {
        escritaAluno = valor;
    }
}