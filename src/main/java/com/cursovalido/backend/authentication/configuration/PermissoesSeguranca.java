package com.cursovalido.backend.authentication.configuration;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seguranca.permissoes")
@Getter
@Setter
public class PermissoesSeguranca {
    private List<String> administrador = new ArrayList<>();
    private List<String> leituraProfessor = new ArrayList<>();
    private List<String> escritaProfessor = new ArrayList<>();
    private List<String> leituraAluno = new ArrayList<>();
    private List<String> escritaAluno = new ArrayList<>();
}
