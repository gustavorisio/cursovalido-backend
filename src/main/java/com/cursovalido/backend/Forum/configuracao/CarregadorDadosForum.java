package com.cursovalido.backend.Forum.configuracao;

import com.cursovalido.backend.Forum.entidade.UsuarioForum;
import com.cursovalido.backend.Forum.repositorio.UsuarioForumRepositorio;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CarregadorDadosForum {
    @Bean
    CommandLineRunner carregarUsuariosForum(UsuarioForumRepositorio repositorio) {
        return args -> {
            repositorio.saveAll(List.of(
                    new UsuarioForum(1L, "Gustavo Di Risio", "aluno1@cursovalido.com", "ALUNO"),
                    new UsuarioForum(2L, "João Silva", "aluno2@cursovalido.com", "ALUNO"),
                    new UsuarioForum(3L, "Prof. Alessandro", "professor@cursovalido.com", "PROFESSOR"),
                    new UsuarioForum(4L, "Admin. Lucas", "admin@cursovalido.com", "ADMINISTRADOR")));
        };
    }
}