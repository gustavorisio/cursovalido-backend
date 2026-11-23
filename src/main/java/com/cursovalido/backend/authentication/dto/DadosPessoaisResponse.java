package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.StatusAprovacao;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

public record DadosPessoaisResponse(Long id, String nomeCompleto, String email, String gamertag,
                String matricula,
                Perfil perfil, boolean ativo, boolean emailConfirmado,
                boolean doisFatoresAtivos,
                StatusAprovacao statusAprovacao,
                Boolean declarouMaiorIdade, Boolean aceitouTermosLgpd, String versaoTermos,
                LocalDateTime dataHoraAceite, List<Consentimento> historicoConsentimentos,
                @JsonInclude(JsonInclude.Include.NON_NULL) List<RegistroAuditoria> registrosAuditoria) {
        public record Consentimento(String versao, LocalDateTime dataHoraAceite,
                        LocalDateTime dataHoraArquivamento, String motivo) {
        }

        public record RegistroAuditoria(String acao, String detalhes, String enderecoIp, LocalDateTime dataHora) {
        }
}
