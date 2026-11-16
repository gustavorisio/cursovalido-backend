package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.StatusAprovacao;
import com.cursovalido.backend.authentication.exception.LimiteReenvioException;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnvioCodigo2faService {
    private final UsuarioRepository usuarios;
    private final TokenEmailService tokensEmail;
    private final EmailService emails;
    private final LogAuditoriaService auditoria;

    @Transactional
    public void enviar(Long usuarioId, String enderecoIp, boolean reenvio) {
        var usuario = usuarios.buscarParaAtualizar(usuarioId).orElse(null);
        if (usuario == null || !usuario.isAtivo() || usuario.getStatusAprovacao() != StatusAprovacao.APROVADO)
            return;

        var ultimo = tokensEmail.ultimoCodigo2fa(usuario).orElse(null);
        LocalDateTime agora = LocalDateTime.now();
        if (reenvio) {
            if (ultimo == null || ultimo.isUtilizado())
                return;
            if (ultimo.getCriadoEm().plusSeconds(60).isAfter(agora))
                throw new LimiteReenvioException();
        } else if (ultimo != null && !ultimo.isUtilizado() && ultimo.getExpiraEm().isAfter(agora)) {
            return;
        }

        String codigo = tokensEmail.criarCodigo2fa(usuario);
        emails.enviarCodigo2fa(usuario, codigo);
        auditoria.registrar(usuario.getId(), "CODIGO_2FA_ENVIADO",
                reenvio ? "Codigo 2FA reenviado por e-mail" : "Codigo enviado por e-mail", enderecoIp);
    }
}
