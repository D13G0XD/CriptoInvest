-- ============================================================================
-- CriptoInvest - VOLTZ Engenharia de Software
-- FASE 5 - SCRIPT DDL (Data Definition Language)
--
-- Banco: Oracle 19c+ (Oracle FIAP)
-- Conteudo: DROP / CREATE / ALTER de todas as tabelas do modelo relacional,
--           com todas as restricoes (PK, FK, UNIQUE, CHECK, NOT NULL).
--
-- Estrategia de heranca em CARTEIRA (Joined):
--   carteira (pai)  ->  carteira_pf | carteira_pj  (filhas)
--
-- Relacionamentos:
--   usuario (1) <-> (1) carteira_pf                 -- 1:1 obrigatorio
--   usuario (1)  -> (N) empresa                     -- 1:N
--   empresa (1) <-> (1) carteira_pj                 -- 1:1 obrigatorio
--   carteira (1) -> (N) transacao | posicao | relatorio  (FK polimorfica)
--   carteira (N) <-> (N) criptoativo                -- resolvida por POSICAO
--   usuario  (N) <-> (N) criptoativo                -- resolvida por ALERTA
--
-- Ordem de execucao: 1) este script (DDL)   2) criptoinvest_dml.sql (DML)
-- ============================================================================


-- ============================================================================
-- 1. DROP - remocao dos objetos existentes (ordem reversa de dependencia)
--    Obs.: na primeira execucao os DROP retornam ORA-00942/ORA-02289
--          (objeto inexistente). E esperado, basta seguir o script.
-- ============================================================================
DROP TABLE alerta       CASCADE CONSTRAINTS;
DROP TABLE posicao      CASCADE CONSTRAINTS;
DROP TABLE relatorio    CASCADE CONSTRAINTS;
DROP TABLE transacao    CASCADE CONSTRAINTS;
DROP TABLE criptoativo  CASCADE CONSTRAINTS;
DROP TABLE empresa      CASCADE CONSTRAINTS;
DROP TABLE usuario      CASCADE CONSTRAINTS;
DROP TABLE carteira_pj  CASCADE CONSTRAINTS;
DROP TABLE carteira_pf  CASCADE CONSTRAINTS;
DROP TABLE carteira     CASCADE CONSTRAINTS;

DROP SEQUENCE seq_carteira;
DROP SEQUENCE seq_usuario;
DROP SEQUENCE seq_empresa;
DROP SEQUENCE seq_criptoativo;
DROP SEQUENCE seq_transacao;
DROP SEQUENCE seq_relatorio;
DROP SEQUENCE seq_alerta;
DROP SEQUENCE seq_posicao;


-- ============================================================================
-- 2. CREATE SEQUENCE - geradores das chaves primarias
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
-- 3. CREATE TABLE - tabelas com PK, NOT NULL e CHECK
--    (as FKs e UNIQUEs sao adicionadas na secao 4, via ALTER TABLE)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- CARTEIRA (tabela pai da heranca Joined)
-- ----------------------------------------------------------------------------
CREATE TABLE carteira (
    id_carteira     NUMBER(10)      NOT NULL,
    descricao       VARCHAR2(100)   NOT NULL,
    saldo_reais     NUMBER(15,2)    DEFAULT 0 NOT NULL,
    tipo            CHAR(2)         NOT NULL,
    CONSTRAINT pk_carteira       PRIMARY KEY (id_carteira),
    CONSTRAINT ck_carteira_tipo  CHECK (tipo IN ('PF','PJ')),
    CONSTRAINT ck_carteira_saldo CHECK (saldo_reais >= 0)
);

-- ----------------------------------------------------------------------------
-- CARTEIRA_PF (filha - Pessoa Fisica)
-- ----------------------------------------------------------------------------
CREATE TABLE carteira_pf (
    id_carteira_pf      NUMBER(10)      NOT NULL,
    limite_diario_saque NUMBER(15,2)    DEFAULT 5000 NOT NULL,
    CONSTRAINT pk_carteira_pf     PRIMARY KEY (id_carteira_pf),
    CONSTRAINT ck_carteira_pf_lim CHECK (limite_diario_saque >= 0)
);

