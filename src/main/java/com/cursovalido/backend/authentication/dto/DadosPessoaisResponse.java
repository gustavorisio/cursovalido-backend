package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.StatusAprovacao;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DadosPessoaisResponse(Long id, String nomeCompleto, String email, String cpf,
        LocalDate dataNascimento, String telefone, String matricula, EnderecoRequest endereco,
        Perfil perfil, boolean ativo, boolean emailConfirmado,
        StatusAprovacao statusAprovacao,
        Boolean declarouMaiorIdade, Boolean aceitouTermosLgpd, String versaoTermos,
        LocalDateTime dataHoraAceite, List<Consentimento> historicoConsentimentos,
        List<RegistroAuditoria> registrosAuditoria) {
    public record Consentimento(String versao, LocalDateTime dataHoraAceite,
            LocalDateTime dataHoraArquivamento, String motivo) {}
    public record RegistroAuditoria(String acao, String detalhes, String enderecoIp, LocalDateTime dataHora) {}
}
