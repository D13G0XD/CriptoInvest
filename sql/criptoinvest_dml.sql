-- ============================================================================
-- CriptoInvest - VOLTZ Engenharia de Software
-- FASE 5 - SCRIPT DML (Data Manipulation Language)
--
-- Banco: Oracle 19c+ (Oracle FIAP)
-- Conteudo: INSERT (populacao), UPDATE, DELETE e SELECT de todas as tabelas.
--
-- PRE-REQUISITO: executar antes o script criptoinvest_ddl.sql
-- Os dados espelham a demonstracao da classe Main.java.
-- ============================================================================


-- ============================================================================
-- 1. INSERT - populacao das tabelas (na ordem das dependencias de FK)
--
-- Nenhum id e escrito na mao: a PK vem da sequence e as FKs sao resolvidas por
-- CURRVAL (quando a linha pai acabou de ser inserida) ou por subconsulta na
-- chave natural - cpf, cnpj e sigla, todas UNIQUE no DDL. Assim o script roda
-- com as sequences em qualquer estado, e nao so logo depois do DDL.
--
-- saldo_reais ja reflete as transacoes da secao 1.5, na mesma regra aplicada
-- por Carteira.registrarTransacao: a compra debita bruto + taxa e a venda
-- credita bruto - taxa. Ex.: carteira do Lucas = 250.000 de aportes
-- - 175.175 (BTC) - 32.032 (ETH) + 34.965 (venda de BTC) = 77.758.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1.1 LUCAS: carteira PF (pai + filha) e o usuario titular
-- ----------------------------------------------------------------------------
INSERT INTO carteira (id_carteira, descricao, saldo_reais, tipo)
VALUES (seq_carteira.NEXTVAL, 'Carteira PF de Lucas', 77758, 'PF');

INSERT INTO carteira_pf (id_carteira_pf, limite_diario_saque)
VALUES (seq_carteira.CURRVAL, 5000);

INSERT INTO usuario (id_usuario, id_carteira_pf, nome, email, senha, cpf, autenticacao_2fa)
VALUES (seq_usuario.NEXTVAL, seq_carteira.CURRVAL,
        'Lucas Alves', 'lucas@email.com', 'senha123', '123.456.789-00', 'S');

-- ----------------------------------------------------------------------------
-- 1.2 ANA: carteira PF (pai + filha) e a usuaria titular
-- ----------------------------------------------------------------------------
INSERT INTO carteira (id_carteira, descricao, saldo_reais, tipo)
VALUES (seq_carteira.NEXTVAL, 'Carteira PF de Ana', 10991, 'PF');

INSERT INTO carteira_pf (id_carteira_pf, limite_diario_saque)
VALUES (seq_carteira.CURRVAL, 3000);

INSERT INTO usuario (id_usuario, id_carteira_pf, nome, email, senha, cpf, autenticacao_2fa)
VALUES (seq_usuario.NEXTVAL, seq_carteira.CURRVAL,
        'Ana Souza', 'ana@email.com', 'senha456', '987.654.321-00', 'N');

-- ----------------------------------------------------------------------------
-- 1.3 EMPRESAS: carteira PJ (pai + filha) e a empresa titular
--     O dono sai de uma subconsulta pelo CPF (uk_usuario_cpf).
-- ----------------------------------------------------------------------------
INSERT INTO carteira (id_carteira, descricao, saldo_reais, tipo)
VALUES (seq_carteira.NEXTVAL, 'Carteira PJ - ABCD Investimentos', 29930, 'PJ');

INSERT INTO carteira_pj (id_carteira_pj, regime_tributario)
VALUES (seq_carteira.CURRVAL, 'LUCRO_PRESUMIDO');

INSERT INTO empresa (id_empresa, id_usuario, id_carteira_pj, nome, cnpj)
VALUES (seq_empresa.NEXTVAL,
        (SELECT id_usuario FROM usuario WHERE cpf = '123.456.789-00'),
        seq_carteira.CURRVAL,
        'ABCD Investimentos', '00.000.000/0001-00');

