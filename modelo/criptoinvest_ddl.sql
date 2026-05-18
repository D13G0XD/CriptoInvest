-- ============================================================================
-- CriptoInvest - DDL Oracle 19c+
-- Modelo relacional gerado a partir do dominio Java (com.criptoinvest.model)
--
-- Para importar no SQL Developer Data Modeler:
--   File -> Import -> DDL File -> selecione este arquivo
--   Target: Oracle Database 19c (ou superior)
--
-- Estrategia de heranca Titular -> (Usuario, Empresa):
--   "Joined Table Strategy" (tabela pai + tabelas filhas).
-- ============================================================================

-- Limpa objetos existentes (ordem reversa de dependencia)
DROP TABLE alerta            CASCADE CONSTRAINTS;
DROP TABLE participacao      CASCADE CONSTRAINTS;
DROP TABLE posicao           CASCADE CONSTRAINTS;
DROP TABLE relatorio         CASCADE CONSTRAINTS;
DROP TABLE transacao         CASCADE CONSTRAINTS;
DROP TABLE criptoativo       CASCADE CONSTRAINTS;
DROP TABLE usuario           CASCADE CONSTRAINTS;
DROP TABLE empresa           CASCADE CONSTRAINTS;
DROP TABLE titular           CASCADE CONSTRAINTS;
DROP TABLE carteira          CASCADE CONSTRAINTS;

DROP SEQUENCE seq_titular;
DROP SEQUENCE seq_carteira;
DROP SEQUENCE seq_criptoativo;
DROP SEQUENCE seq_transacao;
DROP SEQUENCE seq_relatorio;
DROP SEQUENCE seq_alerta;
DROP SEQUENCE seq_posicao;

-- ============================================================================
-- SEQUENCES
-- ============================================================================
CREATE SEQUENCE seq_titular     START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_carteira    START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_criptoativo START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_transacao   START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_relatorio   START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_alerta      START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_posicao     START WITH 1 INCREMENT BY 1 NOCACHE;

-- ============================================================================
-- CARTEIRA
--   Relacionamento 1:1 com Titular: a FK fica em Titular (id_carteira UNIQUE).
-- ============================================================================
CREATE TABLE carteira (
    id_carteira     NUMBER(10)      NOT NULL,
    descricao       VARCHAR2(100)   NOT NULL,
    CONSTRAINT pk_carteira PRIMARY KEY (id_carteira)
);

COMMENT ON TABLE  carteira              IS 'Carteira de investimentos - 1:1 com Titular';
COMMENT ON COLUMN carteira.id_carteira  IS 'PK';
COMMENT ON COLUMN carteira.descricao    IS 'Descricao livre da carteira';

-- ============================================================================
-- TITULAR (atributos compartilhados de Usuario/Empresa)
--   PK: id_titular
--   FK -> carteira (1:1, obrigatoria e unica)
-- ============================================================================
CREATE TABLE titular (
    id_titular              NUMBER(10)      NOT NULL,
    nome                    VARCHAR2(150)   NOT NULL,
    id_carteira             NUMBER(10)      NOT NULL,        -- FK -> carteira
    CONSTRAINT pk_titular PRIMARY KEY (id_titular),
    CONSTRAINT uk_titular_carteira UNIQUE (id_carteira),
    CONSTRAINT fk_titular_carteira FOREIGN KEY (id_carteira)
        REFERENCES carteira (id_carteira)
);

COMMENT ON TABLE  titular                   IS 'Tabela pai da heranca (Joined): atributos comuns de Usuario/Empresa';
COMMENT ON COLUMN titular.id_titular        IS 'PK';
COMMENT ON COLUMN titular.id_carteira       IS 'FK -> carteira(id_carteira) - 1:1 obrigatoria';

