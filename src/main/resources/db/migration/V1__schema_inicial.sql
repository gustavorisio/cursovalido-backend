

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
    nome_completo VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    senha VARCHAR(60) NOT NULL,
    cpf VARCHAR(52),
    cpf_hash VARCHAR(64),
    data_nascimento DATE,
    matricula VARCHAR(50) NOT NULL,
    declarou_maior_idade BOOLEAN NOT NULL,
    aceitou_termos_lgpd BOOLEAN NOT NULL,
    data_hora_aceite TIMESTAMP,
    versao_termos VARCHAR(30),
    perfil VARCHAR(30) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    email_confirmado BOOLEAN NOT NULL DEFAULT FALSE,
    status_aprovacao VARCHAR(20) NOT NULL DEFAULT 'APROVADO',
    gamertag VARCHAR(15) NOT NULL,
    CONSTRAINT pk_tb_usuarios PRIMARY KEY (id),
    CONSTRAINT uk_tb_usuarios_email UNIQUE (email),
    CONSTRAINT uk_tb_usuarios_cpf UNIQUE (cpf_hash),
    CONSTRAINT uk_tb_usuarios_matricula UNIQUE (matricula),
    CONSTRAINT uk_usuarios_gamertag UNIQUE (gamertag),
    CONSTRAINT ck_tb_usuarios_dados_cadastro CHECK (
        perfil = 'ADMINISTRADOR' OR (
            cpf IS NOT NULL AND cpf_hash IS NOT NULL AND data_nascimento IS NOT NULL
            AND data_hora_aceite IS NOT NULL AND versao_termos IS NOT NULL
        )
    )
);

