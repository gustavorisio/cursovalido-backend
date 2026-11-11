package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.dto.ModuloRequest;
import com.cursovalido.backend.aulas.repository.ModuloRepository;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.cursovalido.backend.aulas.service.AulasTestSupport.*;

@ExtendWith(MockitoExtension.class)
class ModuloServiceTest {
    @Mock
    private ModuloRepository repositorio;
    @Mock
    private CursoService cursos;
    @Mock
    private LogAuditoriaService auditoria;
    @InjectMocks
    private ModuloService servico;

    @Test
    void criarVinculaCursoNormalizaEAudita() {
        var salvo = servico.criar(10L, new ModuloRequest(" Inicio ", " Basico ", 2), 1L);
        assertThat(salvo.getIdCurso()).isEqualTo(10L);
        assertThat(salvo.getTitulo()).isEqualTo("Inicio");
        assertThat(salvo.getDescricao()).isEqualTo("Basico");
        assertThat(salvo.getOrdem()).isEqualTo(2);
        assertThat(salvo.getCriadoEm()).isNotNull();
        verify(cursos).buscarAtivo(10L, 1L);
        verify(repositorio).save(salvo);
        verify(auditoria).registrar(eq(1L), eq("CADASTRO_MODULO"), anyString(), isNull());
    }

    @Test
    void listarVerificaCursoAntesDeBuscarModulos() {
        var modulo = modulo();
        when(repositorio.findByIdCursoAndAtivoTrueOrderByOrdemAscIdAsc(10L)).thenReturn(List.of(modulo));
        assertThat(servico.listar(10L, 1L)).containsExactly(modulo);
        var ordem = inOrder(cursos, repositorio);
        ordem.verify(cursos).buscarAtivo(10L, 1L);
        ordem.verify(repositorio).findByIdCursoAndAtivoTrueOrderByOrdemAscIdAsc(10L);
    }

    @Test
    void editarPreservaCurso() {
        var modulo = modulo();
        when(repositorio.findById(20L)).thenReturn(Optional.of(modulo));
        servico.editar(20L, new ModuloRequest(" Novo ", " Texto ", 3), 1L);
        assertThat(modulo.getTitulo()).isEqualTo("Novo");
        assertThat(modulo.getDescricao()).isEqualTo("Texto");
        assertThat(modulo.getOrdem()).isEqualTo(3);
        assertThat(modulo.getIdCurso()).isEqualTo(10L);
        verify(auditoria).registrar(1L, "EDICAO_MODULO", "Modulo 20", null);
    }

    @Test
    void exclusaoLogicaPreservaModulo() {
        var modulo = modulo();
        when(repositorio.findById(20L)).thenReturn(Optional.of(modulo));
        servico.apagar(20L, 1L);
        assertThat(modulo.isAtivo()).isFalse();
        verify(repositorio).save(modulo);
        verify(repositorio, never()).delete(any());
        verify(auditoria).registrar(1L, "EXCLUSAO_MODULO", "Modulo 20", null);
    }

    @Test
    void moduloAusenteRetorna404() {
        erro(() -> servico.buscarAtivo(20L, 1L), 404);
        verifyNoInteractions(cursos);
    }

    @Test
    void moduloInativoRetorna404() {
        var modulo = modulo();
        modulo.setAtivo(false);
        when(repositorio.findById(20L)).thenReturn(Optional.of(modulo));
        erro(() -> servico.buscarAtivo(20L, 1L), 404);
        verifyNoInteractions(cursos);
    }

    @Test
    void cursoProibidoImpedeCriacaoESalvamento() {
        when(cursos.buscarAtivo(10L, 1L)).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        erro(() -> servico.criar(10L, new ModuloRequest("Inicio", "Basico", 1), 1L), 403);
        verifyNoInteractions(repositorio, auditoria);
    }
}
