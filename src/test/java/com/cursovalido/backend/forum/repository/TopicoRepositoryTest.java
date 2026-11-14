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
class TopicoRepositoryTest {

    @Autowired
    private UsuarioForumRepository usuarios;
    @Autowired
    private TopicoRepository topicos;

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

    @Test
    void meusTopicosFiltraAutorAntesDePaginarEBuscar() {
        UsuarioForum autor = usuarios.save(new UsuarioForum(17L, "Autor", "autor@teste.com", "ALUNO"));
        UsuarioForum outro = usuarios.save(new UsuarioForum(18L, "Outro", "outro@teste.com", "PROFESSOR"));
        for (int i = 0; i < 9; i++) {
            Topico topico = new Topico();
            topico.setTitulo(i == 0 ? "Python" : "Java " + i);
            topico.setDescricao("Duvida");
            topico.setUsuarioAutor(i == 8 ? outro : autor);
            topico.setIdAutor(i == 8 ? outro.getId() : autor.getId());
            topico.setCriadoEm(LocalDateTime.now().plusSeconds(i));
            topico.setAtivo(i != 7);
            topicos.save(topico);
        }
        topicos.flush();
        var pagina = topicos.listarMeus(org.springframework.data.domain.PageRequest.of(0, 5), "", autor.getId());
        assertEquals(7, pagina.getTotalElements());
        assertEquals(2, pagina.getTotalPages());
        assertEquals(5, pagina.getContent().size());
        assertTrue(pagina.getContent().stream().allMatch(t -> t.idAutor().equals(autor.getId())));
        var busca = topicos.listarMeus(org.springframework.data.domain.PageRequest.of(1, 5), "Java", autor.getId());
        assertEquals(6, busca.getTotalElements());
        assertEquals(1, busca.getContent().size());
    }
}