INSERT INTO carteira (id_carteira, descricao, saldo_reais, tipo)
VALUES (seq_carteira.NEXTVAL, 'Carteira PJ - VOLTZ Holding', 92722.75, 'PJ');

INSERT INTO carteira_pj (id_carteira_pj, regime_tributario)
VALUES (seq_carteira.CURRVAL, 'LUCRO_REAL');

INSERT INTO empresa (id_empresa, id_usuario, id_carteira_pj, nome, cnpj)
VALUES (seq_empresa.NEXTVAL,
        (SELECT id_usuario FROM usuario WHERE cpf = '123.456.789-00'),
        seq_carteira.CURRVAL,
        'VOLTZ Holding', '11.111.111/0001-11');

-- ----------------------------------------------------------------------------
-- 1.4 CRIPTOATIVO
-- ----------------------------------------------------------------------------
INSERT INTO criptoativo (id_cripto, nome, sigla, preco_atual, variacao_24h, categoria)
VALUES (seq_criptoativo.NEXTVAL, 'Bitcoin', 'BTC', 350000, 16.67, 'Moeda');

INSERT INTO criptoativo (id_cripto, nome, sigla, preco_atual, variacao_24h, categoria)
VALUES (seq_criptoativo.NEXTVAL, 'Ethereum', 'ETH', 16000, 6.67, 'Plataforma');

INSERT INTO criptoativo (id_cripto, nome, sigla, preco_atual, variacao_24h, categoria)
VALUES (seq_criptoativo.NEXTVAL, 'Solana', 'SOL', 900, -3.25, 'Plataforma');

INSERT INTO criptoativo (id_cripto, nome, sigla, preco_atual, variacao_24h, categoria)
VALUES (seq_criptoativo.NEXTVAL, 'Tether', 'USDT', 5.45, 0.02, 'Stablecoin');

-- Cadastrado apenas para catalogo: ainda nao possui transacoes, posicoes ou alertas
INSERT INTO criptoativo (id_cripto, nome, sigla, preco_atual, variacao_24h, categoria)
VALUES (seq_criptoativo.NEXTVAL, 'Cardano', 'ADA', 4.10, -1.80, 'Plataforma');

-- ----------------------------------------------------------------------------
-- 1.5 TRANSACAO (FK polimorfica: carteiras PF e PJ)
-- ----------------------------------------------------------------------------
INSERT INTO transacao (id_transacao, id_carteira, id_cripto, tipo, quantidade, preco_unitario, taxa, data_operacao, observacao)
VALUES (seq_transacao.NEXTVAL,
        (SELECT id_carteira_pf FROM usuario     WHERE cpf   = '123.456.789-00'),
        (SELECT id_cripto      FROM criptoativo WHERE sigla = 'BTC'),
        'COMPRA', 0.5, 350000, 175, DATE '2026-05-07', NULL);

INSERT INTO transacao (id_transacao, id_carteira, id_cripto, tipo, quantidade, preco_unitario, taxa, data_operacao, observacao)
VALUES (seq_transacao.NEXTVAL,
        (SELECT id_carteira_pf FROM usuario     WHERE cpf   = '123.456.789-00'),
        (SELECT id_cripto      FROM criptoativo WHERE sigla = 'ETH'),
        'COMPRA', 2, 16000, 32, DATE '2026-05-07', 'Aporte em ETH');

INSERT INTO transacao (id_transacao, id_carteira, id_cripto, tipo, quantidade, preco_unitario, taxa, data_operacao, observacao)
VALUES (seq_transacao.NEXTVAL,
        (SELECT id_carteira_pf FROM usuario     WHERE cpf   = '123.456.789-00'),
        (SELECT id_cripto      FROM criptoativo WHERE sigla = 'BTC'),
        'VENDA', 0.1, 350000, 35, DATE '2026-05-08', 'Realizacao parcial de lucro');

INSERT INTO transacao (id_transacao, id_carteira, id_cripto, tipo, quantidade, preco_unitario, taxa, data_operacao, observacao)
VALUES (seq_transacao.NEXTVAL,
        (SELECT id_carteira_pf FROM usuario     WHERE cpf   = '987.654.321-00'),
        (SELECT id_cripto      FROM criptoativo WHERE sigla = 'SOL'),
        'COMPRA', 10, 900, 9, DATE '2026-05-08', NULL);

