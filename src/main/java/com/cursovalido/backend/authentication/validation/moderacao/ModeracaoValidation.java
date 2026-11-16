package com.cursovalido.backend.authentication.validation.moderacao;

import com.cursovalido.backend.authentication.dto.SuspensaoRequest;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class ModeracaoValidation {
    private ModeracaoValidation() {
    }

    public static void validarExpiracao(SuspensaoRequest dados) {
        if (dados.expiraEm() != null) {
            if (!dados.expiraEm().isAfter(Instant.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A expiracao deve estar no futuro");
            }
        }
    }

    public static Usuario verificarAlvo(Long idUsuario, Long idAdministrador,
            PrivilegiosAdministrativosService privilegios,
            UsuarioRepository usuarios) {
        var administrador = privilegios.verificarAdministrador(idAdministrador);
        var alvo = usuarios.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));
        if (!podeModerar(administrador, alvo, privilegios.ehPrincipal(administrador), privilegios))
            throw negado();
        return alvo;
    }

    public static boolean podeModerar(Usuario administrador, Usuario alvo, boolean administradorPrincipal,
            PrivilegiosAdministrativosService privilegios) {
        if (administrador.getId().equals(alvo.getId()) || alvo.getExcluidoEm() != null) {
            return false;
        }
        return !privilegios.ehPrincipal(alvo)
                && (alvo.getPerfil() != Perfil.ADMINISTRADOR || administradorPrincipal);
    }

    private static ResponseStatusException negado() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Voce nao pode moderar esta conta");
    }

    public static void validarVinculo(boolean pertenceAoUsuario) {
        if (!pertenceAoUsuario) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Suspensao nao encontrada");
        }
    }
}
