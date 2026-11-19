package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.CadastroUsuarioRequest;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.validation.cadastro.CadastroValidation;
import com.cursovalido.backend.authentication.validation.email.ValidacaoEmailService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CadastroUsuarioService {
    private static final int MENOR_RA = 10000000;
    private static final int MAIOR_RA = 100000000;

    private final UsuarioRepository repositorio;
    private final PasswordEncoder codificadorSenha;
    private final LogAuditoriaService auditoria;
    private final PoliticaService politicas;
    private final ValidacaoEmailService validacaoEmail;
    private final SecureRandom geradorRa = new SecureRandom();

    @Transactional
    // Cria uma conta de aluno. O codigo de acesso e enviado no primeiro login.
    public Usuario cadastrar(CadastroUsuarioRequest requisicao, String enderecoIp) {
        validacaoEmail.validar(requisicao.email());
        CadastroValidation.validar(requisicao, enderecoIp, politicas, repositorio, auditoria);

        Usuario usuario = new Usuario();
        usuario.setNomeCompleto(requisicao.nomeCompleto());
        usuario.setEmail(requisicao.email());
        usuario.setSenha(codificadorSenha.encode(requisicao.senha()));
        usuario.setDataNascimento(requisicao.dataNascimento());
        usuario.setMatricula(gerarRa());
        usuario.setDeclarouMaiorIdade(requisicao.declarouMaiorIdade());
        usuario.setAceitouTermosLgpd(requisicao.aceitouTermosLgpd());
        usuario.setDataHoraAceite(LocalDateTime.now());
        usuario.setVersaoTermos(requisicao.versaoTermos());
        usuario.setPerfil(Perfil.ALUNO);
        Usuario salvo = repositorio.save(usuario);
        auditoria.registrar(salvo.getId(), "CADASTRO_USUARIO", "Cadastro realizado", enderecoIp);
        return salvo;
    }

    private String gerarRa() {
        String ra;
        do {
            ra = Integer.toString(geradorRa.nextInt(MENOR_RA, MAIOR_RA));
        } while (repositorio.existsByMatricula(ra));
        return ra;
    }
}