INSERT INTO transacao (id_transacao, id_carteira, id_cripto, tipo, quantidade, preco_unitario, taxa, data_operacao, observacao)
VALUES (seq_transacao.NEXTVAL,
        (SELECT id_carteira_pj FROM empresa     WHERE cnpj  = '00.000.000/0001-00'),
        (SELECT id_cripto      FROM criptoativo WHERE sigla = 'BTC'),
        'COMPRA', 0.2, 350000, 70, DATE '2026-05-09', 'Compra institucional ABCD');

INSERT INTO transacao (id_transacao, id_carteira, id_cripto, tipo, quantidade, preco_unitario, taxa, data_operacao, observacao)
VALUES (seq_transacao.NEXTVAL,
        (SELECT id_carteira_pj FROM empresa     WHERE cnpj  = '11.111.111/0001-11'),
        (SELECT id_cripto      FROM criptoativo WHERE sigla = 'USDT'),
        'COMPRA', 5000, 5.45, 27.25, DATE '2026-05-09', 'Caixa em stablecoin');

-- ----------------------------------------------------------------------------
-- 1.6 POSICAO (associativa N:N Carteira x Criptoativo)
-- ----------------------------------------------------------------------------
INSERT INTO posicao (id_posicao, id_carteira, id_cripto, quantidade_atual, preco_medio_compra, data_primeira_aquisicao, data_ultima_atualizacao)
VALUES (seq_posicao.NEXTVAL,
        (SELECT id_carteira_pf FROM usuario     WHERE cpf   = '123.456.789-00'),
        (SELECT id_cripto      FROM criptoativo WHERE sigla = 'BTC'),
        0.4, 350000, DATE '2026-05-07', DATE '2026-05-08');

INSERT INTO posicao (id_posicao, id_carteira, id_cripto, quantidade_atual, preco_medio_compra, data_primeira_aquisicao, data_ultima_atualizacao)
VALUES (seq_posicao.NEXTVAL,
        (SELECT id_carteira_pf FROM usuario     WHERE cpf   = '123.456.789-00'),
        (SELECT id_cripto      FROM criptoativo WHERE sigla = 'ETH'),
        2, 16000, DATE '2026-05-07', DATE '2026-05-07');

INSERT INTO posicao (id_posicao, id_carteira, id_cripto, quantidade_atual, preco_medio_compra, data_primeira_aquisicao, data_ultima_atualizacao)
VALUES (seq_posicao.NEXTVAL,
        (SELECT id_carteira_pf FROM usuario     WHERE cpf   = '987.654.321-00'),
        (SELECT id_cripto      FROM criptoativo WHERE sigla = 'SOL'),
        10, 900, DATE '2026-05-08', DATE '2026-05-08');

INSERT INTO posicao (id_posicao, id_carteira, id_cripto, quantidade_atual, preco_medio_compra, data_primeira_aquisicao, data_ultima_atualizacao)
VALUES (seq_posicao.NEXTVAL,
        (SELECT id_carteira_pj FROM empresa     WHERE cnpj  = '00.000.000/0001-00'),
        (SELECT id_cripto      FROM criptoativo WHERE sigla = 'BTC'),
        0.2, 350000, DATE '2026-05-09', DATE '2026-05-09');

INSERT INTO posicao (id_posicao, id_carteira, id_cripto, quantidade_atual, preco_medio_compra, data_primeira_aquisicao, data_ultima_atualizacao)
VALUES (seq_posicao.NEXTVAL,
        (SELECT id_carteira_pj FROM empresa     WHERE cnpj  = '11.111.111/0001-11'),
        (SELECT id_cripto      FROM criptoativo WHERE sigla = 'USDT'),
        5000, 5.45, DATE '2026-05-09', DATE '2026-05-09');

