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

A idade é validada localmente pela data de nascimento informada, entre 18 e 90 anos. O cadastro de aluno exige confirmação de maioridade, aceite dos termos vigentes e confirmação do e-mail. O login é feito com e-mail e senha, seguido de um código enviado por e-mail. Depois dessa confirmação, a API emite o JWT usado nas requisições autenticadas. Há uma sessão ativa por conta: um novo login completo encerra a anterior. Termos pendentes ou revogados também exigem 2FA antes de liberar o token restrito ao novo aceite.

As senhas são armazenadas com BCrypt. Também há recuperação de senha e proteção contra tentativas repetidas de acesso. Se a conta ainda não confirmou o e-mail, a recuperação reenvia a confirmação em vez de emitir um link de redefinição. Professores e administradores entram por convite, com conclusão do cadastro e aprovação administrativa.

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

O formulário público de suporte gera um protocolo e encaminha a solicitação por e-mail, sujeito à configuração e aos limites de envio. O retorno usa o e-mail da conta; não há campo separado de contato. O recurso não mantém um sistema completo de acompanhamento de chamados.

### Dados pessoais e termos de uso

O sistema registra a versão e a data do aceite dos documentos e permite publicar novas políticas, solicitar novo aceite, revogar o consentimento e consultar e exportar os dados pessoais disponibilizados pela API. A exclusão da conta exige confirmação por código enviado por e-mail e segue regras próprias de remoção, anonimização e retenção de registros.

As operações relevantes são registradas em auditoria com assinaturas HMAC-SHA256 encadeadas e verificação de integridade. Os JWTs são associados a sessões persistidas, permitindo verificar validade e revogar acessos.

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

Utilize um usuário com permissão para conectar ao banco e criar tabelas. O único script SQL é [`src/main/resources/db/v3.sql`](src/main/resources/db/v3.sql), sem comentários, CPF, usuários, senhas, textos de documentos ou conteúdo de exemplo. As tabelas de documentos e aceites permanecem porque são usadas pelo backend.

Em banco vazio, o Flyway executa automaticamente o `v3.sql` na primeira inicialização e registra a versão 3. Nas próximas inicializações, apenas valida o histórico; o Hibernate valida a estrutura. O histórico antigo V1/V2 não é mais distribuído: esta configuração se destina a bancos novos.

Para executar o arquivo manualmente em um banco vazio, use:

```powershell
psql -h localhost -U seu_usuario -d cursovalido -v ON_ERROR_STOP=1 --single-transaction -f src/main/resources/db/v3.sql
```

Somente se executar o SQL manualmente, configure `FLYWAY_BASELINE_ON_MIGRATE=true` na primeira inicialização (versão padrão: 3). Após a inicialização bem-sucedida, remova essa variável. O administrador principal é criado com `ADMIN_EMAIL` e `ADMIN_PASSWORD`. Publique os termos e a política de privacidade pela API administrativa antes de liberar o cadastro público.

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

Para autenticação e auditoria:

```text
JWT_SECRET=seu_segredo_jwt
AUDITORIA_CHAVE=seu_segredo_de_auditoria
```

