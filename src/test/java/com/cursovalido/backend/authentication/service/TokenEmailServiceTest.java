package com.cursovalido.backend.authentication.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cursovalido.backend.authentication.entity.*;
import com.cursovalido.backend.authentication.exception.LimiteReenvioException;
import com.cursovalido.backend.authentication.repository.TokenEmailRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class TokenEmailServiceTest {
    private final TokenEmailRepository repositorio = mock(TokenEmailRepository.class);
    private final TokenEmailService servico = new TokenEmailService(repositorio);

    @Test
    void mesmoCodigoParaDuasPessoasNaoGeraMesmoHash() {
        var aleatorio = mock(SecureRandom.class);
        when(aleatorio.nextInt(1_000_000)).thenReturn(123456);
        ReflectionTestUtils.setField(servico, "aleatorio", aleatorio);
        Usuario primeiro = new Usuario();
        primeiro.setEmail("primeiro@example.com");
        Usuario segundo = new Usuario();
        segundo.setEmail("segundo@example.com");
        assertEquals(servico.criarCodigo2fa(primeiro), servico.criarCodigo2fa(segundo));
        var captura = ArgumentCaptor.forClass(TokenEmail.class);
        verify(repositorio, times(2)).save(captura.capture());
        assertNotEquals(captura.getAllValues().get(0).getTokenHash(), captura.getAllValues().get(1).getTokenHash());
    }

    @Test
    void recuperacaoExpiradaNaoConsomeToken() {
        var registro = new TokenEmail("hash", new Usuario(), "ALTERACAO_SENHA", LocalDateTime.now().minusMinutes(1));
        when(repositorio.findByTokenHashAndFinalidade(anyString(), eq("ALTERACAO_SENHA"))).thenReturn(Optional.of(registro));
        assertThrows(IllegalArgumentException.class, () -> servico.validar("token", "ALTERACAO_SENHA"));
        assertFalse(registro.isUtilizado());
        verify(repositorio, never()).save(any());
    }

    @Test
    void limitaReenviosDeCodigo() {
        Usuario usuario = new Usuario();
        when(repositorio.countByUsuarioIdAndFinalidadeAndCriadoEmAfter(isNull(), eq("CODIGO_2FA"), any()))
                .thenReturn(3L);
        assertThrows(LimiteReenvioException.class,
                () -> servico.criarCodigo2fa(usuario));
        verify(repositorio, never()).save(any());
    }
}