-- ----------------------------------------------------------------------------
-- 1.7 RELATORIO
-- ----------------------------------------------------------------------------
INSERT INTO relatorio (id_relatorio, id_carteira, data_geracao, valor_total_carteira, total_investido, total_vendido, total_taxas, lucro_total, rentabilidade_percentual)
VALUES (seq_relatorio.NEXTVAL,
        (SELECT id_carteira_pf FROM usuario WHERE cpf = '123.456.789-00'),
        DATE '2026-05-10', 172000, 207207, 34965, 242, -242, -0.12);

INSERT INTO relatorio (id_relatorio, id_carteira, data_geracao, valor_total_carteira, total_investido, total_vendido, total_taxas, lucro_total, rentabilidade_percentual)
VALUES (seq_relatorio.NEXTVAL,
        (SELECT id_carteira_pj FROM empresa WHERE cnpj = '00.000.000/0001-00'),
        DATE '2026-05-10', 70000, 70070, 0, 70, -70, -0.10);

-- ----------------------------------------------------------------------------
-- 1.8 ALERTA (associativa N:N Usuario x Criptoativo)
-- ----------------------------------------------------------------------------
INSERT INTO alerta (id_alerta, id_usuario, id_cripto, limite_variacao, ativado, data_configuracao)
VALUES (seq_alerta.NEXTVAL,
        (SELECT id_usuario FROM usuario     WHERE cpf   = '123.456.789-00'),
        (SELECT id_cripto  FROM criptoativo WHERE sigla = 'BTC'),
        5.0, 'S', DATE '2026-05-07');

INSERT INTO alerta (id_alerta, id_usuario, id_cripto, limite_variacao, ativado, data_configuracao)
VALUES (seq_alerta.NEXTVAL,
        (SELECT id_usuario FROM usuario     WHERE cpf   = '123.456.789-00'),
        (SELECT id_cripto  FROM criptoativo WHERE sigla = 'ETH'),
        8.0, 'S', DATE '2026-05-07');

INSERT INTO alerta (id_alerta, id_usuario, id_cripto, limite_variacao, ativado, data_configuracao)
VALUES (seq_alerta.NEXTVAL,
        (SELECT id_usuario FROM usuario     WHERE cpf   = '987.654.321-00'),
        (SELECT id_cripto  FROM criptoativo WHERE sigla = 'SOL'),
        10.0, 'N', DATE '2026-05-08');

COMMIT;


-- ============================================================================
-- 2. UPDATE - atualizacao de dados
-- ============================================================================

-- 2.1 Cotacao do Bitcoin subiu para R$ 380.000 (variacao recalculada)
UPDATE criptoativo
   SET preco_atual  = 380000,
       variacao_24h = 8.57
 WHERE sigla = 'BTC';

-- 2.2 Ana ativou a autenticacao em dois fatores
UPDATE usuario
   SET autenticacao_2fa = 'S'
 WHERE cpf = '987.654.321-00';

-- 2.3 Aumento do limite diario de saque da carteira PF do Lucas
UPDATE carteira_pf
   SET limite_diario_saque = 8000
 WHERE id_carteira_pf = (SELECT id_carteira_pf FROM usuario WHERE cpf = '123.456.789-00');

-- 2.4 Mudanca de regime tributario da ABCD Investimentos
UPDATE carteira_pj
   SET regime_tributario = 'LUCRO_REAL'
 WHERE id_carteira_pj = (SELECT id_carteira_pj FROM empresa WHERE cnpj = '00.000.000/0001-00');

-- 2.5 Deposito de R$ 10.000 na carteira PF do Lucas
UPDATE carteira
   SET saldo_reais = saldo_reais + 10000
 WHERE id_carteira = (SELECT id_carteira_pf FROM usuario WHERE cpf = '123.456.789-00');

-- 2.6 Reavaliacao da posicao de BTC na carteira PF do Lucas
UPDATE posicao
   SET quantidade_atual        = 0.35,
       data_ultima_atualizacao = DATE '2026-05-11'
 WHERE id_carteira = (SELECT id_carteira_pf FROM usuario     WHERE cpf   = '123.456.789-00')
   AND id_cripto   = (SELECT id_cripto      FROM criptoativo WHERE sigla = 'BTC');

