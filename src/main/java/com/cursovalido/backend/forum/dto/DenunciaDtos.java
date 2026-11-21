package com.cursovalido.backend.forum.dto;

import com.cursovalido.backend.forum.entity.MotivoDenuncia;
import com.cursovalido.backend.forum.entity.StatusDenuncia;
import com.cursovalido.backend.forum.entity.TipoConteudoForum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class DenunciaDtos {
    private DenunciaDtos() {
    }

    public record DenunciaRequest(@NotNull MotivoDenuncia motivo, @Size(max = 1000) String descricao) {
    }

    public record DenunciaRegistradaResponse(Long id, StatusDenuncia status, String mensagem) {
    }

    public record DecisaoDenunciaRequest(@NotNull Boolean procedente, @NotNull Boolean removerConteudo,
            @NotBlank @Size(max = 1000) String observacao) {
    }

    // O denunciante nao e exposto: a moderacao avalia o conteudo, nao quem denunciou.
    public record DenunciaResponse(Long id, TipoConteudoForum tipo, Long idConteudo, Long idTopico, String trecho,
            boolean conteudoAtivo, Long idAutor, String nomeAutor, MotivoDenuncia motivo, String descricao,
            long denunciasPendentesDoConteudo, StatusDenuncia status, LocalDateTime criadaEm,
            LocalDateTime decididaEm, String observacaoModeracao) {
    }
}
