-- ============================================================================
-- CriptoInvest - DDL Oracle 19c+
-- Modelo relacional gerado a partir do dominio Java (com.criptoinvest.model)
--
-- Estrategia de heranca em CARTEIRA (joined):
--   carteira (pai)  ->  carteira_pf | carteira_pj  (filhas)
--
-- Relacionamentos chave:
--   usuario (1) <-> (1) carteira_pf            -- 1:1 obrigatorio
--   usuario (1)  -> (N) empresa                -- 1:N (usuario possui empresas)
--   empresa (1) <-> (1) carteira_pj            -- 1:1 obrigatorio
--   transacao/relatorio/posicao/alerta -> id_carteira (pai, polimorfico)
-- ============================================================================

-- Limpa objetos existentes (ordem reversa de dependencia)
DROP TABLE alerta            CASCADE CONSTRAINTS;
DROP TABLE posicao           CASCADE CONSTRAINTS;
DROP TABLE relatorio         CASCADE CONSTRAINTS;
DROP TABLE transacao         CASCADE CONSTRAINTS;
DROP TABLE criptoativo       CASCADE CONSTRAINTS;
DROP TABLE empresa           CASCADE CONSTRAINTS;
DROP TABLE usuario           CASCADE CONSTRAINTS;
DROP TABLE carteira_pj       CASCADE CONSTRAINTS;
DROP TABLE carteira_pf       CASCADE CONSTRAINTS;
DROP TABLE carteira          CASCADE CONSTRAINTS;

DROP SEQUENCE seq_carteira;
DROP SEQUENCE seq_usuario;
DROP SEQUENCE seq_empresa;
DROP SEQUENCE seq_criptoativo;
DROP SEQUENCE seq_transacao;
DROP SEQUENCE seq_relatorio;
DROP SEQUENCE seq_alerta;
DROP SEQUENCE seq_posicao;

-- ============================================================================
-- SEQUENCES
-- ============================================================================
CREATE SEQUENCE seq_carteira    START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_usuario     START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_empresa     START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_criptoativo START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_transacao   START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_relatorio   START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_alerta      START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_posicao     START WITH 1 INCREMENT BY 1 NOCACHE;

-- ============================================================================
-- CARTEIRA (tabela pai da heranca joined)
--   Contem atributos comuns: descricao, saldo em reais, discriminador 'tipo'
-- ============================================================================
CREATE TABLE carteira (
    id_carteira     NUMBER(10)      NOT NULL,
    descricao       VARCHAR2(100)   NOT NULL,
    saldo_reais     NUMBER(15,2)    DEFAULT 0 NOT NULL,
    tipo            CHAR(2)         NOT NULL,
    CONSTRAINT pk_carteira          PRIMARY KEY (id_carteira),
    CONSTRAINT ck_carteira_tipo     CHECK (tipo IN ('PF','PJ')),
    CONSTRAINT ck_carteira_saldo    CHECK (saldo_reais >= 0)
);

COMMENT ON TABLE  carteira              IS 'Tabela pai da heranca (Joined): atributos comuns das carteiras PF e PJ';
COMMENT ON COLUMN carteira.tipo         IS 'Discriminador da heranca: PF ou PJ';

-- ============================================================================
-- CARTEIRA_PF (filha - pessoa fisica)
-- ============================================================================
CREATE TABLE carteira_pf (
    id_carteira_pf          NUMBER(10)      NOT NULL,
    limite_diario_saque     NUMBER(15,2)    DEFAULT 5000 NOT NULL,
    CONSTRAINT pk_carteira_pf       PRIMARY KEY (id_carteira_pf),
    CONSTRAINT ck_carteira_pf_lim   CHECK (limite_diario_saque >= 0),
    CONSTRAINT fk_carteira_pf       FOREIGN KEY (id_carteira_pf)
        REFERENCES carteira (id_carteira)
);

COMMENT ON TABLE  carteira_pf                   IS 'Carteira de Pessoa Fisica - filha de carteira';
COMMENT ON COLUMN carteira_pf.id_carteira_pf    IS 'PK/FK -> carteira(id_carteira)';

