# Curso Válido - Back-end

O Curso Válido é uma plataforma web gratuita de aprendizagem em tecnologia, desenvolvida como Projeto Final de Curso de Engenharia de Software da Universidade de Mogi das Cruzes (UMC).

A ideia surgiu da dificuldade de organizar os estudos quando as videoaulas, as notícias e os espaços para tirar dúvidas estão espalhados em diferentes sites. O projeto propõe reunir esses recursos em um mesmo ambiente, permitindo que o aluno acompanhe os conteúdos e converse com professores e outros alunos durante o aprendizado.

Este repositório contém a API da plataforma, responsável pelas regras de negócio, autenticação, controle de acesso e armazenamento dos dados. A interface é desenvolvida separadamente em React e Tailwind CSS.

- [Repositório do back-end](https://github.com/gustavorisio/cursovalido-backend)
- [Repositório do front-end](https://github.com/gustavorisio/cursovalido-frontend)

## Sobre o projeto

A plataforma é voltada a interessados em aprender sobre tecnologia e programação. Seu escopo está organizado em três partes: aulas, notícias e comunidade.

Os alunos poderão estudar por meio de cursos e videoaulas, acompanhar publicações sobre tecnologia e participar do fórum. Os professores serão responsáveis pela organização dos conteúdos e pela publicação de notícias, além de participar das discussões. Os administradores cuidarão dos acessos e das operações administrativas da plataforma.

O projeto está em desenvolvimento. A versão atual do back-end já possui autenticação, gestão de convites, termos de uso, consulta de CEP e fórum. Os módulos de aulas e notícias serão implementados nas próximas etapas.

## Funcionalidades

### Cadastro e acesso

O cadastro de aluno exige confirmação de maioridade, aceite dos termos vigentes e confirmação do e-mail. O login é feito com e-mail e senha, seguido de um código enviado por e-mail. Depois dessa confirmação, a API emite o JWT usado nas requisições autenticadas.

As senhas são armazenadas com BCrypt. Também há recuperação de senha e proteção contra tentativas repetidas de acesso. Professores e administradores entram por convite, com conclusão do cadastro e aprovação administrativa.

### Comunidade

O fórum permite criar tópicos sobre dúvidas de programação e responder às discussões de outros usuários. A listagem possui busca e paginação, e a edição de tópicos e comentários respeita a autoria de cada publicação.

A descrição dos tópicos e o conteúdo dos comentários têm limite de 5.000 caracteres. Também é possível fechar tópicos para novas respostas e retirar conteúdos de circulação conforme as permissões do usuário.

### Aulas e cursos - implementação prevista

O módulo de aprendizagem permitirá organizar cursos em módulos e aulas. Os professores poderão cadastrar e gerenciar os conteúdos, vinculando vídeos do YouTube às aulas.

Os alunos poderão se matricular gratuitamente nos cursos, acessar as videoaulas e acompanhar seu progresso. A reprodução será feita pelo player do YouTube na interface da plataforma, utilizando as resoluções disponíveis no serviço.

### Notícias - implementação prevista

Os professores poderão criar, editar e excluir notícias sobre programação e tecnologia. Os alunos terão acesso à listagem e à leitura das publicações, reunindo novidades da área no mesmo ambiente em que estudam.

Esse módulo complementará as aulas com conteúdos sobre ferramentas, linguagens e outros assuntos relacionados à formação em tecnologia.

### Dados pessoais e termos de uso

O sistema registra a versão e a data do aceite dos termos e permite publicar novas políticas, solicitar novo aceite, revogar o consentimento e excluir a conta. A consulta de CEP pelo ViaCEP auxilia no preenchimento do endereço.

Há criptografia de campos pessoais e registro de operações em auditoria, com assinaturas para verificar a integridade dos registros.

## Tecnologias

O back-end utiliza Java 21 com Spring Boot, Spring MVC, Spring Security e Spring Data JPA. Os dados são armazenados no PostgreSQL, com migrações gerenciadas pelo Flyway.

A autenticação utiliza JWT com a biblioteca JJWT, e as entradas são validadas com Jakarta Bean Validation. O Resend é utilizado para envio de e-mails e o ViaCEP para consulta de endereços. Os testes utilizam JUnit, Mockito, Spring Boot Test e H2.

## Como executar

### 1. Preparar o ambiente

Instale o JDK 21, Git e PostgreSQL. Se utilizar o VS Code, instale também o Extension Pack for Java.

Confira a versão do Java:

```powershell
java -version
```

O projeto inclui Maven Wrapper. Na primeira execução, é necessário acesso à internet para baixar o Maven e as dependências.

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

Também é possível definir `SERVER_PORT`, cujo padrão é `8080`, e `JWT_EXPIRATION`, cujo padrão é `3600000` milissegundos, ou uma hora.

As configurações estão em `src/main/resources/application.properties`. Este procedimento utiliza as variáveis do Windows e dispensa um arquivo `.env`. As referências a Gmail e YouTube presentes nesse arquivo ainda não são utilizadas pelos fluxos implementados.

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
.\mvnw.cmd spring-boot:run
```

No Linux ou macOS, com as variáveis configuradas no ambiente:

```bash
./mvnw spring-boot:run
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
.\mvnw.cmd test
```

O perfil de testes utiliza H2, configurado em `src/test/resources/application-test.properties`. O projeto possui testes unitários e teste de integração do fórum.

## Organização e contexto acadêmico

O back-end segue uma organização em camadas, com controladores, serviços, repositórios, entidades e DTOs. Os pacotes atuais concentram autenticação, endereços e fórum. O desenvolvimento é organizado por Kanban com apoio do Asana e versionamento em Git/GitHub.

**Autor:** Gustavo Di Risio.

**Orientador:** Prof. Lucas Santos da Silva.

**Coorientador:** Prof. Alessandro Aparecido da Silva Horas.

**Instituição:** Universidade de Mogi das Cruzes - Engenharia de Software, 2026.