-- ----------------------------------------------------------------------------
-- CARTEIRA_PJ (filha - Pessoa Juridica)
-- ----------------------------------------------------------------------------
CREATE TABLE carteira_pj (
    id_carteira_pj    NUMBER(10)    NOT NULL,
    regime_tributario VARCHAR2(30)  DEFAULT 'SIMPLES' NOT NULL,
    CONSTRAINT pk_carteira_pj        PRIMARY KEY (id_carteira_pj),
    CONSTRAINT ck_carteira_pj_regime CHECK (regime_tributario IN ('SIMPLES','LUCRO_PRESUMIDO','LUCRO_REAL'))
);

-- ----------------------------------------------------------------------------
-- USUARIO (Pessoa Fisica) - 1:1 obrigatorio com carteira_pf
-- ----------------------------------------------------------------------------
CREATE TABLE usuario (
    id_usuario       NUMBER(10)     NOT NULL,
    id_carteira_pf   NUMBER(10)     NOT NULL,
    nome             VARCHAR2(150)  NOT NULL,
    email            VARCHAR2(150)  NOT NULL,
    senha            VARCHAR2(100)  NOT NULL,
    cpf              VARCHAR2(14)   NOT NULL,
    autenticacao_2fa CHAR(1)        DEFAULT 'N' NOT NULL,
    CONSTRAINT pk_usuario     PRIMARY KEY (id_usuario),
    CONSTRAINT ck_usuario_2fa CHECK (autenticacao_2fa IN ('S','N'))
);

-- ----------------------------------------------------------------------------
-- EMPRESA (Pessoa Juridica) - N:1 com usuario, 1:1 obrigatorio com carteira_pj
-- ----------------------------------------------------------------------------
CREATE TABLE empresa (
    id_empresa     NUMBER(10)     NOT NULL,
    id_usuario     NUMBER(10)     NOT NULL,
    id_carteira_pj NUMBER(10)     NOT NULL,
    nome           VARCHAR2(150)  NOT NULL,
    cnpj           VARCHAR2(18)   NOT NULL,
    CONSTRAINT pk_empresa PRIMARY KEY (id_empresa)
);

-- ----------------------------------------------------------------------------
-- CRIPTOATIVO  (classe escolhida para a integracao Java <-> Oracle)
-- ----------------------------------------------------------------------------
CREATE TABLE criptoativo (
    id_cripto    NUMBER(10)   NOT NULL,
    nome         VARCHAR2(80) NOT NULL,
    sigla        VARCHAR2(10) NOT NULL,
    preco_atual  NUMBER(18,8) NOT NULL,
    variacao_24h NUMBER(10,4) DEFAULT 0,
    categoria    VARCHAR2(50),
    CONSTRAINT pk_criptoativo       PRIMARY KEY (id_cripto),
    CONSTRAINT ck_criptoativo_preco CHECK (preco_atual >= 0)
);

-- ----------------------------------------------------------------------------
-- TRANSACAO - evento de compra/venda (FK polimorfica para carteira)
-- ----------------------------------------------------------------------------
CREATE TABLE transacao (
    id_transacao   NUMBER(10)   NOT NULL,
    id_carteira    NUMBER(10)   NOT NULL,
    id_cripto      NUMBER(10)   NOT NULL,
    tipo           VARCHAR2(10) NOT NULL,
    quantidade     NUMBER(18,8) NOT NULL,
    preco_unitario NUMBER(18,8) NOT NULL,
    taxa           NUMBER(15,4) NOT NULL,
    data_operacao  DATE         NOT NULL,
    CONSTRAINT pk_transacao       PRIMARY KEY (id_transacao),
    CONSTRAINT ck_transacao_tipo  CHECK (tipo IN ('COMPRA','VENDA')),
    CONSTRAINT ck_transacao_qtde  CHECK (quantidade > 0),
    CONSTRAINT ck_transacao_preco CHECK (preco_unitario >= 0),
    CONSTRAINT ck_transacao_taxa  CHECK (taxa >= 0)
);

