package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.entity.ConviteAcesso;
import com.cursovalido.backend.authentication.exception.ServicoEmailIndisponivelException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;
import org.springframework.stereotype.Service;

@Service
public class EmailServico {
    private final RestClient cliente;
    private final String enderecoRemetente;
    private final String urlAplicacao;
    private final String chaveApi;

    public EmailServico(@Value("${resend.api-url}") String urlApi,
            @Value("${resend.api-key}") String chaveApi,
            @Value("${aplicacao.email-remetente}") String enderecoRemetente,
            @Value("${aplicacao.url}") String urlAplicacao) {
        this.cliente = RestClient.builder()
                .baseUrl(urlApi)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + chaveApi)
                .defaultHeader(HttpHeaders.USER_AGENT, "cursovalido-backend/1.0")
                .build();
        this.chaveApi = chaveApi;
        this.enderecoRemetente = enderecoRemetente;
        this.urlAplicacao = urlAplicacao;
    }

    // Envia o link para confirmar o cadastro.
    public void enviarConfirmacao(Usuario usuario, String token) {
        enviar(usuario.getEmail(), "Confirme seu cadastro no Curso Valido",
                "Ola, " + usuario.getNomeCompleto() + ".\n\n"
                        + "Confirme seu e-mail acessando:\n"
                        + urlAplicacao + "/confirmacao-email?token=" + token + "\n\n"
                        + "Este link expira em 24 horas.");
    }

    // Envia o link para redefinir a senha.
    public void enviarRedefinicaoSenha(Usuario usuario, String token) {
        enviar(usuario.getEmail(), "Alteracao de senha do Curso Valido",
                "Ola, " + usuario.getNomeCompleto() + ".\n\n"
                        + "Altere sua senha acessando:\n"
                        + urlAplicacao + "/redefinir-senha?token=" + token + "\n\n"
                        + "Este link expira em 30 minutos.");
    }

    // Envia o codigo usado na segunda etapa do login.
    public void enviarCodigo2fa(Usuario usuario, String codigo) {
        enviar(usuario.getEmail(), "Codigo de acesso do Curso Valido",
                "Ola, " + usuario.getNomeCompleto() + ".\n\n"
                        + "Seu codigo de acesso e: " + codigo + "\n\n"
                        + "O codigo expira em 10 minutos e pode ser usado uma unica vez.");
    }

    // Envia o convite para completar o cadastro.
    public void enviarConvite(ConviteAcesso convite, String token) {
        enviar(convite.getEmail(), "Convite para o Curso Valido",
                "Voce recebeu um convite para concluir seu acesso ao Curso Valido.\n\n"
                        + "Acesse:\n" + urlAplicacao + "/convites/cadastro?token=" + token + "\n\n"
                        + "O convite expira em 48 horas.");
    }

    private void enviar(String destinatario, String assunto, String conteudo) {
        if (chaveApi == null || chaveApi.isBlank())
            throw new IllegalStateException("RESEND_API_KEY nao configurada");

        try {
            cliente.post()
                    .uri("/emails")
                    .body(new EmailRequisicao(enderecoRemetente, List.of(destinatario), assunto, conteudo))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException excecao) {
            throw new ServicoEmailIndisponivelException();
        }
    }

    private record EmailRequisicao(String from, List<String> to, String subject, String text) {
    }
}