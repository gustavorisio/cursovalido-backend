# Curso Válido - Back-end

O Curso Válido é uma plataforma web gratuita que apoia adultos iniciantes no estudo autônomo de tecnologia e programação. Este repositório contém a API da plataforma, responsável pelas regras de negócio, autenticação, controle de acesso e armazenamento dos dados. A interface web fica no [repositório do front-end](https://github.com/gustavorisio/cursovalido-frontend).

- [Repositório do back-end](https://github.com/gustavorisio/cursovalido-backend)
- [Repositório do front-end](https://github.com/gustavorisio/cursovalido-frontend)

## Problema

Quem decide aprender tecnologia por conta própria, fora de uma faculdade ou de um curso pago, costuma recorrer a videoaulas gratuitas. Mesmo com muitos conteúdos disponíveis, o estudante precisa decidir sozinho o que estudar, em que ordem avançar e como verificar se realmente compreendeu o assunto. As sugestões laterais e a reprodução automática podem desviá-lo do conteúdo; aulas de autores diferentes nem sempre formam uma sequência; e assistir a um vídeo, por si só, oferece pouca prática e pouco retorno sobre o que foi aprendido.

Essas barreiras deixam o estudo passivo e dificultam a continuidade: falta um percurso organizado, oportunidades de aplicar o conteúdo e feedback que ajude a identificar o que precisa ser revisto.

## Solução proposta

O Curso Válido reúne esses elementos em um único ambiente. Organiza videoaulas em cursos, módulos e aulas sequenciais, com catálogo filtrado por temas, e registra a posição de reprodução e as aulas concluídas para que o aluno possa retomar o estudo. O vídeo é exibido na página da aula, enquanto os controles de aula anterior e próxima seguem a sequência definida pelo professor.

Para apoiar a prática e o acompanhamento da aprendizagem, a plataforma oferece questionários por módulo e uma avaliação do curso, com correção automática, explicações por questão e novas tentativas. O plano de estudos apresenta as avaliações disponíveis e o percentual de acertos da tentativa mais recente, ajudando o aluno a decidir o que revisar. A conclusão de cursos concede pontos de experiência e níveis, e o ranking torna o avanço visível. Um fórum restrito a usuários autenticados permite compartilhar dúvidas durante o percurso.

A combinação de videoaulas e atividades acompanha evidências discutidas na monografia de que estratégias de aprendizagem ativa podem favorecer retenção e compreensão em comparação com a visualização passiva ([Zhang et al., 2025](https://doi.org/10.1016/j.edurev.2025.100708)).

## Objetivo

Desenvolver uma plataforma web gratuita de apoio à aprendizagem autônoma de tecnologia por adultos iniciantes a partir de 18 anos, combinando videoaulas organizadas em cursos, módulos e aulas, acompanhamento de progresso, questionários com correção automática e explicações por questão e um fórum restrito a usuários autenticados.

## Sobre o projeto

O Curso Válido é o Projeto Final de Curso de Engenharia de Software da Universidade de Mogi das Cruzes (UMC). Alunos estudam e participam do fórum; professores organizam cursos, módulos e aulas; e administradores gerenciam acessos e operações administrativas.

O projeto está em desenvolvimento. A versão atual do back-end oferece autenticação e gestão de contas, convites, termos e políticas, cursos, módulos, aulas, matrículas, progresso, questionários, gamificação, plano de estudos, ranking de alunos, fórum, moderação, comunicados, dashboard administrativo e formulário de suporte.

## Funcionalidades

### Cadastro e acesso

O cadastro de aluno exige a declaração de que tem 18 anos ou mais, o aceite dos termos vigentes e a confirmação do e-mail; não coleta data de nascimento. O login é feito com e-mail e senha, seguido de um código enviado por e-mail. O segundo fator é obrigatório para a equipe e pode ser habilitado pelo aluno. Depois da confirmação, a API emite o JWT usado nas requisições autenticadas.

As senhas são armazenadas com BCrypt. Também há recuperação de senha e proteção contra tentativas repetidas de acesso. Professores e administradores entram por convite, com conclusão do cadastro e aprovação administrativa.

### Comunidade

O fórum permite criar tópicos sobre dúvidas de programação e responder às discussões de outros usuários. A consulta de tópicos e comentários é pública; a participação exige autenticação. A listagem de tópicos possui busca e paginação, e a edição de tópicos e comentários respeita a autoria de cada publicação.

A descrição dos tópicos e o conteúdo dos comentários têm limite de 5.000 caracteres. Também é possível fechar tópicos para novas respostas e retirar conteúdos de circulação conforme as permissões do usuário.

### Aulas e cursos

O módulo de aprendizagem organiza cursos em módulos e aulas. Os professores podem cadastrar e gerenciar os conteúdos sob sua responsabilidade, vinculando vídeos do Vimeo como serviço principal e do YouTube como alternativa. O backend valida os links do Vimeo pela conta autorizada e os vídeos do YouTube pela API, incluindo canal, disponibilidade pública ou não listada e permissão de incorporação.

Os alunos podem se matricular gratuitamente nos cursos disponíveis, salvar e retomar a posição de reprodução e marcar aulas como concluídas. A plataforma oferece questionários por módulo e avaliação do curso, com correção automática, explicação por questão e novas tentativas. O plano de estudos reúne as avaliações disponíveis e o resultado da tentativa mais recente. A conclusão de cursos concede XP e níveis, exibidos também no ranking de alunos. A reprodução e a troca entre os players Vimeo e YouTube são responsabilidade do front-end; o registro de conclusão não comprova, por si só, aprendizagem ou tempo assistido.

### Administração e suporte

A área administrativa reúne gestão de convites e aprovações, documentos legais, restrições de usuários, denúncias e moderação de cursos e aulas. O administrador pode publicar comunicados gerais para os alunos; o sistema registra a leitura individual na área de notificações. Algumas operações são exclusivas do administrador principal. O dashboard apresenta indicadores operacionais sobre usuários, cursos, aulas, matrículas e tópicos, além de matrículas por mês, cursos mais procurados e distribuição de usuários por perfil.

O formulário público de suporte gera um protocolo e encaminha a solicitação por e-mail, sujeito à configuração e aos limites de envio. O recurso não mantém um sistema completo de acompanhamento de chamados.

### Dados pessoais e termos de uso

O sistema registra a versão e a data do aceite dos documentos e permite publicar novas políticas, solicitar novo aceite, revogar o consentimento e consultar e exportar os dados pessoais disponibilizados pela API. A exclusão da conta exige confirmação por código enviado por e-mail e segue regras próprias de remoção, anonimização e retenção de registros.

As operações relevantes são registradas em auditoria com assinaturas HMAC-SHA256 encadeadas e verificação de integridade. Os JWTs são associados a sessões persistidas, permitindo verificar validade e revogar acessos.

## Tecnologias

O back-end utiliza Java 21 com Spring Boot, Spring MVC, Spring Security e Spring Data JPA. Os dados são armazenados no PostgreSQL, com migrações gerenciadas pelo Flyway.

A autenticação utiliza JWT com a biblioteca JJWT, e as entradas são validadas com Jakarta Bean Validation. O Resend é utilizado para envio de e-mails. As APIs do Vimeo e do YouTube são utilizadas para validar os vídeos das aulas. Os testes utilizam JUnit, Mockito, Spring Boot Test, Spring Security Test, H2 e PostgreSQL, com suporte a Testcontainers. O JaCoCo gera relatórios de cobertura dos módulos de fórum e aulas.

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

### 3. Preparar o banco

Para PostgreSQL local, crie o banco no pgAdmin ou em outro cliente:

No pgAdmin ou em outro cliente PostgreSQL, crie o banco:

```sql
CREATE DATABASE cursovalido;
```

Em um serviço PostgreSQL gerenciado, crie o banco pelo painel do provedor e use os dados de conexão fornecidos por ele. O Flyway aplica as migrações da aplicação durante a inicialização do backend.

Utilize um usuário com permissão para conectar ao banco e criar os objetos das migrações. O Flyway cria as tabelas na inicialização, a partir dos scripts em `src/main/resources/db/migration`. Depois, o Hibernate valida a estrutura do banco.

O Flyway aplica, em ordem, as migrações versionadas disponíveis em `src/main/resources/db/migration` durante a inicialização. Em um banco novo, todas as migrações são executadas; em um banco já existente, apenas as pendentes. O Hibernate valida a estrutura resultante. Não configure baseline manualmente para um banco novo.

### 4. Configurar as variáveis de ambiente

No Windows, pesquise **Editar as variáveis de ambiente do sistema**, clique em **Variáveis de Ambiente** e adicione os valores em **Variáveis de usuário**. Cadastre cada nome e valor separadamente, sem aspas.

Configure a conexão com PostgreSQL pelas variáveis de ambiente abaixo. Elas funcionam com banco local, VPS ou serviço gerenciado:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/banco?sslmode=require
SPRING_DATASOURCE_USERNAME=usuario_do_banco
SPRING_DATASOURCE_PASSWORD=senha_do_banco
```

Use `sslmode=require` quando o provedor exigir conexão TLS. Para validar também a identidade do servidor, use `sslmode=verify-full` e configure o certificado raiz conforme a documentação do provedor. Não coloque usuário ou senha dentro da URL: mantenha-os nas variáveis próprias. Defina também `DB_POOL_MAX_SIZE` e `DB_POOL_MIN_IDLE` para ajustar o pool de conexões quando necessário. Guarde os valores no ambiente do servidor ou em um arquivo `.env` local ignorado pelo Git; nunca os inclua no frontend nem em commits.

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
VIMEO_ALLOWED_USER_ID=123456789
VIMEO_ACCESS_TOKEN=seu_token_autenticado_vimeo
YOUTUBE_API_KEY=sua_chave_da_api_youtube
YOUTUBE_ALLOWED_CHANNEL_ID=UCabcdefghijklmnopqrstuv
SUPPORT_EMAIL=email_de_atendimento
```

Configure `VIMEO_ALLOWED_USER_ID` com o ID **numérico** do proprietário dos vídeos, sem `/users/` e sem o nome do perfil. Para obtê-lo, consulte `GET https://api.vimeo.com/me` com o token autenticado: a propriedade `uri` retorna `/users/ID`. Configure `VIMEO_ACCESS_TOKEN` com um token autenticado dessa conta, criado em https://developer.vimeo.com/apps, com os escopos de leitura `public` e `private` (para vídeos não públicos). Não são necessários escopos de edição, upload ou exclusão. Consulte as [instruções oficiais do Vimeo](https://help.vimeo.com/hc/en-us/articles/12427789081745-How-to-generate-a-personal-access-token).

As duas variáveis devem estar no ambiente do **processo do backend**: nas Variáveis de usuário do Windows em execução local ou nas configurações de ambiente/segredos do serviço de hospedagem em produção. Reinicie o backend após alterá-las. Não coloque o token no frontend, no repositório ou nas URLs. O Spring lê essas variáveis em `src/main/resources/application.properties`; arquivos `.env` não são carregados automaticamente.

Exemplo no PowerShell, válido para o terminal atual e processos iniciados nele:

```powershell
$env:VIMEO_ALLOWED_USER_ID = "123456789"
$env:VIMEO_ACCESS_TOKEN = "seu_token_autenticado_vimeo"
mvn spring-boot:run
```

Ao cadastrar ou editar aulas (professor e administrador) ou atualizar o vídeo inicial, o backend consulta o Vimeo e compara `user.uri` com a conta autorizada antes de salvar. Links `https://vimeo.com/ID`, links com hash de privacidade e `https://player.vimeo.com/video/ID?h=HASH` são aceitos. Vídeos de outras contas são recusados. Configuração ausente/inválida, falhas da API ou respostas sem proprietário confirmado bloqueiam a gravação. Deixar o vídeo inicial vazio continua permitindo removê-lo. A validação ocorre na gravação; vídeos já existentes não são auditados nem removidos automaticamente.

Utilize uma chave com acesso à YouTube Data API v3 e substitua o ID de exemplo pelo ID do canal da plataforma (formato `UC` seguido de 22 caracteres). A validação do YouTube confere se o vídeo está público ou não listado, permite incorporação e pertence a esse canal. As aulas exigem Vimeo como servidor principal e YouTube como reserva. Sem as respectivas credenciais, o cadastro/edição de aulas fica indisponível. Sem `SUPPORT_EMAIL`, o envio do formulário de suporte fica indisponível.

Para criar o primeiro administrador em um banco novo:

```text
ADMIN_EMAIL=email_do_administrador
ADMIN_PASSWORD=senha_do_administrador
```

A senha deve ter pelo menos oito caracteres, uma letra maiúscula, um caractere especial e no máximo 72 bytes em UTF-8. A inicialização é executada uma única vez por banco e não promove uma conta existente. Após criar a conta, remova essas duas variáveis do ambiente. O primeiro acesso exige regularizar o aceite dos documentos; o login utiliza confirmação por e-mail.

Também é possível definir `SERVER_PORT`, cujo padrão é `8080`, `JWT_EXPIRATION`, cujo padrão é `86400000` milissegundos, ou 24 horas, e `BCRYPT_STRENGTH`, cujo padrão é `12` e cujo intervalo permitido é de `10` a `16`. A variável `REQUIRE_HTTPS` controla a exigência de HTTPS e possui padrão `false`.

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

Para utilizar as telas da plataforma, clone o repositório do front-end em outra pasta e instale as dependências: `git clone https://github.com/gustavorisio/cursovalido-frontend.git`, `cd cursovalido-frontend` e `npm install`. Para iniciar a interface localmente, execute `npm run dev` e acesse `http://localhost:3000`. Se necessário, configure `VITE_API_URL=http://localhost:8080/api` em um arquivo `.env.local` na raiz do front-end. O backend precisa permitir a origem do front-end em `FRONTEND_ORIGINS`. O envio de e-mails pelo Resend precisa estar configurado para concluir o cadastro e o login.

## Testes

Execute na raiz do projeto:

```powershell
mvn test
```

O projeto possui testes unitários e de integração para autenticação, cadastro, administração inicial, suporte, aulas e fórum. Há verificações do dashboard nos testes compartilhados.

O perfil `test` utiliza H2 como configuração base em `src/test/resources/application-test.properties`. Parte dos testes substitui essa configuração por um PostgreSQL temporário e isolado, incluindo verificações de migrações Flyway.

Para os testes com PostgreSQL, disponibilize os binários locais ou um ambiente Docker compatível com Testcontainers. No Windows, o suporte de testes procura instalações em `C:\Program Files\PostgreSQL`; também é possível definir `TEST_POSTGRES_BIN` com o caminho da pasta `bin`. Sem binários locais, o modo automático tenta iniciar um contêiner `postgres:18-alpine`.

Para escolher explicitamente o modo de execução:

```powershell
mvn '-Dtest.postgres.mode=local' test
mvn '-Dtest.postgres.mode=container' test
```

Os bancos temporários dos testes não utilizam as credenciais de desenvolvimento. Os serviços externos são simulados nos testes correspondentes; a aprovação da suíte não confirma a configuração real de envio de e-mails ou das APIs do Vimeo e do YouTube.

Após executar os testes, os relatórios ficam em:

- `target/surefire-reports`: resultados dos testes.
- `target/site/jacoco/index.html`: cobertura do fórum.
- `target/site/jacoco-aulas/index.html`: cobertura de aulas.

## Organização e contexto acadêmico

O back-end segue uma organização em camadas, com controladores, serviços, repositórios, entidades e DTOs. Os principais pacotes são `authentication`, `aulas`, `forum` e `dashboard`. O desenvolvimento é organizado por Kanban com apoio do Asana e versionamento em Git/GitHub.

**Autor:** Gustavo Di Risio.

**Orientador:** Prof. Lucas Santos da Silva.

**Coorientador:** Prof. Alessandro Aparecido da Silva Horas.

**Instituição:** Universidade de Mogi das Cruzes - Engenharia de Software, 2026.