-- ----------------------------------------------------------------------------
-- RELATORIO - snapshot de desempenho de uma carteira
-- ----------------------------------------------------------------------------
CREATE TABLE relatorio (
    id_relatorio             NUMBER(10)   NOT NULL,
    id_carteira              NUMBER(10)   NOT NULL,
    data_geracao             DATE         NOT NULL,
    valor_total_carteira     NUMBER(18,2),
    total_investido          NUMBER(18,2),
    total_vendido            NUMBER(18,2),
    total_taxas              NUMBER(15,4),
    lucro_total              NUMBER(18,2),
    rentabilidade_percentual NUMBER(10,4),
    CONSTRAINT pk_relatorio PRIMARY KEY (id_relatorio)
);

-- ----------------------------------------------------------------------------
-- POSICAO - associativa N:N entre CARTEIRA e CRIPTOATIVO (saldo agregado)
-- ----------------------------------------------------------------------------
CREATE TABLE posicao (
    id_posicao              NUMBER(10)   NOT NULL,
    id_carteira             NUMBER(10)   NOT NULL,
    id_cripto               NUMBER(10)   NOT NULL,
    quantidade_atual        NUMBER(18,8) NOT NULL,
    preco_medio_compra      NUMBER(18,8) NOT NULL,
    data_primeira_aquisicao DATE         NOT NULL,
    data_ultima_atualizacao DATE         NOT NULL,
    CONSTRAINT pk_posicao       PRIMARY KEY (id_posicao),
    CONSTRAINT ck_posicao_qtde  CHECK (quantidade_atual >= 0),
    CONSTRAINT ck_posicao_preco CHECK (preco_medio_compra >= 0)
);

-- ----------------------------------------------------------------------------
-- ALERTA - associativa N:N entre USUARIO e CRIPTOATIVO
-- ----------------------------------------------------------------------------
CREATE TABLE alerta (
    id_alerta         NUMBER(10)   NOT NULL,
    id_usuario        NUMBER(10)   NOT NULL,
    id_cripto         NUMBER(10)   NOT NULL,
    limite_variacao   NUMBER(10,4) NOT NULL,
    ativado           CHAR(1)      DEFAULT 'S' NOT NULL,
    data_configuracao DATE         NOT NULL,
    CONSTRAINT pk_alerta         PRIMARY KEY (id_alerta),
    CONSTRAINT ck_alerta_ativado CHECK (ativado IN ('S','N')),
    CONSTRAINT ck_alerta_limite  CHECK (limite_variacao > 0)
);


-- ============================================================================
-- 4. ALTER TABLE - restricoes de UNIQUE e de CHAVE ESTRANGEIRA (FK)
-- ============================================================================

-- --- Chaves unicas (identificadores naturais e 1:1 obrigatorios) -------------
ALTER TABLE usuario     ADD CONSTRAINT uk_usuario_email    UNIQUE (email);
ALTER TABLE usuario     ADD CONSTRAINT uk_usuario_cpf      UNIQUE (cpf);
ALTER TABLE usuario     ADD CONSTRAINT uk_usuario_carteira UNIQUE (id_carteira_pf);
ALTER TABLE empresa     ADD CONSTRAINT uk_empresa_cnpj     UNIQUE (cnpj);
ALTER TABLE empresa     ADD CONSTRAINT uk_empresa_carteira UNIQUE (id_carteira_pj);
ALTER TABLE criptoativo ADD CONSTRAINT uk_criptoativo_sigla UNIQUE (sigla);
ALTER TABLE posicao     ADD CONSTRAINT uk_posicao_cart_cripto UNIQUE (id_carteira, id_cripto);

-- --- Heranca Joined: PK das filhas tambem e FK para a tabela pai -------------
ALTER TABLE carteira_pf ADD CONSTRAINT fk_carteira_pf_carteira
    FOREIGN KEY (id_carteira_pf) REFERENCES carteira (id_carteira);

ALTER TABLE carteira_pj ADD CONSTRAINT fk_carteira_pj_carteira
    FOREIGN KEY (id_carteira_pj) REFERENCES carteira (id_carteira);

-- --- Titulares --------------------------------------------------------------
ALTER TABLE usuario ADD CONSTRAINT fk_usuario_carteira_pf
    FOREIGN KEY (id_carteira_pf) REFERENCES carteira_pf (id_carteira_pf);

