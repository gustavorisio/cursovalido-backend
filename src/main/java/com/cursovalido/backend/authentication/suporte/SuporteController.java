package com.cursovalido.backend.authentication.suporte;

import com.cursovalido.backend.authentication.service.EmailService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.cursovalido.backend.authentication.exception.SuporteIndisponivelException;

@RestController
@RequestMapping("/api/suporte/solicitacoes")
public class SuporteController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SuporteController.class);
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
            jakarta.servlet.http.HttpServletRequest request) {
        if (destinatario.isBlank()) {
            log.warn("Suporte indisponivel: configure SUPPORT_EMAIL no ambiente do processo e reinicie o backend.");
            throw new SuporteIndisponivelException();
        }
        String protocolo = UUID.randomUUID().toString();
        String conteudo = "Protocolo: " + protocolo
                + "\nE-mail da conta: " + pedido.emailConta().strip()
                + "\nUsuario: " + pedido.usuario().strip()
                + "\nE-mail para retorno: " + pedido.emailContato().strip()
                + "\nMotivo: " + pedido.motivo()
                + "\nAssunto: " + pedido.assunto().strip()
                + "\n\n" + pedido.descricao().strip()
                + "\n\nDados declarados pelo solicitante; confirme a identidade antes de alterar a conta.";
        limites.enviar(request.getRemoteAddr(), pedido.emailConta(), pedido.emailContato(),
                () -> emails.enviarSuporte(destinatario, pedido.emailContato().strip(),
                        "[Suporte " + protocolo + "] " + pedido.motivo(), conteudo));
        return new SuporteResponse(protocolo, "Solicitacao enviada. O suporte respondera pelo e-mail de contato.");
    }

    public record SuporteResponse(String protocolo, String mensagem) {}
}
