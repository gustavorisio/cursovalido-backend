# Curso Válido - Back-end

O Curso Válido é uma plataforma web gratuita de aprendizagem em tecnologia, desenvolvida como Projeto Final de Curso de Engenharia de Software da Universidade de Mogi das Cruzes (UMC).

A ideia surgiu da dificuldade de organizar os estudos quando as videoaulas, as notícias e os espaços para tirar dúvidas estão espalhados em diferentes sites. O projeto propõe reunir esses recursos em um mesmo ambiente, permitindo que o aluno acompanhe os conteúdos e converse com professores e outros alunos durante o aprendizado.

Este repositório contém a API da plataforma, responsável pelas regras de negócio, autenticação, controle de acesso e armazenamento dos dados. A interface é desenvolvida separadamente em React e Tailwind CSS.

- [Repositório do back-end](https://github.com/gustavorisio/cursovalido-backend)
- [Repositório do front-end](https://github.com/gustavorisio/cursovalido-frontend)

## Sobre o projeto

A plataforma é voltada a interessados em aprender sobre tecnologia e programação. Seu escopo está organizado em três partes: aulas, notícias e comunidade.

Os alunos podem estudar por meio de cursos e videoaulas, acompanhar publicações sobre tecnologia e participar do fórum. Os professores organizam os cursos, módulos e aulas sob sua responsabilidade e participam das discussões. Os administradores gerenciam notícias, acessos e operações administrativas da plataforma.

O projeto está em desenvolvimento. A versão atual do back-end possui autenticação, gestão de convites, termos e políticas, cursos, módulos, aulas, matrículas, progresso, notícias, fórum, moderação, dashboard administrativo e formulário de suporte.

## Funcionalidades

### Cadastro e acesso

O cadastro não depende de consulta externa a CPF/CNPJ: o CPF e a idade são validados localmente. O cadastro de aluno exige confirmação de maioridade, aceite dos termos vigentes e confirmação do e-mail. O login é feito com e-mail e senha, seguido de um código enviado por e-mail. Depois dessa confirmação, a API emite o JWT usado nas requisições autenticadas.

As senhas são armazenadas com BCrypt. Também há recuperação de senha e proteção contra tentativas repetidas de acesso. Professores e administradores entram por convite, com conclusão do cadastro e aprovação administrativa.

### Comunidade

O fórum permite criar tópicos sobre dúvidas de programação e responder às discussões de outros usuários. A consulta de tópicos e comentários é pública; a participação exige autenticação. A listagem de tópicos possui busca e paginação, e a edição de tópicos e comentários respeita a autoria de cada publicação.

A descrição dos tópicos e o conteúdo dos comentários têm limite de 5.000 caracteres. Também é possível fechar tópicos para novas respostas e retirar conteúdos de circulação conforme as permissões do usuário.

### Aulas e cursos

O módulo de aprendizagem organiza cursos em módulos e aulas. Os professores podem cadastrar e gerenciar os conteúdos sob sua responsabilidade, vinculando vídeos do YouTube às aulas. O backend valida o vídeo pela API do YouTube antes de associá-lo à aula.

Os alunos podem se matricular gratuitamente nos cursos disponíveis, acessar as videoaulas, salvar a posição de reprodução e marcar aulas como concluídas. A reprodução pelo player do YouTube é responsabilidade do front-end; o registro de conclusão não comprova, por si só, aprendizagem ou tempo assistido.

### Notícias

As notícias possuem listagem pública com busca e paginação e leitura das publicações ativas. Administradores ativos podem criar notícias; a edição e a exclusão são restritas ao administrador autor. A exclusão é lógica, retirando a publicação das consultas ativas.

Esse módulo complementa as aulas com conteúdos sobre ferramentas, linguagens e outros assuntos relacionados à formação em tecnologia.

### Administração e suporte

A área administrativa reúne gestão de convites, aprovações, documentos, restrições de usuários e consulta e moderação de aulas. Algumas operações são exclusivas do administrador principal. O dashboard apresenta indicadores operacionais, como quantidades de usuários, cursos, aulas, matrículas, tópicos e notícias, além de matrículas por mês, cursos mais procurados e distribuição de usuários por perfil.

O formulário público de suporte gera um protocolo e encaminha a solicitação por e-mail, sujeito à configuração e aos limites de envio. O recurso não mantém um sistema completo de acompanhamento de chamados.

### Dados pessoais e termos de uso

O sistema registra a versão e a data do aceite dos documentos e permite publicar novas políticas, solicitar novo aceite, revogar o consentimento e consultar e exportar os dados pessoais disponibilizados pela API. A exclusão da conta exige confirmação por código enviado por e-mail e segue regras próprias de remoção, anonimização e retenção de registros.

O CPF é armazenado com criptografia e possui hash para identificação. As operações relevantes são registradas em auditoria com assinaturas HMAC-SHA256 encadeadas e verificação de integridade. Os JWTs são associados a sessões persistidas, permitindo verificar validade e revogar acessos.

## Tecnologias

O back-end utiliza Java 21 com Spring Boot, Spring MVC, Spring Security e Spring Data JPA. Os dados são armazenados no PostgreSQL, com migrações gerenciadas pelo Flyway.

A autenticação utiliza JWT com a biblioteca JJWT, e as entradas são validadas com Jakarta Bean Validation. O Resend é utilizado para envio de e-mails e a API do YouTube para validação dos vídeos. Os testes utilizam JUnit, Mockito, Spring Boot Test, Spring Security Test, H2 e PostgreSQL, com suporte a Testcontainers. O JaCoCo gera relatórios de cobertura dos módulos de fórum e aulas.

## Como executar

### 1. Preparar o ambiente

Instale o JDK 21, Maven, Git e PostgreSQL. Configure `JAVA_HOME` para o JDK 21 e disponibilize o Maven no `PATH`. Se utilizar o VS Code, instale também o Extension Pack for Java.

Confira a versão do Java:

```powershell
java -version
mvn -version
```

Na primeira execução, é necessário acesso à internet para baixar as dependências. Os scripts `mvnw` e `mvnw.cmd` atuais apenas procuram uma distribuição previamente instalada em `~/.m2/wrapper/dists/apache-maven-3.9.16`; eles não fazem o download do Maven. Os comandos abaixo utilizam o Maven instalado no ambiente.

### 2. Clonar o repositório

```powershell
git clone https://github.com/gustavorisio/cursovalido-backend.git
cd cursovalido-backend
```

### 3. Criar o banco

No pgAdmin ou em outro cliente PostgreSQL, crie o banco:

```sql
CREATE DATABASE cursovalido;
```

Utilize um usuário com permissão para conectar ao banco e criar os objetos das migrações. O Flyway cria as tabelas na inicialização, a partir dos scripts em `src/main/resources/db/migration`. Depois, o Hibernate valida a estrutura do banco.

### 4. Configurar as variáveis de ambiente

No Windows, pesquise **Editar as variáveis de ambiente do sistema**, clique em **Variáveis de Ambiente** e adicione os valores em **Variáveis de usuário**. Cadastre cada nome e valor separadamente, sem aspas.

Para conectar ao PostgreSQL:

```text
DB_HOST=localhost
DB_PORT=5432
DB_NAME=cursovalido
DB_USERNAME=seu_usuario
DB_PASSWORD=sua_senha
```

Para autenticação, auditoria e criptografia:

```text
JWT_SECRET=seu_segredo_jwt
AUDITORIA_CHAVE=seu_segredo_de_auditoria
DADOS_CRIPTOGRAFIA_CHAVE=sua_chave_de_32_caracteres
```

Esses valores são exemplos: gere as chaves com os comandos da próxima seção. O JWT exige pelo menos 32 bytes, e a chave de criptografia deve ter exatamente 32 caracteres ASCII.

Para envio de e-mails e integração com o front-end:

```text
RESEND_API_KEY=sua_chave_resend
RESEND_FROM_EMAIL=remetente_autorizado
APP_URL=http://localhost:3000
FRONTEND_ORIGINS=http://localhost:3000
```

Utilize um remetente autorizado na sua conta Resend. Ajuste a URL e a origem para a porta utilizada pelo front-end. Se precisar permitir mais de uma origem, separe os endereços por vírgula, sem espaços.

Para validar os vídeos vinculados às aulas e receber solicitações de suporte:

```text
YOUTUBE_API_KEY=sua_chave_da_api_youtube
SUPPORT_EMAIL=email_de_atendimento
```

Utilize uma chave com acesso à YouTube Data API v3. Sem ela, os fluxos que validam vídeos retornam indisponibilidade. Sem `SUPPORT_EMAIL`, o envio do formulário de suporte fica indisponível.

Para criar o primeiro administrador em um banco novo:

```text
ADMIN_EMAIL=email_do_administrador
ADMIN_PASSWORD=senha_do_administrador
```

A senha deve ter pelo menos oito caracteres, uma letra maiúscula, um caractere especial e no máximo 72 bytes em UTF-8. A inicialização é executada uma única vez por banco e não promove uma conta existente. Após criar a conta, remova essas duas variáveis do ambiente. O primeiro acesso exige regularizar o aceite dos documentos; o login utiliza confirmação por e-mail.

Também é possível definir `SERVER_PORT`, cujo padrão é `8080`, `JWT_EXPIRATION`, cujo padrão é `3600000` milissegundos, ou uma hora, e `BCRYPT_STRENGTH`, cujo padrão é `12` e cujo intervalo permitido é de `10` a `16`. A variável `REQUIRE_HTTPS` controla a exigência de HTTPS e possui padrão `false`.

As configurações estão em `src/main/resources/application.properties`. Este procedimento utiliza as variáveis do Windows e dispensa um arquivo `.env`. As propriedades de Gmail SMTP presentes nesse arquivo não são utilizadas pelo serviço de envio de e-mails atual, que usa o Resend. O backend não possui integração ativa com CPF.CNPJ nem consulta de CEP pelo ViaCEP.

Depois de salvar as variáveis, feche completamente e reabra o VS Code e os terminais. Se as alterações não forem reconhecidas, saia da sessão do Windows e entre novamente.

### 5. Gerar as chaves

Execute o bloco abaixo no PowerShell uma vez para `JWT_SECRET` e novamente para `AUDITORIA_CHAVE`. Copie cada resultado para sua respectiva variável; utilize valores diferentes.

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
$rng.Dispose()
```

Para gerar `DADOS_CRIPTOGRAFIA_CHAVE` com 32 caracteres ASCII:

```powershell
$bytes = New-Object byte[] 16
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[BitConverter]::ToString($bytes).Replace("-", "").ToLowerInvariant()
$rng.Dispose()
```

Guarde as chaves e não as publique no repositório. Se já houver dados no banco, preserve as chaves de criptografia e auditoria utilizadas anteriormente. A troca da chave JWT invalida os tokens emitidos.

### 6. Iniciar a aplicação

No PowerShell, na raiz do projeto:

```powershell
mvn spring-boot:run
```

No Linux ou macOS, com as variáveis configuradas no ambiente:

```bash
mvn spring-boot:run
```

No VS Code, também é possível executar a classe `com.cursovalido.CursovalidoBackendApplication` pelo depurador Java, herdando as variáveis do Windows.

A API estará disponível em `http://localhost:8080`, ou na porta definida em `SERVER_PORT`. Para conferir a execução, consulte:

```powershell
Invoke-RestMethod http://localhost:8080/api/politicas/ativas
```

Para utilizar as telas da plataforma, execute também o front-end seguindo as instruções do seu repositório. O envio de e-mails pelo Resend precisa estar configurado para concluir o cadastro e o login.

## Testes

Execute na raiz do projeto:

```powershell
mvn test
```

O projeto possui testes unitários e de integração para autenticação, cadastro, administração inicial, suporte, aulas e fórum. Há verificações dos módulos de notícias e dashboard nos testes compartilhados.

O perfil `test` utiliza H2 como configuração base em `src/test/resources/application-test.properties`. Parte dos testes substitui essa configuração por um PostgreSQL temporário e isolado, incluindo verificações de migrações Flyway.

Para os testes com PostgreSQL, disponibilize os binários locais ou um ambiente Docker compatível com Testcontainers. No Windows, o suporte de testes procura instalações em `C:\Program Files\PostgreSQL`; também é possível definir `TEST_POSTGRES_BIN` com o caminho da pasta `bin`. Sem binários locais, o modo automático tenta iniciar um contêiner `postgres:18-alpine`.

Para escolher explicitamente o modo de execução:

```powershell
mvn '-Dtest.postgres.mode=local' test
mvn '-Dtest.postgres.mode=container' test
```

Os bancos temporários dos testes não utilizam as credenciais de desenvolvimento. Os serviços externos são simulados nos testes correspondentes; a aprovação da suíte não confirma a configuração real de envio de e-mails ou da API do YouTube.

Após executar os testes, os relatórios ficam em:

- `target/surefire-reports`: resultados dos testes.
- `target/site/jacoco/index.html`: cobertura do fórum.
- `target/site/jacoco-aulas/index.html`: cobertura de aulas.

## Organização e contexto acadêmico

O back-end segue uma organização em camadas, com controladores, serviços, repositórios, entidades e DTOs. Os principais pacotes são `authentication`, `aulas`, `noticias`, `forum` e `dashboard`. O desenvolvimento é organizado por Kanban com apoio do Asana e versionamento em Git/GitHub.

**Autor:** Gustavo Di Risio.

**Orientador:** Prof. Lucas Santos da Silva.

**Coorientador:** Prof. Alessandro Aparecido da Silva Horas.

**Instituição:** Universidade de Mogi das Cruzes - Engenharia de Software, 2026.