-- ============================================================================
-- CARTEIRA_PJ (filha - pessoa juridica)
-- ============================================================================
CREATE TABLE carteira_pj (
    id_carteira_pj      NUMBER(10)      NOT NULL,
    regime_tributario   VARCHAR2(30)    DEFAULT 'SIMPLES' NOT NULL,
    CONSTRAINT pk_carteira_pj           PRIMARY KEY (id_carteira_pj),
    CONSTRAINT ck_carteira_pj_regime    CHECK (regime_tributario IN ('SIMPLES','LUCRO_PRESUMIDO','LUCRO_REAL')),
    CONSTRAINT fk_carteira_pj           FOREIGN KEY (id_carteira_pj)
        REFERENCES carteira (id_carteira)
);

COMMENT ON TABLE  carteira_pj                   IS 'Carteira de Pessoa Juridica - filha de carteira';
COMMENT ON COLUMN carteira_pj.id_carteira_pj    IS 'PK/FK -> carteira(id_carteira)';

-- ============================================================================
-- USUARIO
--   1:1 obrigatorio com carteira_pf (UK em id_carteira_pf)
-- ============================================================================
CREATE TABLE usuario (
    id_usuario          NUMBER(10)      NOT NULL,
    id_carteira_pf      NUMBER(10)      NOT NULL,
    nome                VARCHAR2(150)   NOT NULL,
    email               VARCHAR2(150)   NOT NULL,
    senha               VARCHAR2(100)   NOT NULL,
    cpf                 VARCHAR2(14)    NOT NULL,
    autenticacao_2fa    CHAR(1)         DEFAULT 'N' NOT NULL,
    CONSTRAINT pk_usuario           PRIMARY KEY (id_usuario),
    CONSTRAINT uk_usuario_email     UNIQUE (email),
    CONSTRAINT uk_usuario_cpf       UNIQUE (cpf),
    CONSTRAINT uk_usuario_carteira  UNIQUE (id_carteira_pf),
    CONSTRAINT ck_usuario_2fa       CHECK (autenticacao_2fa IN ('S','N')),
    CONSTRAINT fk_usuario_carteira  FOREIGN KEY (id_carteira_pf)
        REFERENCES carteira_pf (id_carteira_pf)
);

-- ============================================================================
-- EMPRESA
--   1:N com usuario (dono): id_usuario obrigatorio
--   1:1 obrigatorio com carteira_pj (UK em id_carteira_pj)
-- ============================================================================
CREATE TABLE empresa (
    id_empresa          NUMBER(10)      NOT NULL,
    id_usuario          NUMBER(10)      NOT NULL,
    id_carteira_pj      NUMBER(10)      NOT NULL,
    nome                VARCHAR2(150)   NOT NULL,
    cnpj                VARCHAR2(18)    NOT NULL,
    CONSTRAINT pk_empresa           PRIMARY KEY (id_empresa),
    CONSTRAINT uk_empresa_cnpj      UNIQUE (cnpj),
    CONSTRAINT uk_empresa_carteira  UNIQUE (id_carteira_pj),
    CONSTRAINT fk_empresa_usuario   FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario),
    CONSTRAINT fk_empresa_carteira  FOREIGN KEY (id_carteira_pj)
        REFERENCES carteira_pj (id_carteira_pj)
);

CREATE INDEX idx_empresa_usuario ON empresa (id_usuario);

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