-- ============================================================================
-- USUARIO (filha de titular)
--   PK/FK: id_usuario -> titular.id_titular
-- ============================================================================
CREATE TABLE usuario (
    id_usuario              NUMBER(10)      NOT NULL,
    email                   VARCHAR2(150)   NOT NULL,
    senha                   VARCHAR2(100)   NOT NULL,
    cpf                     VARCHAR2(14)    NOT NULL,
    autenticacao_2fa        CHAR(1)         DEFAULT 'N',
    saldo_reais             NUMBER(15,2)    DEFAULT 0,
    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT uk_usuario_cpf   UNIQUE (cpf),
    CONSTRAINT ck_usuario_2fa   CHECK (autenticacao_2fa IN ('S','N')),
    CONSTRAINT fk_usuario_titular FOREIGN KEY (id_usuario)
        REFERENCES titular (id_titular)
);

COMMENT ON TABLE  usuario                   IS 'Tabela filha de Titular para Pessoa Fisica (Usuario)';
COMMENT ON COLUMN usuario.id_usuario        IS 'PK/FK -> titular(id_titular)';

-- ============================================================================
-- EMPRESA (filha de titular)
--   PK/FK: id_empresa -> titular.id_titular
-- ============================================================================
CREATE TABLE empresa (
    id_empresa              NUMBER(10)      NOT NULL,
    cnpj                    VARCHAR2(18)    NOT NULL,
    CONSTRAINT pk_empresa PRIMARY KEY (id_empresa),
    CONSTRAINT uk_empresa_cnpj UNIQUE (cnpj),
    CONSTRAINT fk_empresa_titular FOREIGN KEY (id_empresa)
        REFERENCES titular (id_titular)
);

COMMENT ON TABLE  empresa                   IS 'Tabela filha de Titular para Pessoa Juridica (Empresa)';
COMMENT ON COLUMN empresa.id_empresa        IS 'PK/FK -> titular(id_titular)';

-- ============================================================================
-- CRIPTOATIVO
-- ============================================================================
CREATE TABLE criptoativo (
    id_cripto       NUMBER(10)      NOT NULL,
    nome            VARCHAR2(80)    NOT NULL,
    sigla           VARCHAR2(10)    NOT NULL,
    preco_atual     NUMBER(18,8)    NOT NULL,
    variacao_24h    NUMBER(10,4)    DEFAULT 0,
    categoria       VARCHAR2(50),
    CONSTRAINT pk_criptoativo       PRIMARY KEY (id_cripto),
    CONSTRAINT uk_criptoativo_sigla UNIQUE (sigla),
    CONSTRAINT ck_criptoativo_preco CHECK (preco_atual >= 0)
);

COMMENT ON COLUMN criptoativo.id_cripto IS 'PK';
COMMENT ON COLUMN criptoativo.sigla     IS 'Sigla unica (BTC, ETH, etc.)';

-- ============================================================================
-- TRANSACAO (associativa em nivel de evento: Carteira x Criptoativo)
--   PK: id_transacao
--   FK -> carteira     (obrigatoria)
--   FK -> criptoativo  (obrigatoria)
-- ============================================================================
CREATE TABLE transacao (
    id_transacao    NUMBER(10)      NOT NULL,
    id_carteira     NUMBER(10)      NOT NULL,        -- FK
    id_cripto       NUMBER(10)      NOT NULL,        -- FK
    tipo            VARCHAR2(10)    NOT NULL,
    quantidade      NUMBER(18,8)    NOT NULL,
    preco_unitario  NUMBER(18,8)    NOT NULL,
    taxa            NUMBER(15,4)    NOT NULL,
    data_operacao   DATE            NOT NULL,
    CONSTRAINT pk_transacao         PRIMARY KEY (id_transacao),
    CONSTRAINT ck_transacao_tipo    CHECK (tipo IN ('COMPRA','VENDA','CONVERSAO')),
    CONSTRAINT ck_transacao_qtde    CHECK (quantidade > 0),
    CONSTRAINT fk_transacao_carteira FOREIGN KEY (id_carteira)
        REFERENCES carteira (id_carteira),
    CONSTRAINT fk_transacao_cripto   FOREIGN KEY (id_cripto)
        REFERENCES criptoativo (id_cripto)
);

