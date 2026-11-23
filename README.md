# Curso Válido - Front-end

Interface web do **Curso Válido**, plataforma gratuita de apoio à aprendizagem autônoma de tecnologia e programação por adultos iniciantes. O projeto é o Trabalho de Conclusão de Curso do Bacharelado em Engenharia de Software da Universidade de Mogi das Cruzes (UMC).

Quem aprende por conta própria costuma recorrer a videoaulas gratuitas, mas precisa definir sozinho o que estudar, em que ordem avançar e como verificar se compreendeu o conteúdo. Sugestões de vídeos podem desviar a atenção, aulas de autores diferentes nem sempre formam um percurso, e a visualização passiva oferece pouca prática e retorno sobre o aprendizado.

O Curso Válido reúne cursos, módulos e aulas em sequência, registra a reprodução e a conclusão das aulas e oferece questionários com correção e explicações. O player permite alternar entre Vimeo e YouTube; o plano de estudos mostra os resultados das avaliações; XP, níveis, ranking e fórum apoiam a continuidade dos estudos e a troca de dúvidas.

Este repositório contém a interface React e os clientes HTTP da plataforma. A API do back-end aplica as regras de negócio, controla o acesso, persiste os dados e realiza as integrações com os serviços externos.

- [Repositório do front-end](https://github.com/gustavorisio/cursovalido-frontend)
- [Repositório do back-end](https://github.com/gustavorisio/cursovalido-backend)

## Funcionalidades implementadas

### Cadastro e conta

- Cadastro de alunos com nome, e-mail e senha, declaração de maioridade (18 anos ou mais) e aceite dos documentos legais. O formulário atual não coleta CPF nem data de nascimento.
- Confirmação de e-mail, login com código enviado por e-mail quando o segundo fator está habilitado, recuperação e redefinição de senha. O segundo fator é obrigatório para professores e administradores e opcional para alunos.
- Cadastro por convite para professores e administradores, com confirmação de e-mail e aprovação administrativa.
- Consulta e exportação dos dados pessoais, histórico e revogação do aceite dos termos e solicitação de exclusão da conta mediante código enviado por e-mail. A autoexclusão não está disponível para contas administrativas.
- Controle de sessão e proteção de rotas conforme os perfis `ALUNO`, `PROFESSOR` e `ADMINISTRADOR`.

### Cursos e aulas

- Catálogo público e área do aluno com matrícula gratuita e consulta dos cursos em que está matriculado.
- Organização em cursos, módulos e aulas, com pesquisa de conteúdo.
- Área do professor para cadastrar, editar e excluir cursos, módulos e aulas.
- Cadastro de aulas com vídeo Vimeo como servidor principal e YouTube como reserva. O player permite alternar entre os provedores e retomar a aula da posição salva.
- Salvamento periódico da posição de reprodução e retomada da aula a partir do progresso registrado.
- Conclusão manual da aula pelo botão **Marcar como concluída**, com atualização do progresso do curso. A conclusão registrada não comprova, por si só, tempo assistido ou aprendizagem.
- Área administrativa para consultar, editar, arquivar e reabrir cursos e aulas.
- Questionários por módulo, com correção automática, explicações por questão, resultados e novas tentativas. O plano de estudos apresenta as avaliações disponíveis e o percentual de acertos da tentativa mais recente. Acertos de 100% podem conceder XP uma única vez por questionário; o aluno acompanha níveis e ranking.

### Fórum

- Listagem e consulta de tópicos e comentários, com paginação.
- Criação de tópicos e comentários por usuários autenticados, respeitando as restrições da conta.
- Títulos com até 255 caracteres; descrições e comentários com até 5.000 caracteres.
- Edição de tópicos e comentários pelo próprio autor.
- Fechamento de tópicos pelo autor ou administrador.
- Arquivamento por administradores ou pelo professor autor do tópico.
- Exclusão de tópicos por administradores e de comentários pelo autor ou administrador.

### Suporte

- Formulário público de suporte com validação dos campos, protocolo e encaminhamento por e-mail por meio da API.

### Administração

- Dashboard com indicadores e gráficos.
- Consulta de usuários e aplicação ou revogação de suspensões. A exclusão administrativa de contas de alunos, professores e outros administradores é exclusiva do root; a conta root continua protegida contra exclusão.
- Convites e aprovação de cadastros da equipe pelo administrador principal.
- Gestão dos termos de uso e da política de privacidade pelo administrador principal.
- Publicação de comunicados para os alunos, com título de até 255 caracteres e conteúdo de até 5.000 caracteres. Administradores podem editar e excluir comunicados.

### Notificações de comunicados

- Administradores publicam em **Administração → Comunicados aos alunos** (`/administracao/comunicados`), preenchendo somente título e conteúdo.
- Administradores podem editar qualquer comunicado publicado, mantendo os limites de título e conteúdo e as leituras já registradas. A exclusão exige confirmação na interface e remove o comunicado e seus registros de leitura.
- Alunos acessam os comunicados pelo sino no cabeçalho (`/aluno/notificacoes`), com contador de notificações não lidas atualizado a cada minuto e ao retornar à página.
- Abrir **Ler comunicado** registra a leitura no servidor somente para o aluno autenticado. Consultar a lista não marca automaticamente todos os comunicados como lidos.
- Os comunicados são gerais e permanecem disponíveis para alunos que ingressarem posteriormente. Esta versão não envia e-mails nem notificações do navegador.
- A API do backend utiliza `GET/POST /api/administracao/comunicados`, `PUT/DELETE /api/administracao/comunicados/{id}`, `GET /api/aluno/notificacoes`, `GET /api/aluno/notificacoes/nao-lidas` e `PUT /api/aluno/notificacoes/{id}/lida`. As listas aceitam `page` e `size` (de 1 a 50) e retornam `itens`, `pagina`, `totalPaginas`, `totalElementos` e `naoLidas`.
- Ícone [Bell do Bootstrap Icons v1.13.1](https://icons.getbootstrap.com/icons/bell/), usado localmente sob licença MIT, incluída em `public/imagens/LICENCA-bootstrap-icons.txt`.

As operações dependem da API do back-end, responsável por validar permissões, persistir os dados e executar integrações como envio de e-mails pelo Resend e validação dos vídeos pelas APIs do Vimeo e do YouTube. As credenciais dessas integrações ficam no ambiente do backend, não no frontend.

## Tecnologias

| Tecnologia | Uso |
| --- | --- |
| React 19 e React DOM | Interface em JavaScript/JSX |
| React Router DOM 7 | Navegação da SPA e organização de rotas |
| Vite 8 | Servidor de desenvolvimento e build |
| Tailwind CSS 4 e CSS próprio | Estilização da interface |
| CoreUI React 5 | Componentes da área administrativa |
| CoreUI React Chart.js | Gráficos do dashboard |
| Fetch API | Comunicação HTTP com o back-end |
| ESLint 10 | Análise estática do código |
| Vitest, Istanbul e jsdom | Testes automatizados e cobertura |

## Como executar localmente

### Pré-requisitos

- **Node.js** compatível com as dependências. Para a linha 22, utilize **22.22.2 ou superior**; para a linha 24, **24.15.0 ou superior**, considerando também o jsdom usado nos testes.
- **npm**, normalmente incluído na instalação do Node.js, para instalar as dependências e executar os scripts.
- **Git**, para clonar o repositório pelos comandos abaixo. Também é possível baixar e extrair o ZIP do projeto pelo GitHub.
- **Navegador atualizado**, para acessar a interface.
- Acesso à internet para instalar as dependências, utilizar as integrações e carregar os vídeos do Vimeo e do YouTube.

Confira as instalações no terminal:

```bash
node --version
npm --version
git --version
```

React, Vite, Tailwind CSS, CoreUI, Chart.js e as demais bibliotecas são instalados localmente pelo npm a partir do `package.json`. Não precisam de instalação global.

### Requisitos da plataforma completa

Para utilizar os fluxos integrados, mantenha o back-end em execução e configure a API conforme a próxima seção. Segundo a arquitetura descrita na monografia, o ambiente do back-end utiliza:

- **JDK 21** para executar a aplicação Java com Spring Boot.
- **Maven** para construir e executar o back-end.
- **PostgreSQL**, com banco e credenciais configurados; as migrações são gerenciadas pelo Flyway no back-end.
- Configuração no backend das integrações **Resend** (e-mails de confirmação, login, recuperação e suporte), **Vimeo** (validação do proprietário dos vídeos) e **YouTube Data API** (validação e reserva dos vídeos). As credenciais dos serviços externos são mantidas no backend.

Esses requisitos pertencem ao servidor. Se você utilizar uma API já hospedada, não precisa instalar Java, Maven ou PostgreSQL na máquina do front-end. As credenciais dos serviços externos devem ser configuradas no back-end.

Consulte os comandos de inicialização, variáveis de ambiente e requisitos específicos no [repositório do back-end](https://github.com/gustavorisio/cursovalido-backend).

### Instalação

```bash
git clone https://github.com/gustavorisio/cursovalido-frontend.git
cd cursovalido-frontend
npm install
```

O comando `npm install` instala as dependências de desenvolvimento e da aplicação em `node_modules/`. Para uma instalação reproduzível a partir do `package-lock.json`, como em integração contínua, utilize `npm ci` como alternativa.

### Configuração da API

Crie um arquivo `.env.local` na raiz do projeto, se precisar definir o endereço da API:

```dotenv
VITE_API_URL=http://localhost:8080/api
```

Esse também é o endereço padrão quando a variável não está definida. Informe a URL base com o prefixo `/api` e reinicie o servidor de desenvolvimento após alterar a configuração.

O cliente HTTP utiliza tokens de sessão no cabeçalho `Authorization: Bearer ...` nas requisições autenticadas. O back-end deve permitir a origem do front-end na configuração de CORS.

### Inicialização

```bash
npm run dev
```

Acesse [http://localhost:3000](http://localhost:3000). A porta está fixada em `3000` com `strictPort: true` no `vite.config.js`: se estiver ocupada, o Vite encerra a inicialização em vez de escolher outra porta.

## Scripts e verificações

| Comando | Descrição |
| --- | --- |
| `npm install` | Instala as dependências do projeto |
| `npm ci` | Instala as versões do lockfile em um ambiente limpo |
| `npm run dev` | Inicia o servidor de desenvolvimento na porta 3000 |
| `npm run build` | Gera os arquivos de produção em `dist/` |
| `npm run preview` | Disponibiliza uma prévia local do build de produção |
| `npm run lint` | Executa a análise estática com ESLint |
| `npm test` | Executa os testes automatizados com Vitest |
| `npm run test:coverage` | Gera o relatório Istanbul e verifica os mínimos de cobertura configurados no Vitest |

### Cobertura de testes

A cobertura é verificada pela configuração nativa do Vitest em `vitest.config.js`, com o provedor Istanbul. O comando `npm run test:coverage` exige no mínimo 75% de linhas cobertas no projeto e 50% em cada um dos seis arquivos com regra específica. Não há limite máximo de cobertura.

Os mínimos por arquivo se aplicam ao player de aulas, à tabela de ranking, ao vídeo de apresentação, à página de ranking, à moderação de denúncias e ao formulário de denúncia. O relatório HTML fica em `coverage/index.html`, e o relatório JSON em `coverage/coverage-final.json`.

Para explicar à banca: “O Vitest executa os testes e usa o Istanbul para medir a cobertura. Os percentuais mínimos ficam na configuração do próprio Vitest. Se a cobertura ficar abaixo do mínimo, o comando retorna uma falha.” A configuração usa a opção oficial [coverage.thresholds do Vitest](https://vitest.dev/config/coverage#coverage-thresholds).

Para visualizar o build localmente:

```bash
npm run build
npm run preview
```

## Principais rotas

| Área | Rotas |
| --- | --- |
| Conteúdo público | `/`, `/cursos`, `/forum`, `/forum/:idTopico`, `/suporte` |
| Autenticação | `/login`, `/cadastro`, `/convites/cadastro`, `/confirmacao-email` |
| Recuperação de senha | `/solicitar-alteracao-senha`, `/redefinir-senha` |
| Documentos legais | `/termos`, `/termos-aceite`, `/privacidade` |
| Conta autenticada | `/configuracoes`, `/configuracoes/alterar-senha`, `/notificacoes`, `/ranking` |
| Aluno | `/aluno/cursos`, `/aluno/cursos/:idCurso`, `/aluno/cursos/:idCurso/aulas/:idAula`, `/aluno/cursos/:idCurso/modulos/:idModulo/questionario` |
| Professor | `/professor/cursos`, `/professor/cursos/:idCurso/modulos`, `/professor/cursos/:idCurso/modulos/:idModulo/aulas`, `/professor/cursos/:idCurso/modulos/:idModulo/questionario` |
| Administração | `/administracao`, `/administracao/dashboard`, `/administracao/comunicados`, `/administracao/termos`, `/administracao/privacidade`, `/administracao/usuarios`, `/administracao/forum/denuncias`, `/administracao/convites`, `/administracao/inicio`, `/administracao/cursos` |

As rotas de conta autenticada exigem login. As rotas de aluno, professor e administração exigem o perfil correspondente. A rota `/aluno/notificacoes` redireciona para `/notificacoes`.

## Estrutura do projeto

```text
src/
├── assets/          # Imagens e recursos visuais
├── componentes/     # Componentes comuns e módulos de aulas, fórum, suporte e administração
├── contextos/       # Autenticação e restrições da conta
├── paginas/         # Páginas de autenticação, conta e administração
├── servicos/        # Cliente HTTP, sessão, validações e serviços compartilhados
├── App.jsx          # Rotas e composição principal
├── index.css        # Estilos globais
└── main.jsx         # Entrada da aplicação
tests/               # Testes automatizados
```

## Publicação

O build gera uma SPA estática em `dist/`. Configure `VITE_API_URL` antes de executar `npm run build`, pois as variáveis do Vite são incorporadas ao bundle. O servidor de hospedagem deve encaminhar as rotas da aplicação para `index.html`, permitindo acesso direto às URLs gerenciadas pelo `BrowserRouter`.

## Autoria

**Desenvolvido por:** Gustavo Di Risio

**Orientador:** Lucas Santos da Silva

**Coorientador:** Alessandro Aparecido da Silva Horas

**Instituição:** Universidade de Mogi das Cruzes (UMC) - Bacharelado em Engenharia de Software.
