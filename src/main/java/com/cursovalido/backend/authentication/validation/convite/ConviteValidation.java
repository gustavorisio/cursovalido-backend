package com.cursovalido.backend.authentication.validation.convite;

import com.cursovalido.backend.authentication.dto.CadastroUsuarioRequest;
import com.cursovalido.backend.authentication.entity.ConviteAcesso;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.StatusAprovacao;
import com.cursovalido.backend.authentication.entity.StatusConvite;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.exception.ConviteAcessoInvalidoException;
import com.cursovalido.backend.authentication.exception.MenorDeIdadeException;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.service.PoliticaService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import org.springframework.http.HttpStatus;

public final class ConviteValidation {
    private ConviteValidation() {
    }

    public static void validarCriacao(String endereco, Perfil perfil, UsuarioRepository usuarios) {
        if (perfil == Perfil.ALUNO) {
            throw new ConviteAcessoInvalidoException(HttpStatus.BAD_REQUEST, "PERFIL_CONVITE_INVALIDO",
                    "Convites sao permitidos apenas para professores e administradores.");
        }
        if (usuarios.existsByEmail(endereco)) {
            throw new ConviteAcessoInvalidoException(HttpStatus.CONFLICT, "EMAIL_JA_CADASTRADO",
                    "Este e-mail ainda possui uma conta cadastrada. A exclusao deve ser concluida antes de enviar outro convite.");
        }
    }

    public static void validarIdade(CadastroUsuarioRequest cadastro) {
        if (Period.between(cadastro.dataNascimento(), LocalDate.now()).getYears() < 18)
            throw new MenorDeIdadeException();
        if (Period.between(cadastro.dataNascimento(), LocalDate.now()).getYears() > 90)
            throw new IllegalArgumentException("A idade maxima permitida e 90 anos.");
        if (!Boolean.TRUE.equals(cadastro.declarouMaiorIdade()))
            throw new MenorDeIdadeException();
    }

    public static void validarCadastro(ConviteAcesso convite, CadastroUsuarioRequest cadastro,
            PoliticaService politicas, UsuarioRepository usuarios) {
        if (expirado(convite))
            throw new IllegalArgumentException("Convite expirado");
        if (!convite.isEmailConfirmado())
            throw new IllegalArgumentException("E-mail ainda nao confirmado");
        if (!convite.getEmail().equalsIgnoreCase(cadastro.email())) {
            throw new IllegalArgumentException("E-mail diferente do convite");
        }
        if (!politicas.versaoAtiva(cadastro.versaoTermos()))
            throw new IllegalArgumentException("Versao dos termos invalida");
        if (!Boolean.TRUE.equals(cadastro.aceitouTermosLgpd()))
            throw new IllegalArgumentException("Termos nao aceitos");
        if (usuarios.existsByEmail(convite.getEmail())) {
            throw new IllegalArgumentException("Dados ja utilizados");
        }
    }

    public static void validarPendente(ConviteAcesso convite) {
        if (!StatusConvite.PENDENTE.equals(convite.getStatus()))
            throw new IllegalArgumentException("Convite indisponivel");
    }

    public static void validarPrazo(ConviteAcesso convite) {
        if (expirado(convite))
            throw new IllegalArgumentException("Convite expirado");
    }

    private static boolean expirado(ConviteAcesso convite) {
        return convite.getExpiraEm().isBefore(LocalDateTime.now());
    }

    public static void validarUsuarioPendente(Usuario usuario) {
        if (usuario.getStatusAprovacao() != StatusAprovacao.PENDENTE_APROVACAO) {
            throw new IllegalArgumentException("Pendencia indisponivel");
        }
    }
}