Esses valores são exemplos: gere as chaves com os comandos da próxima seção. O JWT exige pelo menos 32 bytes.

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
YOUTUBE_ALLOWED_CHANNEL_ID=UCxxxxxxxxxxxxxxxxxxxxxx
SUPPORT_EMAIL=email_de_atendimento
```

Utilize uma chave com acesso à YouTube Data API v3. Configure `YOUTUBE_ALLOWED_CHANNEL_ID` com o ID exato do único canal autorizado (prefixo `UC` seguido de 22 caracteres; não use o nome, @handle ou URL do canal). Na criação e edição de aulas, tanto por professores quanto por administradores, o backend consulta `status,snippet` e compara `snippet.channelId` antes de salvar. Vídeos de outros canais são rejeitados com HTTP 400. Canal ausente ou inválido bloqueia a validação com HTTP 503, sem consultar o YouTube. Após alterar a variável, reinicie o backend. A mudança vale para novos cadastros e edições; aulas já salvas não são revalidadas automaticamente. Sem ela, os fluxos que validam vídeos retornam indisponibilidade. Sem `SUPPORT_EMAIL`, o envio do formulário de suporte fica indisponível.

Para criar o primeiro administrador em um banco novo:

```text
ADMIN_EMAIL=email_do_administrador
ADMIN_PASSWORD=senha_do_administrador
```

A senha deve conter uma letra maiúscula, uma minúscula e um caractere especial, com no máximo 72 bytes em UTF-8, sem quantidade mínima de caracteres. A inicialização é executada uma única vez por banco e não promove uma conta existente. Após criar a conta, remova essas duas variáveis do ambiente. O primeiro acesso exige regularizar o aceite dos documentos; o login utiliza confirmação por e-mail.

Também é possível definir `SERVER_PORT`, cujo padrão é `8080`, `JWT_EXPIRATION`, cujo padrão é `3600000` milissegundos, ou uma hora, e `BCRYPT_STRENGTH`, cujo padrão é `12` e cujo intervalo permitido é de `10` a `16`. A variável `REQUIRE_HTTPS` controla a exigência de HTTPS e possui padrão `false`.

As configurações estão em `src/main/resources/application.properties`. Este procedimento utiliza as variáveis do Windows e dispensa um arquivo `.env`. O envio de e-mails utiliza o Resend. O backend não possui consulta de CEP pelo ViaCEP.

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

Guarde as chaves e não as publique no repositório. Se já houver dados no banco, preserve a chave de auditoria utilizada anteriormente. A troca da chave JWT invalida os tokens emitidos.

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

## Gestão de contas e termos por perfil (V3)

Plano de implementação aplicado:

1. Preservar o identificador do usuário na exclusão, com `excluido_em`, conta inativa, credenciais inutilizadas e dados pessoais anonimizados. Revogar sessões e remover tokens de e-mail na mesma transação. Preservar auditoria, aceites, cursos, publicações e demais históricos ligados à conta.
2. Liberar o e-mail pela substituição do endereço da conta excluída. Remover a verificação de carência e a coluna `bloqueada_ate`, inclusive para registros anteriores. O hash do e-mail continua servindo à regra existente de preservação de sanções, sem impedir cadastro ou convite.
3. Preservar convites anteriores, criar um registro para cada novo convite e identificar seu criador em `criado_por`. Cancelar convites sem apagá-los. Rejeitar/remover cadastros pendentes pelo mesmo fluxo de anonimização.
4. Disponibilizar documentos específicos para `PROFESSOR` e `ADMINISTRADOR` e exigir aceite explícito no convite ou primeiro acesso. Integrar a pendência ao token restrito de termos e ao filtro JWT para contas existentes.
5. Registrar cada aceite em `tb_aceites_responsabilidade`, com `user_id`, `perfil`, `termo_version`, `ip_address` e `accepted_at`. Preservar esses registros após exclusão. Validar migração PostgreSQL e fluxos de autenticação, exclusão e convites.

O nome do perfil administrador na API permanece `ADMINISTRADOR`.

Consulta pública do termo: `GET /api/politicas/responsabilidade/PROFESSOR` ou `GET /api/politicas/responsabilidade/ADMINISTRADOR`. A resposta contém `perfil`, `versao` e `conteudo`. Os documentos iniciais estão versionados em `ResponsabilidadeService`; qualquer alteração de conteúdo deve incrementar a versão, exigindo novo aceite. O conteúdo não substitui os Termos Gerais e a Política de Privacidade.

Em `POST /api/convites/completar-cadastro`, incluir no objeto `cadastro`, além dos campos existentes:

```json
{
  "aceitouResponsabilidade": true,
  "versaoResponsabilidade": "1.0"
}
```

Aceite ausente, falso ou de versão diferente da vigente retorna HTTP 400 e não cria a conta. O perfil exigido vem do convite, nunca de um campo enviado pelo cliente.

Para uma conta existente, o login retorna `TERMOS_NAO_ACEITOS` com o token restrito já utilizado pela plataforma. Consultar o documento do perfil e enviar a `POST /api/autenticacao/aceitar-termos-novamente`, usando esse token:

```json
{
  "versaoTermos": "<versão vigente dos termos gerais>",
  "aceitouResponsabilidade": true,
  "versaoResponsabilidade": "1.0"
}
```

Após HTTP 204, fazer login novamente. O IP é obtido da requisição no servidor; não é aceito como campo do corpo. Alunos mantêm o contrato anterior de aceite dos termos gerais. Uma mudança de perfil exige o aceite correspondente ao novo perfil.

A exclusão continua exigindo o código enviado por e-mail nos endpoints existentes. O e-mail fica disponível assim que a transação de exclusão termina; uma nova conta recebe outro identificador e não reutiliza sessões antigas. Contas excluídas deixam a listagem operacional de usuários; suas referências permanecem no banco para rastreabilidade. Os logs anteriores não são reescritos, preservando suas assinaturas.

A V3 é incremental e não modifica V1/V2. Ela não consegue recuperar referências apagadas antes da atualização nem identificar retroativamente criadores de convites antigos; `criado_por` é preenchido nos novos convites. A exclusão do administrador principal continua permitida; a regra existente de inicialização única não cria automaticamente um substituto.

Integração com o frontend: `POST /api/convites/consultar`, com `{ "token": "..." }`, retorna `email` e `perfil` apenas para um convite pendente e não expirado. O frontend usa esses dados para selecionar o Termo de Responsabilidade e fixar o e-mail do cadastro; o perfil enviado pelo cliente não define permissões. O token vai no corpo da requisição, seguindo o padrão de confirmação de convites.
