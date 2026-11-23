package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.repository.VersaoDocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VersaoDocumentoService {
    private final VersaoDocumentoRepository repositorio;

    // Mantém o bloqueio no banco até concluir a publicação.
    @Transactional(propagation = Propagation.MANDATORY)
    public String proxima(String tipo) {
        return repositorio.bloquear(tipo)
                .orElseThrow(() -> new IllegalStateException("Sequencia de documentos nao inicializada: " + tipo))
                .incrementar();
    }
}
