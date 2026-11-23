package com.cursovalido.backend.authentication.suporte;

import com.cursovalido.backend.authentication.service.EmailService;
import com.cursovalido.backend.authentication.validation.suporte.SuporteValidation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suporte/solicitacoes")
public class SuporteController {
    private static final Logger log = LoggerFactory.getLogger(SuporteController.class);
    private final EmailService emails;
    private final String destinatario;
    private final LimiteSuporteService limites;

    public SuporteController(EmailService emails, @Value("${aplicacao.suporte.email:}") String destinatario,
            LimiteSuporteService limites) {
        this.emails = emails;
        this.limites = limites;
        this.destinatario = destinatario.strip();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuporteResponse enviar(@Valid @RequestBody SolicitacaoSuporteRequest pedido,
            HttpServletRequest request) {
        SuporteValidation.validarDisponibilidade(destinatario, log);
        String protocolo = UUID.randomUUID().toString();
        String conteudo = "Protocolo: " + protocolo
                + "\nE-mail da conta: " + pedido.emailConta().strip()
                + "\nUsuario: " + pedido.usuario().strip()
                + "\nMotivo: " + pedido.motivo()
                + "\nAssunto: " + pedido.assunto().strip()
                + "\n\n" + pedido.descricao().strip()
                + "\n\nDados declarados pelo solicitante; confirme a identidade antes de alterar a conta.";
        limites.enviar(request.getRemoteAddr(), pedido.emailConta(), pedido.emailConta(),
                () -> emails.enviarSuporte(destinatario, pedido.emailConta().strip(),
                        "[Suporte " + protocolo + "] " + pedido.motivo(), conteudo));
        return new SuporteResponse(protocolo, "Solicitacao enviada. O suporte respondera pelo e-mail da conta.");
    }

    public record SuporteResponse(String protocolo, String mensagem) {
    }
}
