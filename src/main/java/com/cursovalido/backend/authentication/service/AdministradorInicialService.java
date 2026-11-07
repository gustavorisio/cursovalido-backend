package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.ConviteAcessoRequisicao;
import com.cursovalido.backend.authentication.entity.InicializacaoAdministrador;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.InicializacaoAdministradorRepository;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdministradorInicialService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final InicializacaoAdministradorRepository inicializacao;
    private final Validator validador;
    @Value("${aplicacao.admin.email:}")
    private String email;
    @Value("${aplicacao.admin.senha:}")
    private String senha;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void criar() {
        // A migracao cria a linha unica; o fallback atende bancos temporarios com ddl-auto.
        var registro = inicializacao.buscarParaAtualizar()
                .orElseGet(() -> inicializacao.saveAndFlush(new InicializacaoAdministrador()));
        if (registro.isConcluida()) return;
        if (usuarios.existsByPerfil(Perfil.ADMINISTRADOR)) {
            registro.concluir(null);
            inicializacao.save(registro);
            log.warn("Ja existem administradores. A criacao inicial foi encerrada sem promover outra conta.");
            return;
        }
        if (email.isEmpty()) {
            if (senha.isEmpty()) {
                log.info("Defina ADMIN_EMAIL e ADMIN_PASSWORD para criar o primeiro administrador.");
                return;
            }
        }
        String endereco = email.trim().toLowerCase(Locale.ROOT);
        var configuracao = new ConviteAcessoRequisicao(endereco, Perfil.ADMINISTRADOR);
        if (!validador.validate(configuracao).isEmpty()) {
            throw new IllegalStateException("Defina ADMIN_EMAIL com um endereco de e-mail valido.");
        }
        if (senha.isBlank()) throw senhaInvalida();
        if (senha.length() < 8) throw senhaInvalida();
        if (senha.getBytes(StandardCharsets.UTF_8).length > 72) throw senhaInvalida();
        if (usuarios.existsByEmail(endereco)) {
            throw new IllegalStateException("ADMIN_EMAIL ja pertence a uma conta. Nenhuma conta sera promovida.");
        }

        Usuario administrador = new Usuario();
        administrador.setNomeCompleto("Administrador Curso Valido");
        administrador.setEmail(endereco);
        administrador.setSenha(senhas.encode(senha));
        administrador.setMatricula("ADMIN-" + UUID.randomUUID());
        administrador.setPerfil(Perfil.ADMINISTRADOR);
        administrador.setEmailConfirmado(true);
        administrador.setDeclarouMaiorIdade(false);
        administrador.setAceitouTermosLgpd(false);
        administrador = usuarios.saveAndFlush(administrador);
        registro.concluir(administrador.getId());
        inicializacao.save(registro);
        log.info("Administrador principal criado. A criacao inicial foi encerrada definitivamente neste banco.");
    }

    private IllegalStateException senhaInvalida() {
        return new IllegalStateException("ADMIN_PASSWORD deve ter pelo menos 8 caracteres e no maximo 72 bytes.");
    }
}
