package com.cursovalido.backend.authentication.validation.convite;

import com.cursovalido.backend.authentication.dto.CadastroUsuarioRequest;
import com.cursovalido.backend.authentication.entity.ConviteAcesso;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.StatusAprovacao;
import com.cursovalido.backend.authentication.entity.StatusConvite;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.exception.ConviteAcessoInvalidoException;
import com.cursovalido.backend.authentication.repository.ConviteAcessoRepository;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.service.PoliticaService;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;

public final class ConviteValidation {
    // Limites contra cliques repetidos e envio em massa de convites.
    public static final int SEGUNDOS_ENTRE_ENVIOS = 120;
    public static final int MAXIMO_ENVIOS_POR_CONVITE = 5;
    public static final int MAXIMO_CONVITES_POR_HORA = 20;

    private ConviteValidation() {
    }

    public static void validarCriacao(String endereco, Perfil perfil, UsuarioRepository usuarios) {
        if (perfil == null || !perfil.equipe()) {
            throw new ConviteAcessoInvalidoException(HttpStatus.BAD_REQUEST, "PERFIL_CONVITE_INVALIDO",
                    "Convites sao permitidos apenas para professores e administradores.");
        }
        if (usuarios.existsByEmail(endereco)) {
            throw new ConviteAcessoInvalidoException(HttpStatus.CONFLICT, "EMAIL_JA_CADASTRADO",
                    "Este e-mail ainda possui uma conta cadastrada. A exclusao deve ser concluida antes de enviar outro convite.");
        }
    }

    // Rejeita um novo convite quando já existe outro pendente.
    public static void validarConvitePendente(ConviteAcesso pendente) {
        if (pendente == null || pendente.expirado())
            return;
        throw new ConviteAcessoInvalidoException(HttpStatus.CONFLICT, "CONVITE_JA_PENDENTE",
                "Já existe um convite pendente para este e-mail. Use a opção Reenviar ou cancele o convite atual.");
    }

    public static void validarLimiteDoAdministrador(Long administradorId, ConviteAcessoRepository convites) {
        long enviadosNaUltimaHora = convites.countByCriadoPorAndCriadoEmAfter(administradorId,
                LocalDateTime.now().minusHours(1));
        if (enviadosNaUltimaHora >= MAXIMO_CONVITES_POR_HORA)
            throw new ConviteAcessoInvalidoException(HttpStatus.TOO_MANY_REQUESTS, "LIMITE_CONVITES",
                    "Limite de " + MAXIMO_CONVITES_POR_HORA
                            + " convites por hora atingido. Tente novamente mais tarde.");
    }

    public static void validarReenvio(ConviteAcesso convite) {
        validarPendente(convite);
        if (convite.getEnviadoEm().plusSeconds(SEGUNDOS_ENTRE_ENVIOS).isAfter(LocalDateTime.now()))
            throw new ConviteAcessoInvalidoException(HttpStatus.TOO_MANY_REQUESTS, "REENVIO_CONVITE_AGUARDE",
                    "O convite acabou de ser enviado. Aguarde 2 minutos para reenviar.");
        if (convite.getQuantidadeEnvios() >= MAXIMO_ENVIOS_POR_CONVITE)
            throw new ConviteAcessoInvalidoException(HttpStatus.TOO_MANY_REQUESTS, "LIMITE_REENVIO_CONVITE",
                    "Este convite atingiu o limite de reenvios. Cancele e crie um novo convite.");
    }

    // Professores e administradores confirmam a maioridade ao concluir o convite.
    public static void validarMaioridadeDeclarada(CadastroUsuarioRequest cadastro) {
        if (!Boolean.TRUE.equals(cadastro.declarouMaiorIdade()))
            throw new IllegalArgumentException("É necessário confirmar que você tem 18 anos ou mais.");
    }

    public static void validarCadastro(ConviteAcesso convite, CadastroUsuarioRequest cadastro,
            PoliticaService politicas, UsuarioRepository usuarios) {
        validarPrazo(convite);
        if (!convite.getEmail().equalsIgnoreCase(cadastro.email())) {
            throw new IllegalArgumentException("Use o mesmo e-mail que recebeu o convite.");
        }
        if (!politicas.versaoAtiva(cadastro.versaoTermos()))
            throw new IllegalArgumentException(
                    "Os termos foram atualizados. Recarregue a página e aceite a versão atual.");
        if (!Boolean.TRUE.equals(cadastro.aceitouTermosLgpd()))
            throw new IllegalArgumentException("Aceite os Termos de Uso e a Política de Privacidade para continuar.");
        if (usuarios.existsByEmail(convite.getEmail())) {
            throw new IllegalArgumentException("Este e-mail já possui uma conta. Entre com seu e-mail e senha.");
        }
    }

    public static void validarPendente(ConviteAcesso convite) {
        if (!StatusConvite.PENDENTE.equals(convite.getStatus()))
            throw new IllegalArgumentException(
                    "Este convite já foi usado ou cancelado. Peça um novo convite à administração.");
    }

    public static void validarPrazo(ConviteAcesso convite) {
        if (convite.expirado())
            throw new IllegalArgumentException("Convite expirado. Peça um novo convite à administração.");
    }

    public static void validarUsuarioPendente(Usuario usuario) {
        if (usuario.getStatusAprovacao() != StatusAprovacao.PENDENTE_APROVACAO) {
            throw new IllegalArgumentException("Este cadastro não está mais aguardando aprovação.");
        }
    }
}