-- ============================================================================
-- TRANSACAO
--   FK -> carteira (polimorfica: pode ser PF ou PJ)
-- ============================================================================
CREATE TABLE transacao (
    id_transacao    NUMBER(10)      NOT NULL,
    id_carteira     NUMBER(10)      NOT NULL,
    id_cripto       NUMBER(10)      NOT NULL,
    tipo            VARCHAR2(10)    NOT NULL,
    quantidade      NUMBER(18,8)    NOT NULL,
    preco_unitario  NUMBER(18,8)    NOT NULL,
    taxa            NUMBER(15,4)    NOT NULL,
    data_operacao   DATE            NOT NULL,
    CONSTRAINT pk_transacao         PRIMARY KEY (id_transacao),
    CONSTRAINT ck_transacao_tipo    CHECK (tipo IN ('COMPRA','VENDA','CONVERSAO')),
    CONSTRAINT ck_transacao_qtde    CHECK (quantidade > 0),
    CONSTRAINT ck_transacao_preco   CHECK (preco_unitario >= 0),
    CONSTRAINT ck_transacao_taxa    CHECK (taxa >= 0),
    CONSTRAINT fk_transacao_carteira FOREIGN KEY (id_carteira)
        REFERENCES carteira (id_carteira),
    CONSTRAINT fk_transacao_cripto   FOREIGN KEY (id_cripto)
        REFERENCES criptoativo (id_cripto)
);

CREATE INDEX idx_transacao_carteira ON transacao (id_carteira);
CREATE INDEX idx_transacao_cripto   ON transacao (id_cripto);

-- ============================================================================
-- RELATORIO
-- ============================================================================
CREATE TABLE relatorio (
    id_relatorio                NUMBER(10)      NOT NULL,
    id_carteira                 NUMBER(10)      NOT NULL,
    data_geracao                DATE            NOT NULL,
    valor_total_carteira        NUMBER(18,2),
    total_investido             NUMBER(18,2),
    total_taxas                 NUMBER(15,4),
    rentabilidade_percentual    NUMBER(10,4),
    CONSTRAINT pk_relatorio PRIMARY KEY (id_relatorio),
    CONSTRAINT fk_relatorio_carteira FOREIGN KEY (id_carteira)
        REFERENCES carteira (id_carteira)
);

CREATE INDEX idx_relatorio_carteira ON relatorio (id_carteira);

-- ============================================================================
-- POSICAO (associativa N:N: Carteira x Criptoativo - saldo agregado)
-- ============================================================================
CREATE TABLE posicao (
    id_posicao                  NUMBER(10)      NOT NULL,
    id_carteira                 NUMBER(10)      NOT NULL,
    id_cripto                   NUMBER(10)      NOT NULL,
    quantidade_atual            NUMBER(18,8)    NOT NULL,
    preco_medio_compra          NUMBER(18,8)    NOT NULL,
    data_primeira_aquisicao     DATE            NOT NULL,
    data_ultima_atualizacao     DATE            NOT NULL,
    CONSTRAINT pk_posicao PRIMARY KEY (id_posicao),
    CONSTRAINT uk_posicao_carteira_cripto UNIQUE (id_carteira, id_cripto),
    CONSTRAINT ck_posicao_qtde  CHECK (quantidade_atual >= 0),
    CONSTRAINT ck_posicao_preco CHECK (preco_medio_compra >= 0),
    CONSTRAINT fk_posicao_carteira FOREIGN KEY (id_carteira)
        REFERENCES carteira (id_carteira),
    CONSTRAINT fk_posicao_cripto   FOREIGN KEY (id_cripto)
        REFERENCES criptoativo (id_cripto)
);

