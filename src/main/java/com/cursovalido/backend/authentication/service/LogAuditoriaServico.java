package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.LogAuditoria;
import com.cursovalido.backend.authentication.repository.LogAuditoriaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.annotation.PostConstruct;

@Service
public class LogAuditoriaServico {
    @Autowired
    private LogAuditoriaRepositorio repositorio;
    @Value("${seguranca.auditoria.chave}")
    private String chaveTexto;

    // Registra uma acao e liga o registro ao anterior.
    public synchronized void registrar(Long usuarioId, String tipoAcao, String detalhes, String enderecoIp) {
        String hashAnterior = repositorio.findTopByOrderByIdDesc()
                .map(LogAuditoria::getAssinatura)
                .orElse(null);
        LocalDateTime dataHora = LocalDateTime.now();
        String assinatura = assinar(hashAnterior, tipoAcao, detalhes, enderecoIp, dataHora);
        repositorio.save(new LogAuditoria(usuarioId, tipoAcao, detalhes, enderecoIp,
                hashAnterior, assinatura));
    }

    @PostConstruct
    // Protege registros antigos que ainda nao possuem assinatura.
    public synchronized void protegerRegistrosAnteriores() {
        String hashAnterior = null;
        for (LogAuditoria log : repositorio.findAllByOrderByIdAsc()) {
            if (log.getAssinatura() == null) {
                String assinatura = assinar(hashAnterior, log.getTipoAcao(), log.getDetalhes(),
                        log.getEnderecoIp(), log.getDataHora());
                log.proteger(hashAnterior, assinatura);
                repositorio.save(log);
            }
            hashAnterior = log.getAssinatura();
        }
    }

    // Confere se os registros de auditoria continuam inteiros.
    public boolean verificarIntegridade() {
        String hashAnterior = null;
        for (LogAuditoria log : repositorio.findAllByOrderByIdAsc()) {
            if (log.getAssinatura() == null)
                return false;
            if (!java.util.Objects.equals(hashAnterior, log.getHashAnterior()))
                return false;
            String assinatura = assinar(log.getHashAnterior(), log.getTipoAcao(), log.getDetalhes(),
                    log.getEnderecoIp(), log.getDataHora());
            if (!java.util.Objects.equals(assinatura, log.getAssinatura()))
                return false;
            hashAnterior = log.getAssinatura();
        }
        return true;
    }

    private String assinar(String hashAnterior, String tipoAcao, String detalhes,
            String enderecoIp, LocalDateTime dataHora) {
        String conteudo = String.join("|", valor(hashAnterior), valor(tipoAcao), valor(detalhes),
                valor(enderecoIp), dataHora.toString());
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(chaveTexto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(conteudo.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException excecao) {
            throw new IllegalStateException("Nao foi possivel proteger o log", excecao);
        }
    }

    private String valor(String valor) {
        return valor == null ? "" : valor;
    }
}