COMMENT ON TABLE  transacao                 IS 'Evento de operacao - associativa Carteira x Criptoativo (nivel evento)';
COMMENT ON COLUMN transacao.id_transacao    IS 'PK';
COMMENT ON COLUMN transacao.id_carteira     IS 'FK -> carteira(id_carteira)';
COMMENT ON COLUMN transacao.id_cripto       IS 'FK -> criptoativo(id_cripto)';

CREATE INDEX idx_transacao_carteira ON transacao (id_carteira);
CREATE INDEX idx_transacao_cripto   ON transacao (id_cripto);

-- ============================================================================
-- RELATORIO
--   PK: id_relatorio
--   FK -> carteira (N:1 obrigatoria)
-- ============================================================================
CREATE TABLE relatorio (
    id_relatorio                NUMBER(10)      NOT NULL,
    id_carteira                 NUMBER(10)      NOT NULL,    -- FK
    data_geracao                DATE            NOT NULL,
    valor_total_carteira        NUMBER(18,2),
    total_investido             NUMBER(18,2),
    total_taxas                 NUMBER(15,4),
    rentabilidade_percentual    NUMBER(10,4),
    CONSTRAINT pk_relatorio PRIMARY KEY (id_relatorio),
    CONSTRAINT fk_relatorio_carteira FOREIGN KEY (id_carteira)
        REFERENCES carteira (id_carteira)
);

COMMENT ON COLUMN relatorio.id_relatorio    IS 'PK';
COMMENT ON COLUMN relatorio.id_carteira     IS 'FK -> carteira(id_carteira)';

CREATE INDEX idx_relatorio_carteira ON relatorio (id_carteira);

-- ============================================================================
-- POSICAO (entidade associativa N:N: Carteira x Criptoativo - saldo agregado)
--   PK: id_posicao
--   UK: (id_carteira, id_cripto)  -- uma posicao por par
-- ============================================================================
CREATE TABLE posicao (
    id_posicao                  NUMBER(10)      NOT NULL,
    id_carteira                 NUMBER(10)      NOT NULL,    -- FK
    id_cripto                   NUMBER(10)      NOT NULL,    -- FK
    quantidade_atual            NUMBER(18,8)    NOT NULL,
    preco_medio_compra          NUMBER(18,8)    NOT NULL,
    data_primeira_aquisicao     DATE            NOT NULL,
    data_ultima_atualizacao     DATE            NOT NULL,
    CONSTRAINT pk_posicao PRIMARY KEY (id_posicao),
    CONSTRAINT uk_posicao_carteira_cripto UNIQUE (id_carteira, id_cripto),
    CONSTRAINT ck_posicao_qtde  CHECK (quantidade_atual >= 0),
    CONSTRAINT fk_posicao_carteira FOREIGN KEY (id_carteira)
        REFERENCES carteira (id_carteira),
    CONSTRAINT fk_posicao_cripto   FOREIGN KEY (id_cripto)
        REFERENCES criptoativo (id_cripto)
);

COMMENT ON TABLE  posicao                IS 'Associativa N:N Carteira x Criptoativo (saldo agregado)';
COMMENT ON COLUMN posicao.id_posicao     IS 'PK';
COMMENT ON COLUMN posicao.id_carteira    IS 'FK -> carteira(id_carteira)';
COMMENT ON COLUMN posicao.id_cripto      IS 'FK -> criptoativo(id_cripto)';