-- ============================================================================
-- ALERTA (associativa N:N: Usuario x Criptoativo)
-- ============================================================================
CREATE TABLE alerta (
    id_alerta           NUMBER(10)      NOT NULL,
    id_usuario          NUMBER(10)      NOT NULL,
    id_cripto           NUMBER(10)      NOT NULL,
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

CREATE INDEX idx_alerta_usuario ON alerta (id_usuario);
CREATE INDEX idx_alerta_cripto  ON alerta (id_cripto);

-- ============================================================================
-- DADOS DE EXEMPLO (espelham o Main.java)
-- ============================================================================
-- Carteira PF do Lucas (id=1)
INSERT INTO carteira    (id_carteira, descricao, saldo_reais, tipo) VALUES (seq_carteira.NEXTVAL, 'Carteira PF de Lucas', 15000, 'PF');
INSERT INTO carteira_pf (id_carteira_pf, limite_diario_saque)       VALUES (1, 5000);

-- Carteira PJ da ABCD (id=2)
INSERT INTO carteira    (id_carteira, descricao, saldo_reais, tipo) VALUES (seq_carteira.NEXTVAL, 'Carteira PJ - ABCD Investimentos', 0, 'PJ');
INSERT INTO carteira_pj (id_carteira_pj, regime_tributario)         VALUES (2, 'SIMPLES');

-- Usuario
INSERT INTO usuario (id_usuario, id_carteira_pf, nome, email, senha, cpf, autenticacao_2fa)
VALUES (seq_usuario.NEXTVAL, 1, 'Lucas', 'lucas@email.com', 'senha123', '123.456.789-00', 'S');

-- Empresa (pertence ao Usuario Lucas)
INSERT INTO empresa (id_empresa, id_usuario, id_carteira_pj, nome, cnpj)
VALUES (seq_empresa.NEXTVAL, 1, 2, 'ABCD Investimentos', '00.000.000/0001-00');

-- Criptoativos
INSERT INTO criptoativo (id_cripto, nome, sigla, preco_atual, variacao_24h, categoria)
VALUES (seq_criptoativo.NEXTVAL, 'Bitcoin',  'BTC', 350000, 16.67, 'Moeda');
INSERT INTO criptoativo (id_cripto, nome, sigla, preco_atual, variacao_24h, categoria)
VALUES (seq_criptoativo.NEXTVAL, 'Ethereum', 'ETH', 16000,    6.67, 'Plataforma');

-- Transacoes na carteira PF do Lucas (id_carteira=1)
INSERT INTO transacao (id_transacao, id_carteira, id_cripto, tipo, quantidade, preco_unitario, taxa, data_operacao)
VALUES (seq_transacao.NEXTVAL, 1, 1, 'COMPRA', 0.5, 300000, 150,  DATE '2026-05-07');
INSERT INTO transacao (id_transacao, id_carteira, id_cripto, tipo, quantidade, preco_unitario, taxa, data_operacao)
VALUES (seq_transacao.NEXTVAL, 1, 2, 'COMPRA', 2.0, 15000,  30,   DATE '2026-05-07');
INSERT INTO transacao (id_transacao, id_carteira, id_cripto, tipo, quantidade, preco_unitario, taxa, data_operacao)
VALUES (seq_transacao.NEXTVAL, 1, 1, 'VENDA',  0.1, 350000, 35,   DATE '2026-05-07');

-- Relatorio
INSERT INTO relatorio (id_relatorio, id_carteira, data_geracao, valor_total_carteira, total_investido, total_taxas, rentabilidade_percentual)
VALUES (seq_relatorio.NEXTVAL, 1, DATE '2026-05-07', 172000, 207207, 242, -16.99);

-- Alerta
INSERT INTO alerta (id_alerta, id_usuario, id_cripto, limite_variacao, ativado, data_configuracao)
VALUES (seq_alerta.NEXTVAL, 1, 1, 5.0, 'S', DATE '2026-05-07');

-- Posicao
INSERT INTO posicao (id_posicao, id_carteira, id_cripto, quantidade_atual, preco_medio_compra, data_primeira_aquisicao, data_ultima_atualizacao)
VALUES (seq_posicao.NEXTVAL, 1, 1, 0.4, 300000, DATE '2026-05-07', DATE '2026-05-07');
INSERT INTO posicao (id_posicao, id_carteira, id_cripto, quantidade_atual, preco_medio_compra, data_primeira_aquisicao, data_ultima_atualizacao)
VALUES (seq_posicao.NEXTVAL, 1, 2, 2.0, 15000,  DATE '2026-05-07', DATE '2026-05-07');

COMMIT;

-- ============================================================================
-- CONSULTAS DE VALIDACAO
-- ============================================================================
-- SELECT * FROM carteira;
-- SELECT * FROM carteira_pf;
-- SELECT * FROM carteira_pj;
-- SELECT * FROM usuario;
-- SELECT * FROM empresa;
-- SELECT * FROM criptoativo;
-- SELECT * FROM transacao;
-- SELECT * FROM posicao;
-- SELECT * FROM alerta;
-- SELECT * FROM relatorio;
