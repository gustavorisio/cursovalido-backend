package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.ConviteAcesso;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.exception.ServicoEmailIndisponivelException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final RestClient cliente;
    private final String enderecoRemetente;
    private final String urlAplicacao;
    private final String chaveApi;

    public EmailService(@Value("${resend.api-url}") String urlApi,
            @Value("${resend.api-key}") String chaveApi,
            @Value("${aplicacao.email-remetente}") String enderecoRemetente,
            @Value("${aplicacao.url}") String urlAplicacao) {
        SimpleClientHttpRequestFactory transporte = new SimpleClientHttpRequestFactory();
        transporte.setConnectTimeout(5000);
        transporte.setReadTimeout(10000);
        this.cliente = RestClient.builder()
                .requestFactory(transporte)
                .baseUrl(urlApi)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + chaveApi)
                .defaultHeader(HttpHeaders.USER_AGENT, "cursovalido-backend/1.0")
                .build();
        this.chaveApi = chaveApi;
        this.enderecoRemetente = enderecoRemetente;
        this.urlAplicacao = urlAplicacao;
    }

    // Envia o link para redefinir a senha.
    public void enviarRedefinicaoSenha(Usuario usuario, String token) {
        enviar(usuario.getEmail(), "Alteracao de senha do Curso Valido",
                "Ola, " + usuario.getNomeCompleto() + ".\n\n"
                        + "Altere sua senha acessando:\n"
                        + urlAplicacao + "/redefinir-senha?token=" + token + "\n\n"
                        + "Este link expira em 30 minutos.");
    }

    // Envia o código usado na segunda etapa do login.
    public void enviarCodigo2fa(Usuario usuario, String codigo) {
        enviar(usuario.getEmail(), "Codigo de acesso do Curso Valido",
                "Ola, " + usuario.getNomeCompleto() + ".\n\n"
                        + "Seu codigo de acesso e: " + codigo + "\n\n"
                        + "O codigo expira em 10 minutos e pode ser usado uma unica vez.");
    }

    // Envia o convite para completar o cadastro.
    public void enviarConvite(ConviteAcesso convite, String token) {
        enviar(convite.getEmail(), "Convite para o Curso Valido",
                "Voce foi convidado para acessar o Curso Valido como "
                        + (convite.getPerfil() == Perfil.ADMINISTRADOR ? "administrador" : "professor") + ".\n\n"
                        + "Para criar sua conta, abra o link abaixo e preencha o cadastro:\n"
                        + urlAplicacao + "/convites/cadastro?token=" + token + "\n\n"
                        + "Ao concluir, a conta ja estara liberada: entre com seu e-mail e senha em "
                        + urlAplicacao + "/login.\n"
                        + "O link e pessoal, pode ser usado uma unica vez e expira em 48 horas. "
                        + "Se voce nao esperava este convite, ignore esta mensagem.");
    }

    // Avisa o professor ou administrador convidado de que o login já pode ser
    // feito.
    public void enviarAcessoAprovado(Usuario usuario) {
        enviar(usuario.getEmail(), "Seu acesso ao Curso Valido foi aprovado",
                "Ola, " + usuario.getNomeCompleto() + ".\n\n"
                        + "Seu cadastro foi aprovado pela administracao.\n"
                        + "Entre com seu e-mail e senha em " + urlAplicacao + "/login.\n"
                        + "Depois da senha, enviaremos um codigo de 6 digitos para este e-mail.");
    }

    public void enviarExclusaoConta(Usuario usuario, String codigo) {
        enviar(usuario.getEmail(), "Confirme a exclusao da sua conta",
                "Seu codigo para excluir a conta e: " + codigo
                        + "\nO codigo expira em 10 minutos e so pode ser usado uma vez."
                        + "\nAo confirmar, sua conta sera excluida. Um novo cadastro com o mesmo e-mail"
                        + " estara disponivel imediatamente. As punicoes permanecem ate expirar ou serem revogadas.");
    }

    private void enviar(String destinatario, String assunto, String conteudo) {
        enviar(destinatario, assunto, conteudo, null);
    }

    public void enviarSuporte(String destinatario, String contato, String assunto, String conteudo) {
        enviar(destinatario, assunto, conteudo, contato);
    }

    private void enviar(String destinatario, String assunto, String conteudo, String replyTo) {
        if (chaveApi == null || chaveApi.isBlank() || enderecoRemetente == null || enderecoRemetente.isBlank()) {
            log.warn(
                    "Envio de e-mail indisponivel: verifique RESEND_API_KEY e RESEND_FROM_EMAIL no ambiente do backend.");
            throw new ServicoEmailIndisponivelException();
        }

        try {
            cliente.post()
                    .uri("/emails")
                    .body(new EmailRequest(enderecoRemetente, List.of(destinatario), assunto, conteudo, replyTo))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException excecao) {
            if (excecao instanceof RestClientResponseException resposta) {
                log.warn("Resend recusou o envio de e-mail (HTTP {}). Verifique os eventos no painel do provedor.",
                        resposta.getStatusCode().value());
            } else {
                log.warn("Falha na conexao com o provedor de e-mail: {}", excecao.getClass().getSimpleName());
            }
            throw new ServicoEmailIndisponivelException();
        }
    }

    private record EmailRequest(String from, List<String> to, String subject, String text,
            @JsonProperty("reply_to") @JsonInclude(JsonInclude.Include.NON_NULL) String replyTo) {
    }
}