-- ============================================================================
-- PARTICIPACAO (entidade associativa N:N: Usuario x Empresa - PK COMPOSTA)
--   PK composta: (id_usuario, id_empresa)
--   id_usuario referencia usuario.id_usuario
--   id_empresa referencia empresa.id_empresa
-- ============================================================================
CREATE TABLE participacao (
    id_usuario              NUMBER(10)      NOT NULL,        -- FK + parte da PK
    id_empresa              NUMBER(10)      NOT NULL,        -- FK + parte da PK
    percentual_participacao NUMBER(5,2)     NOT NULL,
    cargo                   VARCHAR2(40)    NOT NULL,
    data_entrada            DATE            NOT NULL,
    ativo                   CHAR(1)         DEFAULT 'S' NOT NULL,
    CONSTRAINT pk_participacao PRIMARY KEY (id_usuario, id_empresa),
    CONSTRAINT ck_part_percent CHECK (percentual_participacao BETWEEN 0 AND 100),
    CONSTRAINT ck_part_ativo   CHECK (ativo IN ('S','N')),
    CONSTRAINT ck_part_cargo   CHECK (cargo IN ('SOCIO','ADMINISTRADOR','CONTADOR','DIRETOR')),
    CONSTRAINT fk_part_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario),
    CONSTRAINT fk_part_empresa FOREIGN KEY (id_empresa)
        REFERENCES empresa (id_empresa)
);

COMMENT ON TABLE  participacao              IS 'Associativa N:N Usuario x Empresa - PK COMPOSTA';
COMMENT ON COLUMN participacao.id_usuario   IS 'PK composta + FK -> usuario(id_usuario)';
COMMENT ON COLUMN participacao.id_empresa   IS 'PK composta + FK -> empresa(id_empresa)';

CREATE INDEX idx_part_empresa ON participacao (id_empresa);

-- ============================================================================
-- ALERTA (entidade associativa N:N: Usuario x Criptoativo)
--   PK: id_alerta
--   FK -> usuario + FK -> criptoativo
-- ============================================================================
CREATE TABLE alerta (
    id_alerta           NUMBER(10)      NOT NULL,
    id_usuario          NUMBER(10)      NOT NULL,            -- FK
    id_cripto           NUMBER(10)      NOT NULL,            -- FK
    limite_variacao     NUMBER(10,4)    NOT NULL,
    ativado             CHAR(1)         DEFAULT 'S' NOT NULL,
    data_configuracao   DATE            NOT NULL,
    CONSTRAINT pk_alerta PRIMARY KEY (id_alerta),
    CONSTRAINT ck_alerta_ativado CHECK (ativado IN ('S','N')),
    CONSTRAINT ck_alerta_limite  CHECK (limite_variacao > 0),
    CONSTRAINT fk_alerta_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario),
    CONSTRAINT fk_alerta_cripto  FOREIGN KEY (id_cripto)
        REFERENCES criptoativo (id_cripto)
);

COMMENT ON TABLE  alerta                IS 'Associativa N:N Usuario x Criptoativo';
COMMENT ON COLUMN alerta.id_alerta      IS 'PK';
COMMENT ON COLUMN alerta.id_usuario     IS 'FK -> usuario(id_usuario)';
COMMENT ON COLUMN alerta.id_cripto      IS 'FK -> criptoativo(id_cripto)';

CREATE INDEX idx_alerta_usuario ON alerta (id_usuario);
CREATE INDEX idx_alerta_cripto  ON alerta (id_cripto);

-- ============================================================================
-- DADOS DE EXEMPLO (espelham o Main.java)
-- ============================================================================
-- Carteiras
INSERT INTO carteira (id_carteira, descricao) VALUES (seq_carteira.NEXTVAL, 'Carteira de Lucas');
INSERT INTO carteira (id_carteira, descricao) VALUES (seq_carteira.NEXTVAL, 'Carteira - ABCD Investimentos');

-- Usuario (titular + usuario)
INSERT INTO titular (id_titular, nome, id_carteira)
VALUES (seq_titular.NEXTVAL, 'Lucas', 1);
INSERT INTO usuario (id_usuario, email, senha, cpf, autenticacao_2fa, saldo_reais)
VALUES (1, 'lucas@email.com', 'senha123', '123.456.789-00', 'S', 15000);