ALTER TABLE empresa ADD CONSTRAINT fk_empresa_usuario
    FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario);

ALTER TABLE empresa ADD CONSTRAINT fk_empresa_carteira_pj
    FOREIGN KEY (id_carteira_pj) REFERENCES carteira_pj (id_carteira_pj);

-- --- Movimentacao e analise (FK polimorfica: apontam para a carteira pai) ----
ALTER TABLE transacao ADD CONSTRAINT fk_transacao_carteira
    FOREIGN KEY (id_carteira) REFERENCES carteira (id_carteira);

ALTER TABLE transacao ADD CONSTRAINT fk_transacao_cripto
    FOREIGN KEY (id_cripto) REFERENCES criptoativo (id_cripto);

ALTER TABLE relatorio ADD CONSTRAINT fk_relatorio_carteira
    FOREIGN KEY (id_carteira) REFERENCES carteira (id_carteira);

ALTER TABLE posicao ADD CONSTRAINT fk_posicao_carteira
    FOREIGN KEY (id_carteira) REFERENCES carteira (id_carteira);

ALTER TABLE posicao ADD CONSTRAINT fk_posicao_cripto
    FOREIGN KEY (id_cripto) REFERENCES criptoativo (id_cripto);

ALTER TABLE alerta ADD CONSTRAINT fk_alerta_usuario
    FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario);

ALTER TABLE alerta ADD CONSTRAINT fk_alerta_cripto
    FOREIGN KEY (id_cripto) REFERENCES criptoativo (id_cripto);


-- ============================================================================
-- 5. CREATE INDEX - indices de apoio as FKs mais consultadas
-- ============================================================================
CREATE INDEX idx_empresa_usuario   ON empresa   (id_usuario);
CREATE INDEX idx_transacao_carteira ON transacao (id_carteira);
CREATE INDEX idx_transacao_cripto   ON transacao (id_cripto);
CREATE INDEX idx_relatorio_carteira ON relatorio (id_carteira);
CREATE INDEX idx_alerta_usuario     ON alerta    (id_usuario);
CREATE INDEX idx_alerta_cripto      ON alerta    (id_cripto);


-- ============================================================================
-- 6. COMMENT - documentacao das tabelas e colunas principais
-- ============================================================================
COMMENT ON TABLE  carteira                   IS 'Tabela pai da heranca Joined: atributos comuns das carteiras PF e PJ';
COMMENT ON COLUMN carteira.tipo              IS 'Discriminador da heranca: PF ou PJ';
COMMENT ON TABLE  carteira_pf                IS 'Carteira de Pessoa Fisica - filha de carteira';
COMMENT ON COLUMN carteira_pf.id_carteira_pf IS 'PK e FK -> carteira(id_carteira)';
COMMENT ON TABLE  carteira_pj                IS 'Carteira de Pessoa Juridica - filha de carteira';
COMMENT ON COLUMN carteira_pj.id_carteira_pj IS 'PK e FK -> carteira(id_carteira)';
COMMENT ON TABLE  usuario                    IS 'Pessoa Fisica titular de uma carteira PF (1:1 obrigatorio)';
COMMENT ON TABLE  empresa                    IS 'Pessoa Juridica pertencente a um usuario (1:N) e titular de uma carteira PJ (1:1)';
COMMENT ON TABLE  criptoativo                IS 'Criptomoeda monitorada pela plataforma';
COMMENT ON TABLE  transacao                  IS 'Evento de compra ou venda de criptoativo em uma carteira';
COMMENT ON TABLE  relatorio                  IS 'Snapshot de desempenho de uma carteira em determinada data';
COMMENT ON TABLE  posicao                    IS 'Associativa N:N Carteira x Criptoativo com o saldo agregado';
COMMENT ON TABLE  alerta                     IS 'Associativa N:N Usuario x Criptoativo com o limite de variacao monitorado';


-- ============================================================================
-- 7. VERIFICACAO DA ESTRUTURA CRIADA
-- ============================================================================
-- SELECT table_name FROM user_tables ORDER BY table_name;
-- SELECT constraint_name, constraint_type, table_name
--   FROM user_constraints
--  WHERE constraint_type IN ('P','R','U')
--  ORDER BY table_name, constraint_type;
