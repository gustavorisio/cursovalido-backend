Repositorio FrontEnd
https://github.com/gustavorisio/cursovalido-frontend 

Repositorio Backend
https://github.com/gustavorisio/cursovalido-backend 

# Curso Válido - Back-end

Repositório da API REST do **Curso Válido**, plataforma desenvolvida como Projeto Final de Curso (PFC) de Engenharia de Software na UMC.

## Sobre o Back-end
Esta API está por trás da plataforma de aprendizagem. Ela é responsável por gerenciar toda a regra de negócio: desde o controle de acesso por perfis (Administrador, Professor e Aluno), até o processamento das regras de gamificação (cálculo de XP), gerenciamento dos tópicos do fórum, upload das videoaulas em MP4 e a integração simulada de pagamentos.

## Principais Funcionalidades da API
* **Controle de Acesso (RBAC)**: Autenticação via token JWT, com senhas criptografadas usando BCrypt, separando bem o que aluno, professor e admin podem fazer.
* **Gestão de Conteúdo e Progresso**: Endpoints para upload de vídeos (com limites de tamanho para otimizar I/O), cadastro de aulas, avaliações.
* **Motor de Gamificação**: Lógica que atribui XP e sobe o nível do usuário com base no progresso das aulas e notas.
* **Integração de Assinaturas**: Comunicação via Webhooks com a API do Mercado Pago para simular aprovação e revogação de assinaturas.
* **Geração de Certificados**: Automação para gerar o certificado em PDF com um código único para verificação pública.

## Stack e Tecnologias

* **Java 21 (LTS)**
* **Spring Boot**: Criação da API RESTful.
* **Spring Security & JWT**: Segurança das rotas e autenticação.
* **Spring Data JPA / Hibernate**: Mapeamento objeto-relacional.
* **PostgreSQL**: Banco de dados relacional para persistência de todo o sistema.
* **JUnit, Mockito e JaCoCo**: Para testes unitários e cobertura de código.

---

## Como rodar a API localmente

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

---

## Metodologia e Versionamento

O projeto foi gerenciado usando Kanban (Asana) e as entregas no repositório seguem um fluxo básico de branches:
* `main`: Versões validadas e prontas para deploy (foco na VPS da Hostinger).
* `develop`: Branch de integração onde as novas funcionalidades (aulas, fórum, pagamentos) são unidas antes da release.

---

Orientador: Lucas Santos da Silva

Contribuido e desenvolvido por: Gustavo Di Risio