-- 2.7 Ana vendeu toda a sua posicao em SOL: quantidade zerada
UPDATE posicao
   SET quantidade_atual        = 0,
       data_ultima_atualizacao = DATE '2026-05-11'
 WHERE id_carteira = (SELECT id_carteira_pf FROM usuario     WHERE cpf   = '987.654.321-00')
   AND id_cripto   = (SELECT id_cripto      FROM criptoativo WHERE sigla = 'SOL');

-- 2.8 Alerta de ETH passa a monitorar variacao de 6%
UPDATE alerta
   SET limite_variacao = 6.0
 WHERE id_usuario = (SELECT id_usuario FROM usuario     WHERE cpf   = '123.456.789-00')
   AND id_cripto  = (SELECT id_cripto  FROM criptoativo WHERE sigla = 'ETH');

COMMIT;


-- ============================================================================
-- 3. DELETE - remocao de dados (respeitando a integridade referencial)
-- ============================================================================

-- 3.1 Remove o alerta desativado da Ana
DELETE FROM alerta
 WHERE ativado = 'N';

-- 3.2 Remove as transacoes de venda da carteira PF do Lucas
--     As linhas de RELATORIO nao sao recalculadas de proposito: relatorio e um
--     snapshot fechado em data_geracao, e nao uma visao derivada das transacoes
--     (por isso guarda os totais em colunas proprias). Depois deste DELETE a
--     consulta 4.10 ainda mostra total_vendido = 34965, que era o valor correto
--     na data do relatorio.
DELETE FROM transacao
 WHERE id_carteira = (SELECT id_carteira_pf FROM usuario WHERE cpf = '123.456.789-00')
   AND tipo        = 'VENDA';

-- 3.3 Remove as posicoes zeradas (nenhuma quantidade em custodia)
DELETE FROM posicao
 WHERE quantidade_atual = 0;

-- 3.4 Remove um criptoativo que nao possui transacoes, posicoes nem alertas
--     (as subconsultas garantem que nenhuma FK sera violada)
DELETE FROM criptoativo
 WHERE sigla = 'ADA'
   AND id_cripto NOT IN (SELECT id_cripto FROM transacao)
   AND id_cripto NOT IN (SELECT id_cripto FROM posicao)
   AND id_cripto NOT IN (SELECT id_cripto FROM alerta);

COMMIT;


-- ============================================================================
-- 4. SELECT - consultas de validacao e consultas gerenciais
-- ============================================================================

-- 4.1 Conferencia simples de todas as tabelas
SELECT * FROM carteira    ORDER BY id_carteira;
SELECT * FROM carteira_pf ORDER BY id_carteira_pf;
SELECT * FROM carteira_pj ORDER BY id_carteira_pj;
SELECT * FROM usuario     ORDER BY id_usuario;
SELECT * FROM empresa     ORDER BY id_empresa;
SELECT * FROM criptoativo ORDER BY id_cripto;
SELECT * FROM transacao   ORDER BY id_transacao;
SELECT * FROM posicao     ORDER BY id_posicao;
SELECT * FROM relatorio   ORDER BY id_relatorio;
SELECT * FROM alerta      ORDER BY id_alerta;

-- 4.2 Criptoativos ordenados pelo maior preco
SELECT sigla, nome, categoria, preco_atual, variacao_24h
  FROM criptoativo
 ORDER BY preco_atual DESC;

-- 4.3 Usuarios com a sua carteira PF (heranca Joined: pai + filha)
SELECT u.id_usuario,
       u.nome,
       u.email,
       u.autenticacao_2fa,
       c.descricao,
       c.tipo,
       c.saldo_reais,
       pf.limite_diario_saque
  FROM usuario u
  INNER JOIN carteira_pf pf ON pf.id_carteira_pf = u.id_carteira_pf
  INNER JOIN carteira    c  ON c.id_carteira     = pf.id_carteira_pf
 ORDER BY u.id_usuario;

