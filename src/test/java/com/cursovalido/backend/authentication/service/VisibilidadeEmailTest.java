package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.security.VisibilidadeEmail;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VisibilidadeEmailTest {
    @Test
    void mascaraAlunoMesmoParaAdministradorSemExporTamanhoDoNome() {
        assertEquals("e*******@gmail.com", VisibilidadeEmail.paraListagem("estudante@gmail.com", "ALUNO", true));
        assertEquals("e*******@gmail.com", VisibilidadeEmail.mascarar("e@gmail.com"));
    }

    @Test
    void emailDaEquipeSoFicaCompletoParaAdministrador() {
        for (String perfil : new String[] { "PROFESSOR", "ADMINISTRADOR" }) {
            assertEquals("equipe@example.com", VisibilidadeEmail.paraListagem("equipe@example.com", perfil, true));
            assertEquals("e*******@example.com", VisibilidadeEmail.paraListagem("equipe@example.com", perfil, false));
        }
    }

    @Test
    void entradaAusenteOuInvalidaNaoExibeValorOriginal() {
        assertEquals("*******", VisibilidadeEmail.mascarar(null));
        assertEquals("*******", VisibilidadeEmail.mascarar("invalido"));
        assertEquals("*******", VisibilidadeEmail.mascarar("@example.com"));
    }
}
