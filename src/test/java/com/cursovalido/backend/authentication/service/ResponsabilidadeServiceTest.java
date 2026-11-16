package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.AceiteResponsabilidade;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.AceiteResponsabilidadeRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResponsabilidadeServiceTest {

    private final AceiteResponsabilidadeRepository aceites = mock(AceiteResponsabilidadeRepository.class);
    private final ResponsabilidadeService service = new ResponsabilidadeService(aceites,
            mock(LogAuditoriaService.class));

    @Test
    void deveRegistrarAceiteDaVersaoVigenteParaProfessor() {
        Usuario professor = new Usuario();
        professor.setPerfil(Perfil.PROFESSOR);
        String versao = service.documento(Perfil.PROFESSOR).versao();

        service.registrar(professor, true, versao, "192.0.2.10");

        verify(aceites).save(any(AceiteResponsabilidade.class));
    }

    @Test
    void deveConsultarAceiteDaVersaoVigenteParaAdministrador() {
        Usuario administrador = new Usuario();
        administrador.setPerfil(Perfil.ADMINISTRADOR);
        String versao = service.documento(Perfil.ADMINISTRADOR).versao();
        when(aceites.existsByUsuarioIdAndPerfilAndVersao(null, Perfil.ADMINISTRADOR, versao)).thenReturn(true);

        assertTrue(service.atualizado(administrador));
    }

    @Test
    void deveRecusarVersaoAntigaParaProfessor() {
        assertThrows(IllegalArgumentException.class,
                () -> service.validar(Perfil.PROFESSOR, true, "0.1", "192.0.2.10"));
    }

    @Test
    void deveDispensarTermoDeResponsabilidadeParaAluno() {
        Usuario aluno = new Usuario();
        aluno.setPerfil(Perfil.ALUNO);

        service.registrar(aluno, null, null, null);

        assertTrue(service.atualizado(aluno));
        verifyNoInteractions(aceites);
    }
}