-- 4.4 Empresas, o usuario dono e a carteira PJ
SELECT e.nome            AS empresa,
       e.cnpj,
       u.nome            AS dono,
       pj.regime_tributario,
       c.saldo_reais
  FROM empresa e
  INNER JOIN usuario     u  ON u.id_usuario     = e.id_usuario
  INNER JOIN carteira_pj pj ON pj.id_carteira_pj = e.id_carteira_pj
  INNER JOIN carteira    c  ON c.id_carteira     = pj.id_carteira_pj
 ORDER BY e.id_empresa;

-- 4.5 Extrato de transacoes por carteira e criptoativo
SELECT t.id_transacao,
       c.descricao       AS carteira,
       c.tipo            AS tipo_carteira,
       cr.sigla,
       t.tipo            AS operacao,
       t.quantidade,
       t.preco_unitario,
       t.taxa,
       (t.quantidade * t.preco_unitario) AS valor_bruto,
       t.data_operacao
  FROM transacao t
  INNER JOIN carteira    c  ON c.id_carteira = t.id_carteira
  INNER JOIN criptoativo cr ON cr.id_cripto  = t.id_cripto
 ORDER BY t.data_operacao, t.id_transacao;

-- 4.6 Valor de mercado das posicoes (associativa N:N em uso)
SELECT c.descricao AS carteira,
       cr.sigla,
       p.quantidade_atual,
       p.preco_medio_compra,
       (p.quantidade_atual * cr.preco_atual) AS valor_atual,
       (p.quantidade_atual * (cr.preco_atual - p.preco_medio_compra)) AS lucro_nao_realizado
  FROM posicao p
  INNER JOIN carteira    c  ON c.id_carteira = p.id_carteira
  INNER JOIN criptoativo cr ON cr.id_cripto  = p.id_cripto
 ORDER BY valor_atual DESC;

-- 4.7 Total investido por carteira (agrupamento)
SELECT c.id_carteira,
       c.descricao,
       c.tipo,
       COUNT(t.id_transacao)                    AS qtde_transacoes,
       NVL(SUM(t.quantidade * t.preco_unitario), 0) AS total_movimentado,
       NVL(SUM(t.taxa), 0)                      AS total_taxas
  FROM carteira c
  LEFT JOIN transacao t ON t.id_carteira = c.id_carteira
 GROUP BY c.id_carteira, c.descricao, c.tipo
 ORDER BY total_movimentado DESC;

-- 4.8 Criptoativos com preco acima da media (subconsulta)
SELECT sigla, nome, preco_atual
  FROM criptoativo
 WHERE preco_atual > (SELECT AVG(preco_atual) FROM criptoativo)
 ORDER BY preco_atual DESC;

-- 4.9 Alertas ativos monitorados por cada usuario
SELECT u.nome AS usuario,
       cr.sigla,
       a.limite_variacao,
       cr.variacao_24h,
       CASE
           WHEN ABS(cr.variacao_24h) >= a.limite_variacao THEN 'DISPARADO'
           ELSE 'NORMAL'
       END AS situacao
  FROM alerta a
  INNER JOIN usuario     u  ON u.id_usuario = a.id_usuario
  INNER JOIN criptoativo cr ON cr.id_cripto = a.id_cripto
 WHERE a.ativado = 'S'
 ORDER BY u.nome, cr.sigla;

-- 4.10 Relatorios gerados por carteira
SELECT r.id_relatorio,
       c.descricao AS carteira,
       r.data_geracao,
       r.valor_total_carteira,
       r.lucro_total,
       r.rentabilidade_percentual
  FROM relatorio r
  INNER JOIN carteira c ON c.id_carteira = r.id_carteira
 ORDER BY r.data_geracao DESC;

-- 4.11 Patrimonio consolidado por usuario (carteira PF + carteiras PJ das empresas)
SELECT u.nome AS usuario,
       (SELECT c.saldo_reais
          FROM carteira c
         WHERE c.id_carteira = u.id_carteira_pf) AS saldo_pf,
       NVL((SELECT SUM(c2.saldo_reais)
              FROM empresa  e
              INNER JOIN carteira c2 ON c2.id_carteira = e.id_carteira_pj
             WHERE e.id_usuario = u.id_usuario), 0) AS saldo_pj_total
  FROM usuario u
 ORDER BY u.nome;
