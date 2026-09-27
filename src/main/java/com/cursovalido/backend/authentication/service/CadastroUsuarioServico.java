package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.CadastroUsuarioRequisicao;
import com.cursovalido.backend.authentication.entity.Endereco;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.exception.CadastroDuplicadoException;
import com.cursovalido.backend.authentication.exception.LimiteReenvioException;
import com.cursovalido.backend.authentication.exception.MenorDeIdadeException;
import com.cursovalido.backend.authentication.exception.TermosNaoDisponiveisException;
import com.cursovalido.backend.authentication.repository.UsuarioRepositorio;
import com.cursovalido.backend.forum.service.UsuarioForumServico;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CadastroUsuarioServico {
    private static final int MENOR_RA = 10000000;
    private static final int MAIOR_RA = 100000000;

    @Autowired
    private UsuarioRepositorio repositorio;
    @Autowired
    private PasswordEncoder codificadorSenha;
    @Autowired
    private LogAuditoriaServico auditoria;
    @Autowired
    private UsuarioForumServico usuariosForum;
    @Autowired
    private PoliticaServico politicas;
    @Autowired
    private TokenEmailServico tokensEmail;
    @Autowired
    private EmailServico emails;
    private final SecureRandom geradorRa = new SecureRandom();

    @Transactional
    // Cria uma conta de aluno e envia a confirmacao de e-mail.
    public Usuario cadastrar(CadastroUsuarioRequisicao requisicao, String enderecoIp) {
        if (!politicas.versaoAtiva(requisicao.versaoTermos()))
            throw new TermosNaoDisponiveisException();
        if (Period.between(requisicao.dataNascimento(), LocalDate.now()).getYears() < 18) {
            auditoria.registrar(null, "TENTATIVA_CADASTRO_MENOR_IDADE", requisicao.email(), enderecoIp);
            throw new MenorDeIdadeException();
        }
        if (repositorio.existsByEmail(requisicao.email()))
            throw new CadastroDuplicadoException("E-mail");
        if (repositorio.existsByCpf(requisicao.cpf()))
            throw new CadastroDuplicadoException("CPF");

        Usuario usuario = new Usuario();
        usuario.setNomeCompleto(requisicao.nomeCompleto());
        usuario.setEmail(requisicao.email());
        usuario.setSenha(codificadorSenha.encode(requisicao.senha()));
        usuario.setCpf(requisicao.cpf());
        usuario.setDataNascimento(requisicao.dataNascimento());
        usuario.setTelefone(requisicao.telefone());
        usuario.setMatricula(gerarRa());
        var endereco = requisicao.endereco();
        usuario.setEndereco(new Endereco(endereco.cep(), endereco.logradouro(), endereco.bairro(), endereco.cidade(),
                endereco.estado()));
        usuario.setDeclarouMaiorIdade(requisicao.declarouMaiorIdade());
        usuario.setAceitouTermosLgpd(requisicao.aceitouTermosLgpd());
        usuario.setDataHoraAceite(LocalDateTime.now());
        usuario.setVersaoTermos(requisicao.versaoTermos());
        usuario.setPerfil(Perfil.ALUNO);
        usuario.setEmailConfirmado(false);
        Usuario salvo = repositorio.save(usuario);
        String token = tokensEmail.criarConfirmacao(salvo);
        emails.enviarConfirmacao(salvo, token);
        auditoria.registrar(salvo.getId(), "CADASTRO_USUARIO", "Cadastro realizado", enderecoIp);
        return salvo;
    }

    @Transactional
    // Confirma o e-mail e sincroniza o usuario com o forum.
    public void confirmarEmail(String token) {
        var registro = tokensEmail.validar(token, tokensEmail.finalidadeConfirmacao());
        Usuario usuario = registro.getUsuario();
        usuario.setEmailConfirmado(true);
        repositorio.save(usuario);
        usuariosForum.sincronizar(usuario);
        auditoria.registrar(usuario.getId(), "CONFIRMACAO_EMAIL", "E-mail confirmado", null);
    }

    // Reenvia o e-mail de confirmacao quando ainda for necessario.
    public void reenviarConfirmacao(String email) {
        repositorio.findByEmail(email).filter(usuario -> !usuario.isEmailConfirmado()).ifPresent(usuario -> {
            if (tokensEmail.excedeuLimiteConfirmacao(usuario))
                throw new LimiteReenvioException();
            String token = tokensEmail.criarNovaConfirmacao(usuario);
            emails.enviarConfirmacao(usuario, token);
        });
    }

    private String gerarRa() {
        String ra;
        do {
            ra = Integer.toString(geradorRa.nextInt(MENOR_RA, MAIOR_RA));
        } while (repositorio.existsByMatricula(ra));
        return ra;
    }
}