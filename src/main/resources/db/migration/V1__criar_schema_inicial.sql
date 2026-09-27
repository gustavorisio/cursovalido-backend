CREATE TABLE tb_forum_users (
    id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_tb_forum_users PRIMARY KEY (id)
);

CREATE TABLE tb_topics (
    id BIGSERIAL NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    author_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    closed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_tb_topics PRIMARY KEY (id),
    CONSTRAINT fk_tb_topics_author FOREIGN KEY (author_id) REFERENCES tb_forum_users (id)
);

CREATE TABLE tb_comments (
    id BIGSERIAL NOT NULL,
    content VARCHAR(5000) NOT NULL,
    author_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    topic_id BIGINT NOT NULL,
    CONSTRAINT pk_tb_comments PRIMARY KEY (id),
    CONSTRAINT fk_tb_comments_author FOREIGN KEY (author_id) REFERENCES tb_forum_users (id),
    CONSTRAINT fk_tb_comments_topic FOREIGN KEY (topic_id) REFERENCES tb_topics (id)
);

CREATE INDEX idx_tb_topics_created_at ON tb_topics (created_at);
CREATE INDEX idx_tb_comments_topic_id ON tb_comments (topic_id);

CREATE TABLE tb_usuarios (
    id BIGSERIAL NOT NULL,
    nome_completo VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    senha VARCHAR(60) NOT NULL,
    cpf VARCHAR(11) NOT NULL,
    data_nascimento DATE NOT NULL,
    telefone VARCHAR(30) NOT NULL,
    matricula VARCHAR(50) NOT NULL,
    cep VARCHAR(8) NOT NULL,
    logradouro VARCHAR(255) NOT NULL,
    bairro VARCHAR(255) NOT NULL,
    cidade VARCHAR(255) NOT NULL,
    estado VARCHAR(2) NOT NULL,
    declarou_maior_idade BOOLEAN NOT NULL,
    aceitou_termos_lgpd BOOLEAN NOT NULL,
    data_hora_aceite TIMESTAMP NOT NULL,
    versao_termos VARCHAR(30) NOT NULL,
    perfil VARCHAR(30) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    email_confirmado BOOLEAN NOT NULL DEFAULT FALSE,
    status_aprovacao VARCHAR(20) NOT NULL DEFAULT 'APROVADO',
    CONSTRAINT pk_tb_usuarios PRIMARY KEY (id),
    CONSTRAINT uk_tb_usuarios_email UNIQUE (email),
    CONSTRAINT uk_tb_usuarios_cpf UNIQUE (cpf),
    CONSTRAINT uk_tb_usuarios_matricula UNIQUE (matricula)
);

CREATE TABLE tb_logs_auditoria (
    id BIGSERIAL NOT NULL,
    usuario_id BIGINT,
    tipo_acao VARCHAR(80) NOT NULL,
    detalhes VARCHAR(2000),
    endereco_ip VARCHAR(45),
    data_hora TIMESTAMP NOT NULL,
        hash_anterior VARCHAR(64),
        assinatura VARCHAR(64),
    CONSTRAINT pk_tb_logs_auditoria PRIMARY KEY (id),
    CONSTRAINT fk_tb_logs_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuarios (id)
);

CREATE TABLE tb_tokens_email (
    id BIGSERIAL NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    usuario_id BIGINT NOT NULL,
    finalidade VARCHAR(30) NOT NULL,
    expira_em TIMESTAMP NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    utilizado BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_tb_tokens_email PRIMARY KEY (id),
    CONSTRAINT uk_tb_tokens_email_hash UNIQUE (token_hash),
    CONSTRAINT fk_tb_tokens_email_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuarios (id) ON DELETE CASCADE
);
CREATE INDEX idx_tb_tokens_email_finalidade ON tb_tokens_email (finalidade);
CREATE INDEX idx_tb_logs_auditoria_data_hora ON tb_logs_auditoria (data_hora);

