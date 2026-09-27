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
