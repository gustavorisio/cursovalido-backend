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
    matricula VARCHAR(50) NOT NULL,
    declarou_maior_idade BOOLEAN NOT NULL,
    aceitou_termos_lgpd BOOLEAN NOT NULL,
    data_hora_aceite TIMESTAMP,
    versao_termos VARCHAR(30),
    perfil VARCHAR(30) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    excluido_em TIMESTAMP,
    email_confirmado BOOLEAN NOT NULL DEFAULT FALSE,
    status_aprovacao VARCHAR(20) NOT NULL DEFAULT 'APROVADO',
    CONSTRAINT pk_tb_usuarios PRIMARY KEY (id),
    CONSTRAINT uk_tb_usuarios_email UNIQUE (email),
    CONSTRAINT uk_tb_usuarios_matricula UNIQUE (matricula),
    CONSTRAINT ck_tb_usuarios_dados_cadastro CHECK (
        excluido_em IS NOT NULL OR perfil = 'ADMINISTRADOR' OR (
            data_hora_aceite IS NOT NULL AND versao_termos IS NOT NULL
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
    email VARCHAR(255) NOT NULL,
    perfil VARCHAR(20) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    criado_em TIMESTAMP NOT NULL,
    expira_em TIMESTAMP NOT NULL,
    email_confirmado BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL,
    criado_por BIGINT REFERENCES tb_usuarios(id),
    CONSTRAINT pk_tb_convites_acesso PRIMARY KEY (id)
);

CREATE TABLE tb_temas (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL
);

CREATE UNIQUE INDEX uk_temas_nome ON tb_temas (LOWER(nome));

CREATE TABLE tb_areas_tecnologia (
    id BIGINT PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(1000) NOT NULL,
    passos VARCHAR(1500) NOT NULL,
    area_pai_id BIGINT REFERENCES tb_areas_tecnologia(id),
    ordem INTEGER NOT NULL
);

CREATE TABLE tb_cursos (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descricao VARCHAR(5000) NOT NULL,
    professor_id BIGINT REFERENCES tb_usuarios(id) ON DELETE SET NULL,
    tema_id BIGINT REFERENCES tb_temas(id) ON DELETE SET NULL,
    area_id BIGINT REFERENCES tb_areas_tecnologia(id),
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
    bloqueada_ate TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_contas_excluidas_email ON tb_contas_excluidas(email_hash);

CREATE TABLE tb_suspensoes_usuarios (
    id BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    id_administrador BIGINT REFERENCES tb_usuarios(id) ON DELETE SET NULL,
    escopo VARCHAR(20) NOT NULL CHECK (escopo IN ('CONTA', 'FORUM', 'TOPICOS', 'COMENTARIOS')),
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

CREATE TABLE tb_noticias (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    conteudo VARCHAR(30000) NOT NULL,
    autor_id BIGINT REFERENCES tb_usuarios(id) ON DELETE SET NULL,
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_noticias_recentes ON tb_noticias(criado_em DESC, id DESC) WHERE ativo = TRUE;

CREATE TABLE tb_suporte_controle (id INTEGER PRIMARY KEY);
INSERT INTO tb_suporte_controle (id) VALUES (1);

CREATE TABLE tb_suporte_limites (
    chave VARCHAR(80) PRIMARY KEY,
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_suporte_limites_expiracao ON tb_suporte_limites (expira_em);

CREATE INDEX idx_cursos_tema ON tb_cursos(tema_id);
CREATE INDEX idx_cursos_area ON tb_cursos(area_id);

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
CREATE TABLE tb_caminhos_secoes (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    conteudo VARCHAR(20000) NOT NULL,
    ordem INTEGER NOT NULL DEFAULT 1,
    publicado BOOLEAN NOT NULL DEFAULT FALSE
);

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
