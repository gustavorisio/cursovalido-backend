package com.cursovalido.backend.forum.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cursovalido.backend.forum.entity.Topico;
import com.cursovalido.backend.forum.entity.UsuarioForum;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:forum-persistencia;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class TopicoRepositorioTest {

    @Autowired
    private UsuarioForumRepositorio usuarios;
    @Autowired
    private TopicoRepositorio topicos;

    @Test
    void deveSalvarERecuperarTopicoNoBancoH2() {
        // O autor precisa existir por causa da chave estrangeira.
        UsuarioForum autor = usuarios.save(
                new UsuarioForum(7L, "Aluno", "aluno@teste.com", "ALUNO"));
        Topico topico = new Topico();
        topico.setTitulo("Persistencia");
        topico.setDescricao("Teste com H2");
        topico.setIdAutor(autor.getId());
        topico.setUsuarioAutor(autor);
        topico.setCriadoEm(LocalDateTime.now());

        Topico salvo = topicos.save(topico);
        topicos.flush();

        Topico recuperado = topicos.findById(salvo.getId()).orElseThrow();
        assertEquals("Persistencia", recuperado.getTitulo());
        assertEquals(autor.getId(), recuperado.getIdAutor());
        assertTrue(recuperado.isAtivo());
    }
}
