# Curso Válido - Back-end

Repositorio FrontEnd
https://github.com/gustavorisio/cursovalido-frontend

Repositorio Backend
https://github.com/gustavorisio/cursovalido-backend

## Visão Geral
Repositório da API REST do **Curso Válido**, plataforma desenvolvida como Projeto Final de Curso (PFC) de Engenharia de Software na UMC. Esta API é responsável por gerenciar toda a regra de negócio por trás da plataforma de aprendizagem. O sistema foi atualizado para operar em um modelo de acesso totalmente gratuito para os alunos. Além disso, o tratamento de dados pessoais, credenciais e perfis opera sob o escopo da Lei Geral de Proteção de Dados (LGPD), exigindo o aceite formal dos Termos de Uso e da Política de Privacidade durante a etapa de cadastro.

## Objetivo
O objetivo principal desta API é fornecer uma infraestrutura de back-end robusta e segura para a gestão de cursos, progressão de alunos e interações na comunidade em um ambiente de educação gratuita.

## Funcionalidades
A plataforma baseia-se em três principais funcionalidades educacionais:

*   **Aulas:**
    *   O cadastramento de aulas e o upload de vídeos são operações exclusivas para usuários com perfil de Professor.
    *   Os vídeos hospedados devem estar obrigatoriamente no formato MP4, respeitando limites rigorosos de tamanho definidos pelo sistema para mitigar sobrecargas de I/O no servidor.
    *   A reprodução (streaming) ocorre diretamente no frontend da aplicação.
    *   Para a aula ser considerada concluída, o aluno deve atingir um percentual mínimo de visualização do vídeo e registrar de forma explícita a conclusão no ambiente.
*   **Certificados:**
    *   Os certificados são emitidos em formato PDF exclusivamente após o aluno cumprir todos os critérios de conclusão exigidos, incluindo progresso de visualização e aprovação nas avaliações.
    *   Cada certificado contém um identificador único, e o sistema fornece um mecanismo público para que terceiros possam consultar sua autenticidade e validade.
    *   O documento é de uso exclusivo para comprovar a conclusão dos conteúdos fornecidos pela plataforma Curso Válido.
*   **Fórum (Comunidade):**
    *   Alunos, Professores e Administradores possuem acesso total ao fórum para criar tópicos de dúvidas de programação e respostas, gerando uma base de conhecimento.
    *   Os tópicos e comentários possuem um limite máximo de 5000 caracteres.
    *   A edição de conteúdo é restrita apenas ao autor original, enquanto o fechamento de um tópico pode ser feito pelo autor ou por um administrador (bloqueando novos comentários, geralmente quando a dúvida foi resolvida).
    *   A exclusão de tópicos e comentários ocorre sempre de maneira lógica (preservando o banco de dados e alterando o status para inativo), ação que inativa consultas ao conteúdo.

## Funcionamento no Projeto
*   **Segurança e Controle de Acesso (RBAC):** O sistema isola as permissões com base nos perfis de Administrador, Professor e Aluno. O acesso exige um token JWT válido e todas as senhas são armazenadas de forma segura utilizando o hash BCrypt, garantindo proteção aos dados.
*   **Liberação de Acesso:** Por ser gratuita, a liberação de acesso ao conteúdo das aulas ocorre de forma imediata após a matrícula do aluno. A revogação do acesso acontece apenas caso a conta do usuário seja inativada, cancelada ou excluída.
*   **Avaliações e Gamificação:** O conteúdo das aulas engloba avaliações nas quais os alunos devem atingir um percentual mínimo de aproveitamento estipulado pelo curso. A aprovação nestas atividades concede Pontos de Experiência (XP), base do motor de gamificação que permite a evolução de nível do aluno na plataforma.

## Desenvolvimento
### Stack e Dependências
O projeto utiliza as seguintes tecnologias:
*   **Java 21 (LTS)**
*   **Spring Boot**: Criação da API RESTful.
*   **Spring Security & JWT**: Segurança das rotas e autenticação.
*   **Spring Data JPA / Hibernate**: Mapeamento objeto-relacional.
*   **PostgreSQL**: Banco de dados relacional para persistência de todo o sistema.
*   **JUnit, Mockito e JaCoCo**: Para testes unitários e cobertura de código.

### Como instalar e rodar localmente
1. Clone o repositório:
   ```bash
   git clone https://github.com/gustavorisio/cursovalido-backend.git
   cd cursovalido-backend
   ```

2. Configure o banco de dados:
   Abra o arquivo `src/main/resources/application.yml` e coloque as credenciais do seu PostgreSQL (URL, usuário e senha).

3. Rode a aplicação usando o Maven Wrapper nativo:
   * **No Windows:**
     ```bash
     .\mvnw spring-boot:run
     ```
   * **No Linux/macOS:**
     ```bash
     ./mvnw spring-boot:run
     ```

A API estará disponível na porta configurada, por padrão em `http://localhost:8080`.

## Contribuição Esperada
O projeto foi gerenciado usando a metodologia Kanban via Asana. Espera-se que o desenvolvimento e envio de novas contribuições sigam o fluxo padrão de versionamento estabelecido para o repositório:
*   `main`: Branch focada na VPS da Hostinger, contendo apenas versões validadas e prontas para o deploy em produção.
*   `develop`: Branch de integração; todo o código para novas funcionalidades, melhorias no fórum, regras de gamificação ou testes de banco de dados devem ser submetidos para esta branch e revisados antes da release.

---

Orientador: Lucas Santos da Silva

Contribuido e desenvolvido por: Gustavo Di Risio