-- Empresa (titular + empresa)
INSERT INTO titular (id_titular, nome, id_carteira)
VALUES (seq_titular.NEXTVAL, 'ABCD Investimentos', 2);
INSERT INTO empresa (id_empresa, cnpj)
VALUES (2, '00.000.000/0001-00');

-- Criptoativos
INSERT INTO criptoativo (id_cripto, nome, sigla, preco_atual, variacao_24h, categoria)
VALUES (seq_criptoativo.NEXTVAL, 'Bitcoin',  'BTC', 350000, 16.67, 'Moeda');
INSERT INTO criptoativo (id_cripto, nome, sigla, preco_atual, variacao_24h, categoria)
VALUES (seq_criptoativo.NEXTVAL, 'Ethereum', 'ETH', 16000,    6.67, 'Plataforma');

-- Transacoes (id_carteira=1 da carteira do Lucas)
INSERT INTO transacao (id_transacao, id_carteira, id_cripto, tipo, quantidade, preco_unitario, taxa, data_operacao)
VALUES (seq_transacao.NEXTVAL, 1, 1, 'COMPRA', 0.5, 300000, 150,  DATE '2026-05-07');
INSERT INTO transacao (id_transacao, id_carteira, id_cripto, tipo, quantidade, preco_unitario, taxa, data_operacao)
VALUES (seq_transacao.NEXTVAL, 1, 2, 'COMPRA', 2.0, 15000,  30,   DATE '2026-05-07');
INSERT INTO transacao (id_transacao, id_carteira, id_cripto, tipo, quantidade, preco_unitario, taxa, data_operacao)
VALUES (seq_transacao.NEXTVAL, 1, 1, 'VENDA',  0.1, 350000, 35,   DATE '2026-05-07');

-- Relatorio
INSERT INTO relatorio (id_relatorio, id_carteira, data_geracao, valor_total_carteira, total_investido, total_taxas, rentabilidade_percentual)
VALUES (seq_relatorio.NEXTVAL, 1, DATE '2026-05-07', 172000, 207207, 242, -16.99);

-- Alerta (associativa Usuario x Criptoativo)
INSERT INTO alerta (id_alerta, id_usuario, id_cripto, limite_variacao, ativado, data_configuracao)
VALUES (seq_alerta.NEXTVAL, 1, 1, 5.0, 'S', DATE '2026-05-07');

-- Posicao (associativa Carteira x Criptoativo)
INSERT INTO posicao (id_posicao, id_carteira, id_cripto, quantidade_atual, preco_medio_compra, data_primeira_aquisicao, data_ultima_atualizacao)
VALUES (seq_posicao.NEXTVAL, 1, 1, 0.4, 300000, DATE '2026-05-07', DATE '2026-05-07');
INSERT INTO posicao (id_posicao, id_carteira, id_cripto, quantidade_atual, preco_medio_compra, data_primeira_aquisicao, data_ultima_atualizacao)
VALUES (seq_posicao.NEXTVAL, 1, 2, 2.0, 15000,  DATE '2026-05-07', DATE '2026-05-07');

-- Participacao (associativa Usuario x Empresa - PK COMPOSTA)
INSERT INTO participacao (id_usuario, id_empresa, percentual_participacao, cargo, data_entrada, ativo)
VALUES (1, 2, 100, 'SOCIO', DATE '2026-05-07', 'S');

COMMIT;

-- ============================================================================
-- CONSULTAS DE VALIDACAO
-- ============================================================================
-- SELECT * FROM titular;
-- SELECT * FROM usuario;
-- SELECT * FROM empresa;
-- SELECT * FROM carteira;
-- SELECT * FROM criptoativo;
-- SELECT * FROM transacao;
-- SELECT * FROM posicao;
-- SELECT * FROM alerta;
-- SELECT * FROM participacao;
-- SELECT * FROM relatorio;