CREATE TABLE tb_sessoes (
    id VARCHAR(36) PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES tb_usuarios(id),
    expira_em TIMESTAMP NOT NULL,
    revogada BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_tb_sessoes_usuario ON tb_sessoes(usuario_id);

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

CREATE TABLE tb_cursos (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descricao VARCHAR(5000) NOT NULL,
    professor_id BIGINT REFERENCES tb_usuarios(id) ON DELETE SET NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL
);
CREATE TABLE tb_modulos (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descricao VARCHAR(5000) NOT NULL,
    curso_id BIGINT NOT NULL REFERENCES tb_cursos(id),
    ordem INTEGER NOT NULL CHECK (ordem > 0),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL
);
CREATE TABLE tb_aulas (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descricao VARCHAR(5000) NOT NULL,
    modulo_id BIGINT NOT NULL REFERENCES tb_modulos(id),
    ordem INTEGER NOT NULL CHECK (ordem > 0),
    youtube_video_id VARCHAR(11) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL
);
CREATE INDEX idx_cursos_professor ON tb_cursos(professor_id);
CREATE INDEX idx_modulos_curso_ordem ON tb_modulos(curso_id, ordem, id);
CREATE INDEX idx_aulas_modulo_ordem ON tb_aulas(modulo_id, ordem, id);

CREATE TABLE tb_matriculas (
    id BIGSERIAL PRIMARY KEY,
    aluno_id BIGINT NOT NULL REFERENCES tb_usuarios(id) ON DELETE CASCADE,
    curso_id BIGINT NOT NULL REFERENCES tb_cursos(id),
    matriculado_em TIMESTAMP NOT NULL,
    CONSTRAINT uk_matricula_aluno_curso UNIQUE (aluno_id, curso_id)
);
CREATE INDEX idx_matriculas_curso ON tb_matriculas(curso_id);

CREATE TABLE tb_progresso_aulas (
    id BIGSERIAL PRIMARY KEY,
    matricula_id BIGINT NOT NULL REFERENCES tb_matriculas(id) ON DELETE CASCADE,
    aula_id BIGINT NOT NULL REFERENCES tb_aulas(id) ON DELETE CASCADE,
    posicao_segundos BIGINT NOT NULL DEFAULT 0 CHECK (posicao_segundos >= 0),
    concluida BOOLEAN NOT NULL DEFAULT FALSE,
    concluida_em TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL,
    CONSTRAINT uk_progresso_matricula_aula UNIQUE (matricula_id, aula_id)
);
CREATE INDEX idx_progresso_aula ON tb_progresso_aulas(aula_id);

CREATE TABLE tb_contas_excluidas (
    id BIGINT PRIMARY KEY,
    email_hash VARCHAR(64) NOT NULL,
    cpf_hash VARCHAR(64),
    bloqueada_ate TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_contas_excluidas_email ON tb_contas_excluidas(email_hash);
CREATE INDEX idx_contas_excluidas_cpf ON tb_contas_excluidas(cpf_hash);

CREATE TABLE tb_suspensoes_usuarios (
    id BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    id_administrador BIGINT REFERENCES tb_usuarios(id) ON DELETE SET NULL,
    escopo VARCHAR(20) NOT NULL CHECK (escopo IN ('CONTA', 'FORUM')),
    motivo VARCHAR(1000) NOT NULL,
    criada_em TIMESTAMP WITH TIME ZONE NOT NULL,
    expira_em TIMESTAMP WITH TIME ZONE,
    revogada_em TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_suspensoes_usuario ON tb_suspensoes_usuarios(id_usuario) WHERE revogada_em IS NULL;

CREATE TABLE tb_inicializacao_administrador (
    id BIGINT PRIMARY KEY CHECK (id = 1),
    concluida BOOLEAN NOT NULL DEFAULT FALSE,
    id_usuario BIGINT UNIQUE REFERENCES tb_usuarios(id) ON DELETE SET NULL,
    concluida_em TIMESTAMP WITH TIME ZONE
);

INSERT INTO tb_inicializacao_administrador (id, concluida) VALUES (1, FALSE);

CREATE TABLE tb_politicas_privacidade (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    versao VARCHAR(30) NOT NULL UNIQUE,
    conteudo TEXT NOT NULL,
    ativa BOOLEAN NOT NULL,
    data_publicacao TIMESTAMP NOT NULL
);

CREATE TABLE tb_suporte_controle (id INTEGER PRIMARY KEY);
INSERT INTO tb_suporte_controle (id) VALUES (1);

CREATE TABLE tb_suporte_limites (
    chave VARCHAR(80) PRIMARY KEY,
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_suporte_limites_expiracao ON tb_suporte_limites (expira_em);



CREATE TABLE tb_temas (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL
);

CREATE UNIQUE INDEX uk_temas_nome ON tb_temas (LOWER(nome));

ALTER TABLE tb_cursos ADD COLUMN tema_id BIGINT REFERENCES tb_temas(id) ON DELETE SET NULL;
CREATE INDEX idx_cursos_tema ON tb_cursos(tema_id);

CREATE TABLE tb_questionarios (
    id BIGSERIAL PRIMARY KEY,
    curso_id BIGINT NOT NULL REFERENCES tb_cursos(id),
    modulo_id BIGINT REFERENCES tb_modulos(id),
    titulo VARCHAR(255) NOT NULL,
    publicado BOOLEAN NOT NULL DEFAULT FALSE,
    versao INTEGER NOT NULL DEFAULT 1
);
CREATE UNIQUE INDEX uk_diagnostico_curso ON tb_questionarios(curso_id) WHERE modulo_id IS NULL;
CREATE UNIQUE INDEX uk_questionario_modulo ON tb_questionarios(modulo_id) WHERE modulo_id IS NOT NULL;

CREATE TABLE tb_questoes (
    id BIGSERIAL PRIMARY KEY,
    questionario_id BIGINT NOT NULL REFERENCES tb_questionarios(id) ON DELETE CASCADE,
    enunciado VARCHAR(1000) NOT NULL,
    explicacao VARCHAR(2000) NOT NULL,
    alternativa_correta INTEGER NOT NULL,
    aula_revisao_id BIGINT NOT NULL REFERENCES tb_aulas(id),
    ordem INTEGER NOT NULL
);
CREATE TABLE tb_alternativas_questao (
    questao_id BIGINT NOT NULL REFERENCES tb_questoes(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    texto VARCHAR(500) NOT NULL,
    PRIMARY KEY (questao_id, ordem)
);

CREATE TABLE tb_tentativas_questionario (
    id BIGSERIAL PRIMARY KEY,
    aluno_id BIGINT NOT NULL REFERENCES tb_usuarios(id) ON DELETE CASCADE,
    questionario_id BIGINT NOT NULL REFERENCES tb_questionarios(id),
    versao INTEGER NOT NULL,
    acertos INTEGER NOT NULL,
    total INTEGER NOT NULL,
    criado_em TIMESTAMP NOT NULL
);
CREATE INDEX idx_tentativas_aluno_questionario ON tb_tentativas_questionario(aluno_id, questionario_id, versao, id);
CREATE TABLE tb_respostas_questionario (
    tentativa_id BIGINT NOT NULL REFERENCES tb_tentativas_questionario(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    alternativa INTEGER NOT NULL,
    PRIMARY KEY (tentativa_id, ordem)
);

CREATE TABLE tb_caminhos_questionarios (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descricao VARCHAR(2000) NOT NULL,
    pergunta_inicial VARCHAR(50),
    versao INTEGER NOT NULL DEFAULT 1,
    publicado BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE tb_caminhos_perguntas (
    id BIGSERIAL PRIMARY KEY,
    questionario_id BIGINT NOT NULL REFERENCES tb_caminhos_questionarios(id) ON DELETE CASCADE,
    chave VARCHAR(50) NOT NULL,
    enunciado VARCHAR(1000) NOT NULL,
    ordem INTEGER NOT NULL
);
CREATE TABLE tb_caminhos_opcoes (
    pergunta_id BIGINT NOT NULL REFERENCES tb_caminhos_perguntas(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    texto VARCHAR(500) NOT NULL,
    proxima_pergunta VARCHAR(50),
    resultado VARCHAR(50),
    PRIMARY KEY (pergunta_id, ordem)
);
CREATE TABLE tb_caminhos_resultados (
    id BIGSERIAL PRIMARY KEY,
    questionario_id BIGINT NOT NULL REFERENCES tb_caminhos_questionarios(id) ON DELETE CASCADE,
    chave VARCHAR(50) NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    orientacao VARCHAR(5000) NOT NULL,
    ordem INTEGER NOT NULL
);
CREATE TABLE tb_caminhos_temas_resultado (
    resultado_id BIGINT NOT NULL REFERENCES tb_caminhos_resultados(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    tema VARCHAR(150) NOT NULL,
    PRIMARY KEY (resultado_id, ordem)
);
CREATE TABLE tb_caminhos_respostas_aluno (
    id BIGSERIAL PRIMARY KEY,
    aluno_id BIGINT NOT NULL REFERENCES tb_usuarios(id) ON DELETE CASCADE,
    questionario_id BIGINT NOT NULL REFERENCES tb_caminhos_questionarios(id),
    versao INTEGER NOT NULL,
    resultado VARCHAR(50) NOT NULL,
    atualizado_em TIMESTAMP NOT NULL,
    CONSTRAINT uk_caminhos_resposta UNIQUE (aluno_id, questionario_id)
);
CREATE TABLE tb_caminhos_escolhas_aluno (
    resposta_id BIGINT NOT NULL REFERENCES tb_caminhos_respostas_aluno(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    pergunta VARCHAR(50) NOT NULL,
    opcao INTEGER NOT NULL,
    PRIMARY KEY (resposta_id, ordem)
);
ALTER TABLE tb_usuarios DROP CONSTRAINT ck_tb_usuarios_dados_cadastro;
ALTER TABLE tb_usuarios DROP COLUMN cpf;
ALTER TABLE tb_usuarios DROP COLUMN cpf_hash;
ALTER TABLE tb_usuarios ADD CONSTRAINT ck_tb_usuarios_dados_cadastro CHECK (
    perfil = 'ADMINISTRADOR' OR (
        data_nascimento IS NOT NULL AND data_hora_aceite IS NOT NULL AND versao_termos IS NOT NULL
    )
);
ALTER TABLE tb_contas_excluidas DROP COLUMN cpf_hash;


ALTER TABLE tb_usuarios ADD COLUMN excluido_em TIMESTAMP;
ALTER TABLE tb_usuarios DROP CONSTRAINT ck_tb_usuarios_dados_cadastro;
ALTER TABLE tb_usuarios ADD CONSTRAINT ck_tb_usuarios_dados_cadastro CHECK (
    excluido_em IS NOT NULL OR perfil = 'ADMINISTRADOR' OR (
        data_nascimento IS NOT NULL AND data_hora_aceite IS NOT NULL AND versao_termos IS NOT NULL
    )
);

ALTER TABLE tb_contas_excluidas ALTER COLUMN bloqueada_ate DROP NOT NULL;

ALTER TABLE tb_convites_acesso ADD COLUMN criado_por BIGINT REFERENCES tb_usuarios(id);
ALTER TABLE tb_convites_acesso DROP CONSTRAINT tb_convites_acesso_email_key;
CREATE INDEX idx_convites_email ON tb_convites_acesso(email, id DESC);

CREATE TABLE tb_aceites_responsabilidade (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES tb_usuarios(id),
    perfil VARCHAR(30) NOT NULL CHECK (perfil IN ('PROFESSOR', 'ADMINISTRADOR')),
    termo_version VARCHAR(30) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    accepted_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_aceites_responsabilidade_usuario ON tb_aceites_responsabilidade(user_id, perfil, termo_version);


CREATE TABLE tb_versoes_documentos (
    tipo VARCHAR(30) PRIMARY KEY,
    numero BIGINT NOT NULL CHECK (numero >= 0)
);

INSERT INTO tb_versoes_documentos (tipo, numero)
SELECT 'TERMOS', COALESCE(MAX(substring(versao FROM '^([0-9]+)([.][0-9]+)?$')::BIGINT), 0)
FROM tb_politicas;
INSERT INTO tb_versoes_documentos (tipo, numero)
SELECT 'PRIVACIDADE', COALESCE(MAX(substring(versao FROM '^([0-9]+)([.][0-9]+)?$')::BIGINT), 0)
FROM tb_politicas_privacidade;

ALTER TABLE tb_politicas ADD CONSTRAINT uk_politicas_versao UNIQUE (versao);


ALTER TABLE tb_cursos ADD COLUMN capa_jpeg TEXT;


CREATE TABLE tb_comunicados (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL CHECK (char_length(trim(titulo)) > 0),
    conteudo VARCHAR(5000) NOT NULL CHECK (char_length(trim(conteudo)) > 0),
    autor_id BIGINT REFERENCES tb_usuarios(id) ON DELETE SET NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_comunicados_recentes ON tb_comunicados (criado_em DESC, id DESC);

CREATE TABLE tb_comunicados_leituras (
    comunicado_id BIGINT NOT NULL REFERENCES tb_comunicados(id) ON DELETE CASCADE,
    usuario_id BIGINT NOT NULL REFERENCES tb_usuarios(id) ON DELETE CASCADE,
    lido_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (usuario_id, comunicado_id)
);


ALTER TABLE tb_questoes ALTER COLUMN aula_revisao_id DROP NOT NULL;


ALTER TABLE tb_caminhos_questionarios ALTER COLUMN descricao TYPE VARCHAR(2500);
ALTER TABLE tb_caminhos_perguntas ALTER COLUMN enunciado TYPE VARCHAR(2500);
ALTER TABLE tb_caminhos_opcoes ALTER COLUMN texto TYPE VARCHAR(1500);


CREATE TABLE tb_cadastro_vagas (
    chave VARCHAR(48) PRIMARY KEY
);

INSERT INTO tb_cadastro_vagas (chave)
SELECT endereco_ip || ':' || CAST(vaga AS VARCHAR)
FROM (
    SELECT endereco_ip, COUNT(DISTINCT usuario_id) AS total
    FROM tb_logs_auditoria
    WHERE tipo_acao IN ('CADASTRO_USUARIO', 'CADASTRO_PENDENTE_APROVACAO')
      AND endereco_ip IS NOT NULL AND endereco_ip <> ''
    GROUP BY endereco_ip
) cadastros
CROSS JOIN (VALUES (1), (2)) AS vagas(vaga)
WHERE cadastros.total >= vagas.vaga;


UPDATE tb_versoes_documentos
SET numero = (
    SELECT GREATEST(COALESCE(MAX(FLOOR(substring(versao FROM '^v?([0-9]+[.]?[0-9]*)$')::NUMERIC * 5)), 0) - 4, 0)::BIGINT
    FROM tb_politicas
)
WHERE tipo = 'TERMOS';


DROP TABLE IF EXISTS tb_caminhos_escolhas_aluno;
DROP TABLE IF EXISTS tb_caminhos_respostas_aluno;
DROP TABLE IF EXISTS tb_caminhos_temas_resultado;
DROP TABLE IF EXISTS tb_caminhos_resultados;
DROP TABLE IF EXISTS tb_caminhos_opcoes;
DROP TABLE IF EXISTS tb_caminhos_perguntas;
DROP TABLE IF EXISTS tb_caminhos_questionarios;
DROP TABLE IF EXISTS tb_cadastro_vagas;


ALTER TABLE tb_usuarios DROP CONSTRAINT ck_tb_usuarios_dados_cadastro;
UPDATE tb_usuarios SET data_nascimento = NULL WHERE perfil <> 'ALUNO';
ALTER TABLE tb_usuarios ADD CONSTRAINT ck_tb_usuarios_dados_cadastro CHECK (
    excluido_em IS NOT NULL OR perfil = 'ADMINISTRADOR' OR (
        data_hora_aceite IS NOT NULL AND versao_termos IS NOT NULL
        AND (perfil <> 'ALUNO' OR data_nascimento IS NOT NULL)
    )
);
ALTER TABLE tb_usuarios ADD CONSTRAINT ck_tb_usuarios_idade_somente_aluno CHECK (
    perfil = 'ALUNO' OR data_nascimento IS NULL
);

CREATE TABLE tb_vinculos_responsaveis (
    id BIGSERIAL PRIMARY KEY,
    aluno_id BIGINT NOT NULL UNIQUE REFERENCES tb_usuarios(id) ON DELETE CASCADE,
    responsavel_id BIGINT REFERENCES tb_usuarios(id) ON DELETE SET NULL,
    nome_responsavel VARCHAR(120) NOT NULL,
    email_responsavel VARCHAR(254) NOT NULL,
    token_hash VARCHAR(64) UNIQUE,
    token_expira_em TIMESTAMP,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDENTE', 'CONFIRMADO', 'REVOGADO')),
    acesso_forum VARCHAR(20) NOT NULL DEFAULT 'SOMENTE_LEITURA'
        CHECK (acesso_forum IN ('LIBERADO', 'SOMENTE_LEITURA', 'BLOQUEADO')),
    criado_em TIMESTAMP NOT NULL,
    confirmado_em TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL
);
CREATE INDEX idx_vinculos_responsavel ON tb_vinculos_responsaveis(responsavel_id, status);

CREATE TABLE tb_gamificacao_alunos (
    aluno_id BIGINT PRIMARY KEY REFERENCES tb_usuarios(id) ON DELETE CASCADE,
    nivel INTEGER NOT NULL DEFAULT 1 CHECK (nivel >= 1),
    xp_nivel BIGINT NOT NULL DEFAULT 0 CHECK (xp_nivel >= 0),
    xp_total BIGINT NOT NULL DEFAULT 0 CHECK (xp_total >= 0),
    atualizado_em TIMESTAMP NOT NULL
);

CREATE TABLE tb_conclusoes_cursos (
    id BIGSERIAL PRIMARY KEY,
    aluno_id BIGINT NOT NULL REFERENCES tb_usuarios(id) ON DELETE CASCADE,
    curso_id BIGINT NOT NULL REFERENCES tb_cursos(id) ON DELETE CASCADE,
    xp_concedido INTEGER NOT NULL CHECK (xp_concedido >= 0),
    concluido_em TIMESTAMP NOT NULL,
    CONSTRAINT uk_conclusao_aluno_curso UNIQUE (aluno_id, curso_id)
);
CREATE INDEX idx_conclusoes_curso ON tb_conclusoes_cursos(curso_id);
CREATE INDEX idx_progresso_concluida_em ON tb_progresso_aulas(concluida_em) WHERE concluida;
CREATE INDEX idx_matriculas_aluno ON tb_matriculas(aluno_id);

UPDATE tb_forum_users SET email = CASE
    WHEN position('@' IN email) > 1 THEN substring(email FROM 1 FOR 1) || '*******' || substring(email FROM position('@' IN email))
    ELSE '*******'
END
WHERE email NOT LIKE '_*******@%';

UPDATE tb_forum_users SET name = CASE
    WHEN position(' ' IN btrim(name)) > 0
        THEN split_part(btrim(name), ' ', 1) || ' ' || upper(left(regexp_replace(btrim(name), '^.*\s', ''), 1)) || '.'
    ELSE btrim(name)
END
WHERE role = 'ALUNO';



CREATE TABLE tb_configuracoes_supervisao (
    aluno_id BIGINT PRIMARY KEY REFERENCES tb_usuarios(id) ON DELETE CASCADE,
    acesso_forum VARCHAR(20) NOT NULL DEFAULT 'SOMENTE_LEITURA'
        CHECK (acesso_forum IN ('LIBERADO', 'SOMENTE_LEITURA', 'BLOQUEADO')),
    limite_diario_minutos INTEGER CHECK (limite_diario_minutos BETWEEN 15 AND 720),
    alterada_por VARCHAR(20) NOT NULL CHECK (alterada_por IN ('SISTEMA', 'ALUNO', 'RESPONSAVEL')),
    atualizada_em TIMESTAMP NOT NULL
);

INSERT INTO tb_configuracoes_supervisao (aluno_id, acesso_forum, limite_diario_minutos, alterada_por, atualizada_em)
SELECT aluno_id, acesso_forum, 120,
       CASE WHEN status = 'CONFIRMADO' THEN 'RESPONSAVEL' ELSE 'SISTEMA' END, CURRENT_TIMESTAMP
FROM tb_vinculos_responsaveis;

INSERT INTO tb_configuracoes_supervisao (aluno_id, acesso_forum, limite_diario_minutos, alterada_por, atualizada_em)
SELECT id, 'SOMENTE_LEITURA', 120, 'SISTEMA', CURRENT_TIMESTAMP
FROM tb_usuarios
WHERE perfil = 'ALUNO' AND excluido_em IS NULL AND data_nascimento > current_date - interval '18 years'
ON CONFLICT (aluno_id) DO NOTHING;

ALTER TABLE tb_vinculos_responsaveis DROP COLUMN acesso_forum;

CREATE TABLE tb_uso_diario (
    aluno_id BIGINT NOT NULL REFERENCES tb_usuarios(id) ON DELETE CASCADE,
    dia DATE NOT NULL,
    segundos BIGINT NOT NULL DEFAULT 0 CHECK (segundos >= 0),
    ultima_atividade_em TIMESTAMP NOT NULL,
    CONSTRAINT pk_tb_uso_diario PRIMARY KEY (aluno_id, dia)
);

UPDATE tb_usuarios SET status_aprovacao = 'APROVADO', ativo = TRUE
WHERE perfil = 'ALUNO' AND status_aprovacao = 'PENDENTE_RESPONSAVEL' AND excluido_em IS NULL
  AND data_nascimento <= current_date - interval '17 years';

UPDATE tb_usuarios SET status_aprovacao = 'REJEITADO', ativo = FALSE
WHERE perfil = 'ALUNO' AND status_aprovacao = 'PENDENTE_RESPONSAVEL'
  AND data_nascimento > current_date - interval '17 years';

CREATE TABLE tb_verificacoes_idade (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL UNIQUE REFERENCES tb_usuarios(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('PENDENTE', 'VERIFICADA', 'SUSPEITA', 'EM_CONTESTACAO', 'REPROVADA')),
    metodo VARCHAR(20) NOT NULL CHECK (metodo IN ('AUTODECLARACAO', 'PROVEDOR_EXTERNO', 'ANALISE_MANUAL')),
    provedor VARCHAR(60),
    referencia_externa VARCHAR(120),
    motivo VARCHAR(500),
    id_suspensao BIGINT REFERENCES tb_suspensoes_usuarios(id) ON DELETE SET NULL,
    token_contestacao_hash VARCHAR(64) UNIQUE,
    token_contestacao_expira_em TIMESTAMP,
    justificativa_contestacao VARCHAR(2000),
    contestada_em TIMESTAMP,
    id_administrador_decisao BIGINT REFERENCES tb_usuarios(id) ON DELETE SET NULL,
    decidida_em TIMESTAMP,
    criada_em TIMESTAMP NOT NULL,
    atualizada_em TIMESTAMP NOT NULL
);
CREATE INDEX idx_verificacoes_idade_status ON tb_verificacoes_idade(status, atualizada_em);

UPDATE tb_sessoes SET revogada = TRUE
WHERE usuario_id IN (
    SELECT id FROM tb_usuarios
    WHERE perfil = 'ALUNO' AND ativo AND excluido_em IS NULL
      AND data_nascimento > current_date - interval '17 years'
);

WITH abaixo AS (
    SELECT id FROM tb_usuarios
    WHERE perfil = 'ALUNO' AND ativo AND excluido_em IS NULL
      AND data_nascimento > current_date - interval '17 years'
), suspensas AS (
    INSERT INTO tb_suspensoes_usuarios (id_usuario, escopo, motivo, criada_em)
    SELECT id, 'CONTA', 'Verificação de idade: cadastro abaixo da idade mínima de 17 anos.', CURRENT_TIMESTAMP
    FROM abaixo
    RETURNING id, id_usuario
)
INSERT INTO tb_verificacoes_idade (usuario_id, status, metodo, motivo, id_suspensao, criada_em, atualizada_em)
SELECT id_usuario, 'SUSPEITA', 'AUTODECLARACAO', 'Cadastro abaixo da idade mínima de 17 anos.', id,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM suspensas;

INSERT INTO tb_verificacoes_idade (usuario_id, status, metodo, motivo, criada_em, atualizada_em)
SELECT id, 'PENDENTE', 'AUTODECLARACAO', 'Idade autodeclarada aguardando confirmacao', CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM tb_usuarios
WHERE perfil = 'ALUNO' AND excluido_em IS NULL
ON CONFLICT (usuario_id) DO NOTHING;

CREATE TABLE tb_denuncias_forum (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('TOPICO', 'COMENTARIO')),
    conteudo_id BIGINT NOT NULL,
    autor_conteudo_id BIGINT NOT NULL,
    denunciante_id BIGINT NOT NULL REFERENCES tb_usuarios(id) ON DELETE CASCADE,
    motivo VARCHAR(30) NOT NULL CHECK (motivo IN ('OFENSA', 'ASSEDIO', 'DISCRIMINACAO', 'CONTEUDO_IMPROPRIO',
        'DADOS_PESSOAIS', 'SPAM', 'OUTRO')),
    descricao VARCHAR(1000),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDENTE', 'PROCEDENTE', 'IMPROCEDENTE')),
    criada_em TIMESTAMP NOT NULL,
    moderador_id BIGINT REFERENCES tb_usuarios(id) ON DELETE SET NULL,
    decidida_em TIMESTAMP,
    observacao_moderacao VARCHAR(1000),
    conteudo_removido BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_denuncia_conteudo_denunciante UNIQUE (tipo, conteudo_id, denunciante_id)
);
CREATE INDEX idx_denuncias_forum_status ON tb_denuncias_forum(status, criada_em);
CREATE INDEX idx_denuncias_forum_conteudo ON tb_denuncias_forum(tipo, conteudo_id, status);



DELETE FROM tb_tokens_email
WHERE usuario_id IN (SELECT id FROM tb_usuarios WHERE perfil = 'RESPONSAVEL');

UPDATE tb_sessoes SET revogada = TRUE
WHERE usuario_id IN (SELECT id FROM tb_usuarios WHERE perfil = 'RESPONSAVEL');

UPDATE tb_usuarios
SET perfil = 'ALUNO',
    ativo = FALSE,
    email_confirmado = FALSE,
    excluido_em = COALESCE(excluido_em, CURRENT_TIMESTAMP),
    nome_completo = 'Usuario excluido',
    email = 'excluido-' || id || '@conta.invalid',
    senha = '',
    data_nascimento = NULL,
    matricula = 'excluido-' || id
WHERE perfil = 'RESPONSAVEL';

UPDATE tb_usuarios SET status_aprovacao = 'REJEITADO', ativo = FALSE
WHERE status_aprovacao = 'PENDENTE_RESPONSAVEL';

UPDATE tb_sessoes SET revogada = TRUE
WHERE usuario_id IN (
    SELECT id FROM tb_usuarios
    WHERE perfil = 'ALUNO' AND ativo AND excluido_em IS NULL
      AND data_nascimento > current_date - interval '18 years'
);

INSERT INTO tb_suspensoes_usuarios (id_usuario, escopo, motivo, criada_em)
SELECT u.id, 'CONTA', 'O Curso Válido é destinado somente a pessoas com 18 anos completos ou mais.', CURRENT_TIMESTAMP
FROM tb_usuarios u
WHERE u.perfil = 'ALUNO' AND u.ativo AND u.excluido_em IS NULL
  AND u.data_nascimento > current_date - interval '18 years'
  AND NOT EXISTS (
      SELECT 1 FROM tb_suspensoes_usuarios s
      WHERE s.id_usuario = u.id AND s.escopo = 'CONTA' AND s.expira_em IS NULL AND s.revogada_em IS NULL
  );

DROP TABLE IF EXISTS tb_uso_diario;
DROP TABLE IF EXISTS tb_configuracoes_supervisao;
DROP TABLE IF EXISTS tb_verificacoes_idade;
DROP TABLE IF EXISTS tb_vinculos_responsaveis;

ALTER TABLE tb_convites_acesso ADD COLUMN enviado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tb_convites_acesso ADD COLUMN quantidade_envios INTEGER NOT NULL DEFAULT 1;
UPDATE tb_convites_acesso SET enviado_em = criado_em;
ALTER TABLE tb_convites_acesso ADD CONSTRAINT ck_convites_quantidade_envios CHECK (quantidade_envios >= 1);

UPDATE tb_convites_acesso c SET status = 'CANCELADO'
WHERE c.status = 'PENDENTE'
  AND EXISTS (
      SELECT 1 FROM tb_convites_acesso mais_recente
      WHERE mais_recente.email = c.email AND mais_recente.status = 'PENDENTE' AND mais_recente.id > c.id
  );

CREATE UNIQUE INDEX uk_convites_pendente_por_email ON tb_convites_acesso(email) WHERE status = 'PENDENTE';
CREATE INDEX idx_convites_criado_por ON tb_convites_acesso(criado_por, criado_em);


ALTER TABLE tb_aulas
    ADD COLUMN vimeo_video_url VARCHAR(2048);

ALTER TABLE tb_aulas
    ALTER COLUMN youtube_video_id DROP NOT NULL;

ALTER TABLE tb_aulas
    ADD CONSTRAINT ck_aulas_video_configurado
    CHECK (vimeo_video_url IS NOT NULL OR youtube_video_id IS NOT NULL);


ALTER TABLE tb_denuncias_forum
    DROP CONSTRAINT IF EXISTS uk_denuncia_conteudo_denunciante;

CREATE INDEX IF NOT EXISTS idx_denuncias_usuario_autor_data
    ON tb_denuncias_forum (denunciante_id, autor_conteudo_id, criada_em);

CREATE INDEX IF NOT EXISTS idx_topics_autor_data
    ON tb_topics (author_id, created_at);


ALTER TABLE tb_usuarios ADD COLUMN dois_fatores_ativos BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE tb_usuarios ALTER COLUMN dois_fatores_ativos SET DEFAULT FALSE;


ALTER TABLE tb_usuarios DROP CONSTRAINT IF EXISTS ck_tb_usuarios_dados_cadastro;
ALTER TABLE tb_usuarios DROP CONSTRAINT IF EXISTS ck_tb_usuarios_idade_somente_aluno;

ALTER TABLE tb_usuarios DROP COLUMN IF EXISTS data_nascimento;

ALTER TABLE tb_usuarios ADD CONSTRAINT ck_tb_usuarios_dados_cadastro CHECK (
    excluido_em IS NOT NULL OR perfil = 'ADMINISTRADOR' OR (
        data_hora_aceite IS NOT NULL AND versao_termos IS NOT NULL
    )
);


CREATE TABLE tb_video_inicio (
    id SMALLINT PRIMARY KEY CHECK (id = 1),
    vimeo_url VARCHAR(2048)
);


DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM tb_usuarios) THEN
        DELETE FROM tb_politicas;
        DELETE FROM tb_politicas_privacidade;
        UPDATE tb_versoes_documentos SET numero = 0 WHERE tipo IN ('TERMOS', 'PRIVACIDADE');
    END IF;
END;
$$;