CREATE TABLE tb_politicas (
    id BIGSERIAL NOT NULL,
    nome VARCHAR(100) NOT NULL,
    versao VARCHAR(30) NOT NULL,
    conteudo TEXT NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    data_publicacao TIMESTAMP NOT NULL,
    CONSTRAINT pk_tb_politicas PRIMARY KEY (id)
);

CREATE TABLE tb_aceites_termos_arquivados (
    id BIGSERIAL NOT NULL,
    usuario_id_original BIGINT NOT NULL,
    versao VARCHAR(30) NOT NULL,
    data_hora_aceite TIMESTAMP NOT NULL,
    data_hora_arquivamento TIMESTAMP NOT NULL,
    motivo VARCHAR(40) NOT NULL,
    CONSTRAINT pk_tb_aceites_termos_arquivados PRIMARY KEY (id)
);

CREATE TABLE tb_convites_acesso (
    id BIGSERIAL NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    perfil VARCHAR(20) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    criado_em TIMESTAMP NOT NULL,
    expira_em TIMESTAMP NOT NULL,
    email_confirmado BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT pk_tb_convites_acesso PRIMARY KEY (id)
);

INSERT INTO tb_politicas (nome, versao, conteudo, ativa, data_publicacao)
VALUES
    ('Termo de Aceite', '1.0', $$<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Termos de Aceite - Curso Valido</title>
</head>
<body>
    <h1>Termo de Aceite do Curso Valido</h1>
    <p><strong>Versao 1.0</strong></p>
    <h2>1. Objeto</h2>
    <p>Este termo define as regras para uso da plataforma educacional Curso Valido, incluindo cadastro, autenticacao, controle de acesso, forum, comunicacoes por e-mail e auditoria. O aceite e necessario para criar e utilizar uma conta.</p>
    <h2>2. Plataforma gratuita e elegibilidade</h2>
    <p>O Curso Valido e uma plataforma totalmente gratuita. Nao ha venda de cursos, assinatura, mensalidade, cobranca, pagamento ou reembolso associado ao uso descrito neste termo. O acesso gratuito nao elimina as regras de seguranca, os requisitos de cadastro ou as responsabilidades do usuario.</p>
    <p>A plataforma e destinada exclusivamente a pessoas com 18 anos ou mais. Menores de 18 anos nao podem criar conta, utilizar as funcionalidades autenticadas ou participar do forum. No cadastro, o usuario informa sua data de nascimento e declara expressamente ser maior de idade. O sistema valida a idade informada e rejeita cadastros que nao atendam a esse requisito. Se a administracao identificar uma conta de menor de idade, ela podera suspender o acesso e excluir os dados conforme a legislacao aplicavel.</p>
    <h2>3. Declaracoes do usuario</h2>
    <p>Ao criar ou utilizar uma conta, o usuario declara que forneceu informacoes verdadeiras, completas e atuais, que e maior de 18 anos e que informara a administracao sobre qualquer alteracao relevante em seus dados.</p>
    <h2>4. Dados e privacidade</h2>
    <p>Este Termo apresenta diretamente os dados e usos essenciais para o aceite. No cadastro, o sistema coleta: nome completo, e-mail, senha, CPF, data de nascimento, telefone, CEP, logradouro, bairro, cidade, estado, declaracao de maioridade e aceite dos documentos. Durante o uso, tambem trata matricula, perfil, status da conta, confirmacao de e-mail, versao/data/hora do aceite, tokens de confirmacao e 2FA, IP, registros de auditoria e topicos e comentarios publicados no forum.</p>
    <p>Esses dados sao utilizados diretamente para criar a conta, verificar que o usuario tem 18 anos ou mais, autenticar o acesso, enviar e-mails de confirmacao, redefinicao de senha e 2FA, aplicar permissoes, disponibilizar o forum, consultar o endereco pelo ViaCEP, proteger o sistema e cumprir obrigacoes legais. O Resend recebe somente o e-mail e o conteudo necessario para as mensagens transacionais. O projeto nao coleta dados de pagamento, nao vende dados pessoais e nao usa os dados para publicidade direcionada.</p>
    <p>As senhas e tokens nao sao armazenados em texto puro. O aceite registra a versao, a data e a hora; tokens possuem prazo de expiracao. Os dados sao mantidos enquanto necessarios para a conta, seguranca, auditoria ou obrigacao legal, podendo ser removidos ou anonimizados quando a conta for excluida. O titular pode solicitar confirmacao, acesso, correcao, atualizacao, anonimização, eliminacao quando aplicavel, informacoes sobre compartilhamento, portabilidade quando regulamentada e revogacao do consentimento pelo canal oficial do Curso Valido.</p>
    <h2>5. Aceite e registro</h2>
    <p>O usuario declara que leu este Termo e esta ciente dos dados pessoais coletados, das finalidades, do tratamento realizado, do compartilhamento com servicos necessarios e dos direitos do titular. O aceite registra a versao, a data e a hora no sistema.</p>
    <h2>6. Conta e seguranca</h2>
    <p>O usuario deve manter sua senha em sigilo, nao compartilhar credenciais e comunicar suspeitas de acesso indevido. O sistema pode utilizar confirmacao de e-mail, codigo de dupla verificacao, JWT e regras de permissao para proteger a conta.</p>
    <h2>7. Uso da plataforma e do forum</h2>
    <p>O usuario deve utilizar a plataforma para finalidades educacionais, respeitar os demais participantes e publicar somente conteudo licito, verdadeiro e relacionado ao forum. E proibido tentar acessar contas, dados ou funcionalidades sem autorizacao, explorar falhas ou prejudicar a disponibilidade do sistema.</p>
    <h2>8. Suspensao e encerramento</h2>
    <p>O acesso pode ser suspenso ou encerrado em caso de dados falsos, violacao deste termo, uso indevido, rejeicao administrativa ou revogacao do aceite. A revogacao dos termos pode impedir o uso de funcionalidades que dependem do tratamento dos dados.</p>
    <h2>9. Atualizacao do termo</h2>
    <p>Uma nova versao pode ser publicada para refletir alteracoes no sistema, na finalidade do tratamento ou na legislacao. A versao aceita deve corresponder a uma politica ativa publicada pelo Curso Valido. Quando necessario, o sistema solicitara novo aceite e registrara a versao antes de liberar o acesso.</p>
    <h2>10. Direitos e contato</h2>
    <p>O usuario pode exercer os direitos descritos neste Termo e solicitar esclarecimentos a administracao do Curso Valido pelo canal oficial da instituicao.</p>
</body>
</html>$$, TRUE, CURRENT_TIMESTAMP),
    ('Politica de Privacidade', '1.0', $$<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Politica de Privacidade - Curso Valido</title>
</head>
<body>
    <h1>Politica de Privacidade do Curso Valido</h1>
    <p><strong>Versao 1.0</strong></p>
    <h2>1. Responsavel e abrangencia</h2>
    <p>Esta politica explica como a administracao do projeto educacional Curso Valido trata dados pessoais de alunos, professores, administradores e pessoas convidadas para criar uma conta. O canal oficial de atendimento da instituicao responsavel pelo curso e utilizado para pedidos de privacidade e direitos do titular.</p>
    <h2>2. Dados pessoais coletados</h2>
    <p><strong>Dados:</strong> nome completo e e-mail.</p><p><strong>Origem e funcionalidade:</strong> formulario de cadastro, login, convite e recuperacao de conta.</p><p><strong>Finalidade e acesso:</strong> identificar a conta e enviar confirmacao, redefinicao de senha e codigo 2FA. O e-mail e acessado pela administracao autorizada e pelo servico de envio Resend.</p><p><strong>Retencao:</strong> enquanto a conta existir e pelo periodo necessario para obrigacoes e auditoria.</p>
    <p><strong>Dados:</strong> CPF, data de nascimento e declaracao de maioridade.</p><p><strong>Origem e funcionalidade:</strong> formulario de cadastro e validacoes de identidade e idade.</p><p><strong>Finalidade e acesso:</strong> evitar contas duplicadas, verificar o requisito de 18 anos e atender regras de acesso. Administradores autorizados podem consultar esses dados.</p><p><strong>Retencao:</strong> enquanto necessarios a conta, seguranca, auditoria ou obrigacao legal.</p>
    <p><strong>Dados:</strong> telefone.</p><p><strong>Origem e funcionalidade:</strong> formulario de cadastro.</p><p><strong>Finalidade e acesso:</strong> manter o cadastro e permitir contato institucional quando necessario. Nao e usado para publicidade.</p><p><strong>Retencao:</strong> enquanto mantido no cadastro ou ate pedido de exclusao, ressalvadas obrigacoes legais.</p>
    <p><strong>Dados:</strong> CEP, logradouro, bairro, cidade e estado.</p><p><strong>Origem e funcionalidade:</strong> endereco informado no cadastro e consulta do CEP ao ViaCEP.</p><p><strong>Finalidade e acesso:</strong> completar e validar o endereco do usuario. O ViaCEP recebe o CEP consultado.</p><p><strong>Retencao:</strong> enquanto fizerem parte do cadastro ou forem necessarios para a finalidade informada.</p>
    <p><strong>Dados:</strong> hash BCrypt da senha.</p><p><strong>Origem e funcionalidade:</strong> cadastro e login.</p><p><strong>Finalidade e acesso:</strong> autenticar o usuario. A senha original nao e armazenada; o hash BCrypt ja contem o salt necessario a verificacao.</p><p><strong>Retencao:</strong> enquanto a conta estiver ativa, com exclusao quando deixar de ser necessaria.</p>
    <p><strong>Dados:</strong> matricula, perfil, status, conta ativa e e-mail confirmado.</p><p><strong>Origem e funcionalidade:</strong> gerados ou atualizados pelo cadastro, aprovacao e administracao.</p><p><strong>Finalidade e acesso:</strong> aplicar RBAC e liberar as funcionalidades de aluno, professor ou administrador.</p><p><strong>Retencao:</strong> enquanto a conta e os registros de responsabilizacao forem necessarios.</p>
    <p><strong>Dados:</strong> versao, data e hora do aceite dos termos e da Politica.</p><p><strong>Origem e funcionalidade:</strong> checkboxes de aceite no cadastro e aceite de nova versao.</p><p><strong>Finalidade e acesso:</strong> comprovar qual documento foi apresentado e aceito. A administracao consulta esses registros para governanca.</p><p><strong>Retencao:</strong> pelo tempo necessario para comprovar o aceite e cumprir obrigacoes.</p>
    <p><strong>Dados:</strong> tokens de e-mail e codigo 2FA.</p><p><strong>Origem e funcionalidade:</strong> confirmacao de e-mail, redefinicao de senha, convite e login em duas etapas.</p><p><strong>Finalidade e acesso:</strong> validar uma operacao especifica. O banco armazena somente hash do token/codigo, com expiracao e uso unico.</p><p><strong>Retencao:</strong> ate expiracao, utilizacao ou invalidacao; depois deixam de ser validos.</p>
    <p><strong>Dados:</strong> IP, data, hora, tipo da operacao e detalhes da auditoria.</p><p><strong>Origem e funcionalidade:</strong> requisicoes de cadastro, login, forum, politicas e operacoes administrativas.</p><p><strong>Finalidade e acesso:</strong> prevenir fraude, investigar incidentes e demonstrar a integridade das operacoes. Acesso restrito a administradores.</p><p><strong>Retencao:</strong> pelo prazo necessario para seguranca, auditoria e exercicio regular de direitos.</p>
    <p><strong>Dados:</strong> topicos, comentarios, nome, perfil e identificador do autor.</p><p><strong>Origem e funcionalidade:</strong> publicacoes realizadas no forum.</p><p><strong>Finalidade e acesso:</strong> disponibilizar a comunidade de aprendizagem, exibir autoria e permitir moderacao.</p><p><strong>Retencao:</strong> enquanto a publicacao e os registros de moderacao forem necessarios; exclusoes podem ser logicas.</p>
    <p>Por ser gratuito, o projeto nao solicita dados de pagamento e nao usa os dados acima para venda de produtos ou publicidade direcionada.</p>
    <h2>3. Finalidades do tratamento</h2>
    <ul><li>criar, identificar e administrar contas de usuarios;</li><li>autenticar o acesso, enviar confirmacoes, redefinicoes de senha e codigo de dupla verificacao;</li><li>aplicar perfis e permissoes de aluno, professor e administrador;</li><li>verificar a maioridade obrigatoria, o aceite dos termos e os requisitos de cadastro;</li><li>disponibilizar o forum e vincular publicacoes aos seus autores;</li><li>consultar dados de endereco a partir do CEP informado;</li><li>prevenir uso indevido, investigar incidentes e manter auditoria das operacoes;</li><li>atender obrigacoes legais e solicitacoes do titular.</li></ul>
    <h2>4. Bases legais</h2>
    <p>Conforme a finalidade, o tratamento se apoia na execucao das funcionalidades educacionais solicitadas, no cumprimento de obrigacoes legais, no exercicio regular de direitos, na protecao da seguranca do sistema e, quando aplicavel, no consentimento manifestado pelo aceite. A versao, a data e a hora do aceite sao registradas para comprovar a escolha feita pelo usuario.</p>
    <h2>5. Como o tratamento e realizado</h2>
    <p>O sistema coleta os dados enviados nos formularios, valida as informacoes, grava os registros no banco PostgreSQL, atualiza o status da conta conforme as regras de aprovacao e usa os dados para executar as funcionalidades descritas nesta politica. A senha nao e gravada em texto puro: o sistema utiliza BCrypt, que armazena o hash com o salt incorporado.</p>
    <h2>6. Compartilhamento e servicos externos</h2>
    <p>O CEP informado pode ser enviado a API publica ViaCEP exclusivamente para retornar logradouro, bairro, cidade e estado. O sistema utiliza o Resend para enviar confirmacao, convite, redefinicao de senha e autenticacao 2FA; para isso, compartilha o e-mail do destinatario e o conteudo da mensagem. O Curso Valido nao vende dados pessoais. Compartilhamentos legais podem ocorrer quando exigidos por autoridade competente.</p>
    <h2>7. Retencao, seguranca e exclusao</h2>
    <p>Os dados permanecem enquanto a conta e as finalidades do sistema estiverem ativas ou enquanto houver necessidade legal ou de auditoria. O titular pode solicitar a exclusao da conta; quando a exclusao ocorrer, registros necessarios para comprovar o aceite ou a auditoria podem ser preservados ou anonimizados conforme a finalidade e a obrigacao aplicavel. O acesso e protegido por JWT, controle de permissoes e registros de auditoria. Tokens deixam de ser validos quando expiram ou sao utilizados. Dados que nao precisarem ser mantidos serao removidos ou anonimizados quando a conta for excluida.</p>
    <h2>8. Direitos do titular</h2>
    <p>Nos termos da LGPD e observadas as hipoteses legais, o titular pode solicitar confirmacao da existencia de tratamento, acesso aos dados, correcao, atualizacao, anonimização, bloqueio ou eliminacao de dados desnecessarios, informacoes sobre compartilhamento, portabilidade quando regulamentada, revogacao do consentimento e revisao de decisoes tomadas exclusivamente de forma automatizada, quando aplicavel.</p>
    <h2>9. Como exercer os direitos</h2>
    <p>Solicitacoes devem ser encaminhadas a administracao do Curso Valido pelo canal informado pela instituicao, com identificacao suficiente para evitar atendimento a pessoa incorreta. A solicitacao sera analisada e respondida conforme a legislacao aplicavel.</p>
    <h2>10. Atualizacoes</h2>
    <p>A versao vigente permanece acessivel nesta politica. Quando uma alteracao exigir novo aceite, o sistema solicitara a confirmacao antes de liberar as funcionalidades correspondentes.</p>
</body>
</html>$$, TRUE, CURRENT_TIMESTAMP);
