# CriptoInvest

**Sistema de Gestão de Investimentos em Criptoativos**

Projeto acadêmico desenvolvido pela equipe **VOLTZ Engenharia de Software** para a disciplina de Engenharia de Software — FIAP.

---

## Sobre o Projeto

O CriptoInvest é uma plataforma voltada para investidores pessoa física e jurídica que desejam gerenciar seus portfólios de criptoativos de forma centralizada, segura e com acompanhamento diário de desempenho.

O sistema resolve problemas reais enfrentados por investidores no mercado cripto: fragmentação de ativos em múltiplas exchanges, falta de acompanhamento em tempo real, ausência de relatórios gerenciais e dificuldade na gestão de investimentos distribuídos entre múltiplas empresas (CNPJs).

## Contexto

O mercado de criptoativos ultrapassou a marca de 2,5 trilhões de dólares em capitalização global, e o Brasil ocupa a 5ª posição no Índice Global de Adoção de Criptomoedas (Chainalysis). Apesar desse crescimento, investidores ainda carecem de ferramentas que combinem gestão multiempresa, relatórios robustos e segurança em uma única plataforma.

## Público-Alvo

- **Empresários** com múltiplos CNPJs que buscam controle centralizado dos investimentos em criptoativos de cada empresa.
- **Investidores experientes** que utilizam aplicativos financeiros e desejam monitoramento em tempo real com dashboards intuitivos.
- **Investidores iniciantes** que estão entrando no mercado cripto e precisam de uma interface simples e didática.

## Funcionalidades

- **Dashboard de portfólio** com visão consolidada dos criptoativos, gráficos de evolução patrimonial e indicadores de rentabilidade.
- **Gestão de investimentos** com registro de aportes, histórico de transações e simulador de rentabilidade.
- **Gestão multiempresa** com cadastro de múltiplos CNPJs, separação de portfólios por empresa e visão gerencial unificada.
- **Segurança** com autenticação em dois fatores (2FA), criptografia de dados e conformidade com a LGPD.
- **Relatórios e exportação** com relatórios diários de performance e exportação de dados.
- **Alertas** configuráveis para variações de preço dos criptoativos monitorados.

> As funcionalidades acima descrevem o **escopo do produto** definido na Fase 1. O que está
> implementado até a Fase 6 é o modelo de domínio em Java, o modelo relacional Oracle e a
> integração JDBC completa — um DAO para cada uma das 10 tabelas do DDL, com uma `Main` que
> testa INSERT, SELECT, UPDATE e DELETE de todas elas. Não há interface gráfica, cotações em
> tempo real nem a camada de segurança (2FA e criptografia são requisitos de produto, ainda não
> implementados). A `Main` das Fases 1 a 4 (demonstração do domínio e persistência em arquivos
> texto) foi substituída pelo teste de integração e continua no histórico do git (commit `bb7e437`).

## Como Executar

O projeto é Java puro. Compile e execute direto com `javac`/`java`; o Maven (`pom.xml`) é
opcional e serve apenas para baixar o driver JDBC do Oracle (`ojdbc8`).

A classe principal é `com.criptoinvest.Main` (arquivo `src/com/criptoinvest/Main.java`, no
pacote raiz — saiu do pacote `model` na Fase 6). Ela é o **teste de integração JDBC** das 10
tabelas: precisa do driver no classpath, das credenciais do banco e do DDL já aplicado no schema.

**Compilar:**
```
javac -sourcepath src -d out src/com/criptoinvest/Main.java
```
O `-sourcepath` faz o compilador puxar sozinho as classes dos três pacotes (`model`, `dao`
e `factory`), sem precisar listar cada um. A compilação não depende do driver (os DAOs usam só
`java.sql`).

Sem o driver no classpath (ou sem acesso ao banco), a `Main` testa a conexão, informa que não há
conexão, mostra como configurar as credenciais e termina sem executar os testes. Com conexão, mas
sem o DDL aplicado, a contagem inicial das tabelas falha (`ORA-00942`) e a `Main` pede para rodar
`sql/criptoinvest_ddl.sql`.

### Compilar e executar com o banco

A integração com o Oracle exige o driver JDBC (`ojdbc8`) no classpath. Há duas formas:

**Com Maven (recomendado):**
```
mvn compile
mvn exec:java
```
O `pom.xml` declara o `exec-maven-plugin` com versão fixa e já aponta a classe principal
`com.criptoinvest.Main`, então não é preciso passar `-Dexec.mainClass`.

**Com javac/java + driver baixado manualmente em `lib/`:**
```
javac -cp lib/ojdbc8.jar -sourcepath src -d out src/com/criptoinvest/Main.java

java -cp "out;lib/ojdbc8.jar" com.criptoinvest.Main   # Windows
java -cp "out:lib/ojdbc8.jar" com.criptoinvest.Main   # Linux / macOS
```
Atenção ao separador do classpath: `;` no Windows e `:` no Linux e no macOS.

### Credenciais do banco

A `ConnectionFactory` é o **único lugar** do código que conhece a URL, o usuário e a senha do
banco: os DAOs e a `Main` só pedem uma conexão a ela, e a senha nunca é exibida (a `Main` mostra
apenas a URL e o usuário).

Copie o `.env_exemplo` para um arquivo `.env` na raiz do projeto e troque os valores. O `.env`
guarda credenciais reais, está listado no `.gitignore` e **nunca deve ser versionado**:
```
DB_URL = jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL
DB_USER = rmXXXXXX
DB_PASSWORD = ddmmaa
```
(no padrão da FIAP, o usuário é o RM do aluno e a senha é a data de nascimento em `ddmmaa`)

A `ConnectionFactory` resolve cada dado na primeira fonte que o define:

1. propriedades de sistema: `-Ddb.url`, `-Ddb.user`, `-Ddb.password`;
2. o arquivo `.env` acima — procurado na pasta atual e em até três níveis acima, ou no caminho
   indicado por `-Denv.file=/caminho/para/.env`;
3. as constantes `_PADRAO` da própria classe.

Assim dá para sobrepor o `.env` pontualmente, sem editá-lo:
```
java -Ddb.user=rmXXXXXX -Ddb.password=SUA_SENHA -cp "out;lib/ojdbc8.jar" com.criptoinvest.Main
mvn exec:java -Ddb.user=rmXXXXXX -Ddb.password=SUA_SENHA
```

### Ordem dos scripts SQL (Oracle)

Execute os scripts como script (no SQL Developer, "Executar Script" — F5), nesta ordem:

1. **`sql/criptoinvest_ddl.sql`** (obrigatório) — cria tabelas, sequences, restrições e índices.
   Pode ser executado de novo quando quiser: ele apaga e recria tudo, **inclusive os dados**.
2. **`sql/criptoinvest_dml.sql`** (opcional) — dados de exemplo, UPDATEs, DELETEs e consultas.
   Só deve rodar **logo depois do DDL**, com as tabelas recém-criadas: executá-lo de novo sem
   reaplicar o DDL falha com `ORA-00001` (CPF, e-mail, CNPJ, sigla, posição e alerta já
   existem) e duplica transações e relatórios, podendo deixar carteiras sem titular. Para
   repovoar, rode os dois scripts de novo, nessa ordem.

A `Main` funciona com ou sem o DML: os dados de teste usam chaves naturais com sufixo de
timestamp (não colidem com o DML nem com execuções anteriores), são apagados no fim e a contagem
final de cada tabela é conferida contra a inicial. Por isso ela pode rodar antes ou depois do DML
e quantas vezes for preciso.

## Estrutura de Classes

O código está dividido em quatro partes: o ponto de entrada em `com.criptoinvest`, o domínio em
`com.criptoinvest.model`, o acesso a dados em `com.criptoinvest.dao` e a conexão em
`com.criptoinvest.factory`.

```
src/com/criptoinvest/
├── Main.java                   → Ponto de entrada: teste de integração JDBC das 10 tabelas (Fase 6)
├── factory/
│   └── ConnectionFactory.java  → Conexão com o Oracle da FIAP (URL, usuário, senha, timeout)
├── dao/
│   ├── CrudDAO.java            → Interface genérica: inserir, alterar, excluir, buscarPorId, listarTodos
│   ├── BaseDAO.java            → Superclasse dos DAOs: conexão, NEXTVAL da sequence, rollback, S/N, datas
│   ├── CriptoativoDAO.java     → criptoativo (+ buscarPorSigla)
│   ├── CarteiraDAO.java        → carteira: leitura polimórfica (PF/PJ), atualizarSaldo, carregarHistorico
│   ├── CarteiraPFDAO.java      → carteira_pf + linha pai em carteira
│   ├── CarteiraPJDAO.java      → carteira_pj + linha pai em carteira
│   ├── UsuarioDAO.java         → usuario + a sua carteira PF (mesma transação)
│   ├── EmpresaDAO.java         → empresa + a sua carteira PJ (mesma transação)
│   ├── TransacaoDAO.java       → transacao: registrar (transação + posição + saldo, atômico) e CRUD
│   ├── PosicaoDAO.java         → posicao (associativa carteira × criptoativo)
│   ├── RelatorioDAO.java       → relatorio (snapshot gravado, lido sem recalcular)
│   └── AlertaDAO.java          → alerta (associativa usuario × criptoativo)
└── model/
    ├── Carteira.java       → Classe abstrata pai da herança (joined): saldo, transações, cálculos
    ├── CarteiraPF.java     → Carteira de Pessoa Física (limite diário de saque)
    ├── CarteiraPJ.java     → Carteira de Pessoa Jurídica (regime tributário)
    ├── Usuario.java        → Pessoa Física: 1:1 com CarteiraPF, 1:N com Empresa
    ├── Empresa.java        → Pessoa Jurídica: 1:1 com CarteiraPJ, N:1 com Usuario (dono)
    ├── Criptoativo.java    → Representa uma criptomoeda (BTC, ETH, etc.)
    ├── Transacao.java      → Registro de compra ou venda (taxa de 0,1%, com observação)
    ├── Posicao.java        → Associativa Carteira ↔ Criptoativo (saldo agregado)
    ├── Alerta.java         → Associativa Usuario ↔ Criptoativo (limite de variação)
    ├── Relatorio.java      → Snapshot imutável de desempenho de uma carteira numa data
    ├── Valores.java        → Escalas, precisão e arredondamentos dos valores (BigDecimal)
    └── Datas.java          → Conversão das datas em texto "yyyy-MM-dd" para LocalDate
```

### Scripts SQL

```
sql/
├── criptoinvest_ddl.sql  → DDL: DROP, CREATE (tabelas/sequences/índices) e ALTER (PKs, FKs, UKs)
└── criptoinvest_dml.sql  → DML: INSERT (população), UPDATE, DELETE e SELECT (consultas gerenciais)
```

**Fonte de verdade do modelo:** `sql/criptoinvest_ddl.sql` (Fase 6), com cada mudança da fase
marcada com `-- [Fase 6]` — veja [Alterações do DDL na Fase 6](#alterações-do-ddl-na-fase-6).
O `modelo/Dicionario_de_Dados.pdf` foi regerado na Fase 6 a partir desse DDL (inclui
sequences, índices e o histórico de alterações do schema). O MER e a normalização ainda são
os da Fase 4 (ver [Limitações conhecidas](#limitações-conhecidas)).

Sobre reexecução (detalhes em [Ordem dos scripts SQL](#ordem-dos-scripts-sql-oracle)):

- o DDL pode ser executado repetidas vezes: ele abre com um bloco PL/SQL que consulta
  `user_tables` e `user_sequences` e só remove o que existe, então não devolve
  `ORA-00942`/`ORA-02289` na primeira execução;
- o DML não escreve nenhum id na mão. A PK sai da sequence e as FKs são resolvidas por
  `CURRVAL` (quando a linha pai acabou de ser inserida) ou por subconsulta na chave natural —
  `cpf`, `cnpj` e `sigla`, todas `UNIQUE`. Assim ele não depende de as sequences estarem
  zeradas (a `Main` consome as sequences ao testar). Mas ele **não** é reexecutável sozinho:
  deve rodar uma vez, logo depois do DDL.

### Diagrama de Relacionamentos

```
                Carteira (abstract, tipo IN ('PF','PJ'))
                /        \
         CarteiraPF      CarteiraPJ
        (limite saque)   (regime trib.)
            ▲                 ▲
            │ 1:1             │ 1:1
            │                 │
        Usuario ───POSSUI───► Empresa
        (idUsuario PK)  1:N  (idEmpresa PK, FK→Usuario)
            │                 │
            │                 │ AGRUPA (1:N) ──► Transacao (FK→Carteira, FK→Criptoativo)
            │ MONITORA (N:N)  │
            │                 └─► Relatorio (FK→Carteira)
            ▼                 └─► Posicao (FK→Carteira, FK→Criptoativo)
         Alerta
       (FK→Usuario,
        FK→Criptoativo)         Criptoativo (idCripto PK)
```

#### Tabela de Relacionamentos

| Origem      | Verbo            | Destino     | Cardinalidade | Obrigatoriedade               | Resolução                        |
|-------------|------------------|-------------|---------------|-------------------------------|----------------------------------|
| Usuario     | POSSUI           | CarteiraPF  | 1 : 1         | Obrigatório dos dois lados    | FK `idCarteiraPF` em Usuario     |
| Empresa     | POSSUI           | CarteiraPJ  | 1 : 1         | Obrigatório dos dois lados    | FK `idCarteiraPJ` em Empresa     |
| Usuario     | POSSUI           | Empresa     | 1 : N         | Empresa obrigatória ter dono  | FK `idUsuario` em Empresa        |
| Carteira    | AGRUPA           | Transacao   | 1 : N         | Transacao obrigatória ter Carteira; Carteira pode estar vazia | FK `idCarteira` em Transacao |
| Transacao   | REFERE-SE A      | Criptoativo | N : 1         | Transacao obrigatória ter Cripto | FK `idCripto` em Transacao    |
| Relatorio   | RESUME           | Carteira    | N : 1         | Relatorio obrigatório ter Carteira | FK `idCarteira` em Relatorio |
| **Carteira**| **POSSUI**       | **Criptoativo** | **N : N** | Resolvida por `Posicao`       | **Posicao** (PK `idPosicao`, FKs `idCarteira`+`idCripto`) |
| **Usuario** | **MONITORA**     | **Criptoativo** | **N : N** | Resolvida por `Alerta`        | **Alerta** (PK `idAlerta`, FKs `idUsuario`+`idCripto`) |

#### Entidades Associativas — PK, FKs e Atributos próprios

| Entidade    | PK            | FKs                                              | Atributos próprios                                            |
|-------------|---------------|--------------------------------------------------|---------------------------------------------------------------|
| `Posicao`   | `idPosicao`   | `idCarteira` → Carteira, `idCripto` → Criptoativo | `quantidadeAtual`, `precoMedioCompra`, `dataPrimeiraAquisicao`, `dataUltimaAtualizacao` |
| `Alerta`    | `idAlerta`    | `idUsuario` → Usuario, `idCripto` → Criptoativo  | `limiteVariacao`, `ativado`, `dataConfiguracao`               |

### Herança em `Carteira` (estratégia Joined)

A herança foi modelada na **carteira**, não no titular: `Carteira` (pai abstrato) tem como filhas `CarteiraPF` e `CarteiraPJ`. No banco, isso vira três tabelas — `carteira` (atributos comuns + discriminador `tipo`), `carteira_pf` (PK/FK ligando-se à pai, com `limite_diario_saque`) e `carteira_pj` (PK/FK ligando-se à pai, com `regime_tributario`). Tabelas filhas como `transacao`, `posicao`, `relatorio` referenciam a tabela pai `carteira`, mantendo a FK polimórfica.

Como `carteira` é uma tabela única para PF e PJ, o id da carteira **não** vem do titular (usuário 2 e empresa 2 gerariam a mesma PK): ele vem da sequence `seq_carteira`, obtida pelo DAO no INSERT. O mesmo vale para todas as entidades — `Posicao` usa `seq_posicao`, `Transacao` usa `seq_transacao` e assim por diante. Até o INSERT o id é **0**, que significa "ainda não persistido"; o DAO só atribui o id ao objeto depois de o INSERT dar certo e o devolve a 0 se a transação for desfeita. Por isso a carteira localiza a posição de uma transação pelo próprio objeto `Criptoativo` (e pelo id apenas quando ele já é > 0): dois criptoativos novos, ambos com id 0, não se misturam na mesma posição.

#### Regras de negócio da `Carteira`

`registrarTransacao` movimenta as duas pontas de uma vez — o saldo em reais e a posição em custódia — e só grava a operação no histórico depois de validar as duas:

| Operação | Saldo em reais | Posição | Recusada quando |
|---|---|---|---|
| `COMPRA` | debita `bruto + taxa` | soma a quantidade e recalcula o preço médio | o saldo em reais não cobre o valor com taxa (`ck_carteira_saldo`) |
| `VENDA`  | credita `bruto - taxa` | subtrai a quantidade | a posição em custódia é menor que a quantidade vendida |

Uma transação recusada não altera nada e não entra no histórico, então os totais de vendas, taxas, lucro e rentabilidade nunca contam uma operação que não aconteceu. O método devolve `boolean` informando se a operação foi efetivada. Uma observação maior que `transacao.observacao VARCHAR2(200)` (contada em bytes UTF-8, como o Oracle conta) também recusa a operação antes de qualquer efeito em memória.

`registrarTransacao` só altera a memória; quem grava o resultado no banco é `TransacaoDAO.registrar` (ver [Transações JDBC](#transações-jdbc)).

## Conceitos de POO Aplicados

| Conceito | Implementação |
|---|---|
| **Encapsulamento** | Todos os campos são `private`/`protected`, acessados via getters/setters |
| **Herança** | `CarteiraPF` e `CarteiraPJ` estendem `Carteira` (joined inheritance); todos os DAOs estendem `BaseDAO` |
| **Polimorfismo dinâmico** | `getTipo()` abstrato em `Carteira`, sobrescrito nas filhas; `sacar()` sobrescrito em `CarteiraPF` para aplicar limite diário; `CarteiraDAO` devolve cada linha como `CarteiraPF` ou `CarteiraPJ` |
| **Polimorfismo estático** | Sobrecarga de `depositar`, `sacar`, `registrarTransacao` e `atualizarPreco`, e os construtores que aceitam `double` ou `BigDecimal` (as versões `double` recebem a data como texto `yyyy-MM-dd`) |
| **Abstração / interface** | `CrudDAO<T>` define o contrato comum (`inserir`, `alterar`, `excluir`, `buscarPorId`, `listarTodos`) implementado pelos 10 DAOs |
| **Imutabilidade** | `Relatorio` fecha os números no construtor: campos `final`, sem setters — um snapshot não pode se contradizer |

## Tecnologias

- **Linguagem:** Java SE
- **JDK:** compilado para Java 17 (`maven.compiler.source/target` no `pom.xml`); testado no OpenJDK 26
- **Banco de Dados:** Oracle 19c+ (FIAP) — scripts em `sql/`
- **Acesso a dados:** JDBC puro (driver `ojdbc8`), padrão DAO + Connection Factory, `PreparedStatement`
  com parâmetros `?` e transações JDBC (`setAutoCommit(false)`, `commit`/`rollback`)
- **Datas:** `java.time.LocalDate`, gravada e lida como `DATE` (`java.sql.Date`)
- **Valores monetários:** `BigDecimal` com as escalas do DDL (2 para reais, 8 para cripto,
  4 para percentuais) — `double` não representa 0,10 exatamente e o erro se acumula a cada
  operação da carteira. A precisão (dígitos antes da vírgula) só é conferida nos percentuais:
  a variação 24h e a rentabilidade calculadas (`Valores.percentualNoTeto`) e o limite do alerta
  são limitados ao teto de `NUMBER(10,4)` (±999999.9999), em vez de falharem no Oracle
  com `ORA-01438`
- **Build:** Maven (`pom.xml`) ou `javac`/`java` com o driver no classpath

## Equipe VOLTZ

| Nome | RM |
|---|---|
| Arthur Sousa Pereira | RM561380 |
| Diego Motoike Kanamori | RM561237 |
| João Guilherme Caetano Diniz | RM563945 |
| Joao Silva Portugal Guimarães | RM556923 |
| Lucas Alves Pereira | RM561636 |

## Entregas

| Sprint | Entrega |
|---|---|
| Sprint 1 — Fase 1 | Escopo do Produto (Problema, Público-Alvo e Solução) |
| Sprint 2 — Fase 2 | Classes Java + Diagrama de Classes |
| Sprint 3 — Fase 3 | Encapsulamento, Herança, Polimorfismo e Classe Main |
| Sprint 4 — Fase 4 | Modelo Relacional SQL (DDL Oracle, herança joined em Carteira); Normalização 1FN/2FN/3FN (`modelo/Normalizacao.pdf`); persistência em arquivos texto a partir de `ArrayList` e `HashMap` na `Main` da época (substituída na Fase 6; segue no histórico do git) |
| Sprint 5 — Fase 5 | Script DDL (`sql/criptoinvest_ddl.sql`) e script DML (`sql/criptoinvest_dml.sql`); classe de conexão com o Oracle da FIAP (`ConnectionFactory`); integração completa da classe `Criptoativo` com o banco (`CriptoativoDAO`: inserir, alterar, excluir e exibir); métodos de teste dessa integração na `Main` da época |
| Sprint 6 — Fase 6 | Integração JDBC completa: um DAO por tabela (10 tabelas) sobre `CrudDAO` + `BaseDAO`, ids das sequences, transações JDBC (herança joined e `TransacaoDAO.registrar`) e leitura polimórfica de `carteira`; datas em `LocalDate` e construtores de reconstrução no modelo; DDL revisado (marcas `-- [Fase 6]`) e Dicionário de Dados regerado (`modelo/Dicionario_de_Dados.pdf`); `Main` movida para `com.criptoinvest.Main` como teste de INSERT/SELECT/UPDATE/DELETE de todas as tabelas — ver [Fase 6](#fase-6--integração-completa-com-o-banco) |

## Fase 6 — Integração completa com o banco

Todas as classes do modelo são gravadas e lidas pelo seu DAO, e todas as 10 tabelas do DDL têm
INSERT, UPDATE, DELETE e SELECT em Java. Cada DAO estende `BaseDAO` e implementa `CrudDAO<T>`:

- `inserir` obtém a PK com `SELECT seq_x.NEXTVAL FROM dual`, grava a linha e só então atribui o
  id ao objeto; devolve o id gerado. Um objeto que já tem id (> 0) é recusado com
  `IllegalStateException` ("use alterar");
- `alterar` e `excluir` devolvem as linhas afetadas (0 = id inexistente);
- `buscarPorId` devolve `null` quando a linha não existe e `listarTodos` devolve um `ArrayList`
  ordenado pela PK;
- todo SQL usa `PreparedStatement` com parâmetros `?`, sem concatenar valores, e nenhuma string
  SQL termina com `;` (o driver do Oracle recusa);
- as conversões ficam no `BaseDAO`: `LocalDate` ↔ `DATE`, `boolean` ↔ `'S'/'N'` e texto vazio →
  `NULL` (o Oracle grava `''` como `NULL`). Os `BigDecimal` lidos são normalizados com a escala
  da coluna (`Valores.dinheiro`, `cripto` e `percentual`), porque o Oracle devolve `175` e não
  `175.00`;
- as FKs são conferidas antes do SQL: gravar uma transação com carteira ou criptoativo ainda não
  persistidos, ou uma empresa cujo dono tem id 0, gera `IllegalStateException` com mensagem clara;
- usuário e senha do banco ficam só na `ConnectionFactory`.

### DAOs, tabelas e métodos

| DAO | Tabela(s) | INSERT | UPDATE | DELETE | SELECT |
|---|---|---|---|---|---|
| `CriptoativoDAO` | `criptoativo` | `inserir` | `alterar` | `excluir` | `buscarPorId`, `buscarPorSigla`, `listarTodos` |
| `CarteiraPFDAO` | `carteira` + `carteira_pf` | `inserir` (pai e filha) | `alterar` (pai e filha) | `excluir` (filha e pai) | `buscarPorId`, `listarTodos` (`JOIN`) |
| `CarteiraPJDAO` | `carteira` + `carteira_pj` | `inserir` (pai e filha) | `alterar` (pai e filha) | `excluir` (filha e pai) | `buscarPorId`, `listarTodos` (`JOIN`) |
| `CarteiraDAO` | `carteira` (+ filhas) | `inserir` polimórfico (delega ao DAO da filha) | `alterar` (só a tabela pai: descrição e saldo), `atualizarSaldo` | `excluir` (filha e pai) | `buscarPorId`, `listarTodos`, `listarPorTipo` (`LEFT JOIN`, polimórficos), `carregarHistorico` |
| `UsuarioDAO` | `usuario` (+ carteira PF) | `inserir` (carteira PF e usuário) | `alterar` (usuário e carteira PF) | `excluir` (usuário e carteira PF) | `buscarPorId`, `buscarPorCpf`, `buscarPorEmail`, `listarTodos` |
| `EmpresaDAO` | `empresa` (+ carteira PJ) | `inserir` (carteira PJ e empresa; exige o dono já gravado) | `alterar` (empresa e carteira PJ) | `excluir` (empresa e carteira PJ) | `buscarPorId`, `buscarPorCnpj`, `listarPorUsuario`, `listarTodos` |
| `TransacaoDAO` | `transacao` (+ `posicao` e `carteira` no `registrar`) | `registrar` (transação, posição e saldo); `inserir` (só a linha) | `alterar` (só a linha) | `excluir` (só a linha) | `buscarPorId`, `listarPorCarteira`, `listarTodos` |
| `PosicaoDAO` | `posicao` | `inserir` | `alterar` | `excluir` | `buscarPorId`, `buscarPorCarteiraECripto`, `listarPorCarteira`, `listarTodos` |
| `RelatorioDAO` | `relatorio` | `inserir` (grava o snapshot) | `alterar` (regrava o snapshot) | `excluir` | `buscarPorId`, `listarPorCarteira`, `listarTodos` (sem recalcular) |
| `AlertaDAO` | `alerta` | `inserir` | `alterar` (`ativado` ↔ `'S'/'N'`) | `excluir` | `buscarPorId`, `listarPorUsuario`, `listarAtivos`, `listarTodos` |

Na leitura, as FKs viram objetos montados na mesma conexão: a transação, a posição e o
relatório voltam com a carteira (já como `CarteiraPF` ou `CarteiraPJ`), a empresa com o dono e o
alerta com o usuário e o criptoativo. A carteira volta sem histórico (use
`CarteiraDAO.carregarHistorico`) e o usuário sem as empresas (use `EmpresaDAO.listarPorUsuario`).

### Transações JDBC

Cada método público abre a própria conexão pela `ConnectionFactory`. As operações que tocam mais
de uma tabela abrem **uma** conexão, desligam o autocommit, executam tudo e fazem `commit` no fim;
em qualquer erro fazem `rollback`, devolvem a 0 os ids que já tinham atribuído aos objetos e
relançam a exceção. Para compor operações de vários DAOs na mesma transação, os DAOs expõem
sobrecargas de pacote (sem `public`) que recebem a `Connection`.

**Herança joined (`carteira` → `carteira_pf` | `carteira_pj`):** pai, filha e titular são
gravados juntos, ou nada é gravado.

| Operação | Comandos, na mesma transação |
|---|---|
| `CarteiraPFDAO.inserir` / `CarteiraPJDAO.inserir` | `NEXTVAL` de `seq_carteira` → `INSERT carteira` (`tipo` = `PF`/`PJ`) → `INSERT carteira_pf`/`carteira_pj` com o mesmo id |
| `UsuarioDAO.inserir` / `EmpresaDAO.inserir` | os dois INSERTs da carteira acima → `INSERT usuario`/`empresa` apontando para ela |
| `UsuarioDAO.alterar` / `EmpresaDAO.alterar` | `UPDATE usuario`/`empresa` → `UPDATE carteira` → `UPDATE carteira_pf`/`carteira_pj` |
| `UsuarioDAO.excluir` / `EmpresaDAO.excluir` | `DELETE usuario`/`empresa` → `DELETE carteira_pf`/`carteira_pj` → `DELETE carteira` |
| `CarteiraDAO.excluir` | `DELETE carteira_pf` e `DELETE carteira_pj` (um deles afeta 0 linhas) → `DELETE carteira` |

Se um usuário ainda tem empresa ou alerta, ou a carteira tem transação, posição ou relatório, o
Oracle recusa o DELETE com `ORA-02292` e a transação inteira é desfeita. Na leitura polimórfica,
`CarteiraDAO` faz `LEFT JOIN` nas duas filhas e a coluna `tipo` decide a subclasse. Como o banco
não amarra o discriminador à tabela filha, uma linha incoerente (tipo `PF` sem linha em
`carteira_pf`, ou presente nas duas filhas) gera `SQLException` em vez de um objeto montado pela
metade.

**`TransacaoDAO.registrar` (transação + posição + saldo, atômico):** grava numa única transação
o que `Carteira.registrarTransacao` já aplicou em memória:

1. `INSERT` da transação (id de `seq_transacao`);
2. a posição da carteira para o criptoativo, por "upsert" na UK `(id_carteira, id_cripto)`:
   `UPDATE` se a linha existe, `INSERT` se não existe;
3. `UPDATE` do `saldo_reais` da carteira.

Uso: `if (carteira.registrarTransacao(t)) transacaoDAO.registrar(t);`. O saldo é gravado como
valor absoluto, então a carteira precisa ter vindo do banco (`CarteiraDAO.buscarPorId` seguido de
`carregarHistorico`) ou ter sido criada na execução, e não pode haver outra cópia em memória da
mesma carteira registrando operações. Se `registrar` falhar, o banco volta ao estado anterior,
mas a operação continua aplicada em memória: a carteira fica bloqueada para novos `registrar` até
ser recarregada do banco. `carregarHistorico` recusa uma cópia cujo saldo difere do gravado (ou
uma carteira que não existe mais).

### Como a `Main` testa cada tabela

Cada verificação roda isolada no método `testar` (o `try` de cada passo fica nele), então uma
falha não interrompe as seguintes; um passo cujo pré-requisito falhou aparece como não executado
(`--`), sem contar como falha. Para cada tabela a `Main` faz INSERT, SELECT (por id e listagem em
`ArrayList`, impressa uma linha por registro, com `*` nas criadas na execução) e UPDATE com
releitura do banco provando a mudança; os DELETEs ficam na limpeza, que roda num `finally`.

| Seção | O que testa |
|---|---|
| 0. Conexão | `ConnectionFactory.testarConexao` (mostra URL e usuário, nunca a senha) e contagem inicial de cada tabela |
| 1. `criptoativo` | INSERT de dois criptoativos, SELECT por id/lista/sigla, UPDATE do preço com releitura, UK da sigla (`ORA-00001`) |
| 2–3. `carteira_pf` / `carteira_pj` | carteiras avulsas: INSERT (pai + filha), SELECT, UPDATE com releitura |
| 4. `usuario` | INSERT com a carteira PF, SELECT por id/CPF/e-mail, UPDATE com releitura |
| 5. `empresa` | INSERT com a carteira PJ, SELECT por id/CNPJ/usuário, UPDATE com releitura |
| 6. `carteira` | INSERT polimórfico, leitura polimórfica, `listarPorTipo`, UPDATE da tabela pai, `atualizarSaldo` |
| 7. `transacao` | INSERT via `registrar` (compras e venda na carteira PF, compra na PJ; saldo e posição relidos), `inserir` do CRUD em verificação própria (grava só a linha), compra recusada, FK polimórfica, SELECT e UPDATE |
| 8. `posicao` | posições gravadas por `registrar` + INSERT direto, SELECT por id/UK/carteira, UPDATE com releitura |
| 9. `relatorio` | INSERT do snapshot, leitura sem recálculo, `carregarHistorico` (relatório igual ao da memória), UPDATE regravando o snapshot |
| 10. `alerta` | INSERT, SELECT, desativação (S → N) com releitura, `listarAtivos`, UK usuário × criptoativo |
| 11. Integridade | excluir criptoativo e usuário com dependentes é recusado com `ORA-02292` (rollback) |
| 12. Limpeza | DELETE de tudo o que foi criado, em ordem reversa das FKs, conferindo `buscarPorId == null` — são os testes de DELETE |
| 13. Resumo | contagem final = inicial em cada tabela e matriz tabela × operação com o total de falhas |

O **resumo final** mostra uma matriz com uma linha por tabela e uma coluna por operação
(`OK`, `FALHA` ou `--` quando não foi executada) e a coluna `OUTRAS` com o placar das demais
verificações (chaves naturais, UKs, FKs, polimorfismo, limpeza), que vêm listadas uma a uma logo
abaixo. Depois aparecem o detalhe das falhas (se houver), a contagem geral e o resultado. Fim de
uma execução sem falhas:

```
  TABELA       INSERT  SELECT  UPDATE  DELETE  OUTRAS
  criptoativo  OK      OK      OK      OK      4/4 OK
  carteira_pf  OK      OK      OK      OK      1/1 OK
  ...
  transacao    OK      OK      OK      OK      5/5 OK
  ...
  alerta       OK      OK      OK      OK      3/3 OK

Outras verificacoes:
  OK    criptoativo  chave natural: buscarPorSigla
  OK    criptoativo  UK sigla duplicada (ORA-00001)
  ...
  OK    alerta       limpeza: contagem final = inicial

Verificacoes: 66 OK | 0 FALHA | 0 nao executadas
TOTAL DE FALHAS: 0
RESULTADO: integracao JDBC aprovada nas 10 tabelas.
```

Os erros de banco mais comuns aparecem com mensagem própria, seguida do código, do SQLState e da
mensagem do driver: `ORA-00001` (chave única), `ORA-02291`/`ORA-02292` (FK), `ORA-02290`
(CHECK), `ORA-01400` (NOT NULL), `ORA-01438` (número acima da precisão da coluna), `ORA-12899`
(texto acima do tamanho) e tabela ou sequence inexistente (DDL não executado).

### Alterações do DDL na Fase 6

O `sql/criptoinvest_ddl.sql` continua sendo um script único e completo; cada mudança está marcada
com `-- [Fase 6]`. Para levar um schema da Fase 5 para a Fase 6, basta rodá-lo de novo (ele
apaga e recria tudo, então rode o DML em seguida se quiser os dados de exemplo).

| Item | Fase 5 | Fase 6 | Por quê |
|---|---|---|---|
| `carteira.descricao` | `VARCHAR2(100)` | `VARCHAR2(200)` | o Java monta a descrição como prefixo + nome do titular (`"Carteira PF de "` tem 15 caracteres e o nome até 150), o que podia passar de 100 |
| `transacao.taxa` | `NUMBER(15,4)` | `NUMBER(15,2)` | a taxa é dinheiro: o Java a calcula e arredonda com 2 casas (`Valores.dinheiro`), e o DDL dizia 4 |
| `relatorio.total_taxas` | `NUMBER(15,4)` | `NUMBER(15,2)` | soma das taxas, com a mesma escala de `transacao.taxa` |
| `uk_alerta_usuario_cripto` | não existia | `UNIQUE (id_usuario, id_cripto)` | `alerta` é a associativa usuário × criptoativo: nada impedia alertas repetidos, e o próprio DML (UPDATE 2.8) já identificava o alerta pelo par |
| `idx_posicao_cripto` | não existia | índice em `posicao (id_cripto)` | a FK `fk_posicao_cripto` não tinha índice de apoio (o índice da UK começa por `id_carteira`) |
| `COMMENT ON` | — | colunas alteradas e tabela `alerta` | documentação das mudanças no próprio banco |

### Inconsistências da Fase 5 corrigidas

A revisão feita antes da Fase 6 comparou o modelo Java com o DDL e o DML. Além das mudanças do
DDL acima, o modelo e os DAOs foram corrigidos nestes pontos:

- **Datas em `LocalDate`:** as colunas `DATE NOT NULL` (`transacao.data_operacao`, as duas datas
  de `posicao`, `alerta.data_configuracao` e `relatorio.data_geracao`) eram `String` no Java, sem
  validação — aceitavam `"2026-02-30"`, `"07/05/2026"` ou texto vazio. Agora são `LocalDate`,
  gravadas com `setDate` e lidas com `getDate(...).toLocalDate()`; as sobrecargas que recebem
  texto convertem `"yyyy-MM-dd"` pela classe `Datas` e recusam datas inválidas.
- **Ids pela sequence:** `Carteira` e `Posicao` geravam o id com contadores estáticos, que
  recomeçavam em 1 a cada execução e não conheciam `seq_carteira`/`seq_posicao`, e o
  `CriptoativoDAO` gravava o id do objeto quando ele era > 0. Os contadores foram removidos: todo
  `inserir` usa a sequence, id 0 significa "não persistido" e o id só é atribuído depois do
  INSERT. `Usuario` e `Empresa` passaram a ter `idUsuario`/`idEmpresa` com setter, como as
  demais entidades.
- **Construtores de reconstrução:** não havia como montar um objeto a partir de uma linha
  gravada sem recalcular valores ou criar uma carteira nova. Agora `CarteiraPF`/`CarteiraPJ`
  recebem id, descrição e saldo; `Usuario` e `Empresa` recebem a carteira já montada; `Transacao`
  recebe a taxa gravada (sem recalcular os 0,1%); `Posicao` recebe quantidade, preço médio e
  datas; `Relatorio` recebe os totais do snapshot. `Carteira.carregarHistorico` substitui as
  listas de transações e posições sem aplicar as regras de negócio (uso da camada de persistência).
- **Escalas e tamanhos alinhados ao DDL:** taxa e total de taxas com 2 casas, descrição da
  carteira com até 200 caracteres, um alerta por usuário × criptoativo e índice de apoio à FK de
  `posicao` (tabela acima).
- **Outros ajustes:** sigla do criptoativo normalizada para maiúsculas (a UK diferencia caixa);
  quantidade e limite do alerta arredondados **antes** de validar `> 0` (um valor positivo muito
  pequeno virava 0 e violava o CHECK); conversão `'S'/'N'` centralizada no `BaseDAO`; percentuais
  limitados à precisão de `NUMBER(10,4)`; `Main` fora do pacote `model` (evita o ciclo
  `model` ↔ `dao`), com chaves naturais exclusivas por execução e um `try` por teste.

### Limitações conhecidas

- **Saques do dia da `CarteiraPF` não persistem:** o acumulado de saques do dia
  (`saquesHoje`/`dataUltimoSaque`) fica só em memória, porque `carteira_pf` não tem coluna para
  ele. Uma carteira relida do banco recomeça com o acumulado zerado, então o limite diário vale
  apenas dentro de um mesmo objeto. Depósitos e saques também não geram linha no banco: só o saldo
  final é gravado (`CarteiraDAO.alterar` ou `atualizarSaldo`). Persistir o acumulado (ex.: colunas
  `saques_hoje` e `data_ultimo_saque` em `carteira_pf`) fica como melhoria futura.
- **MER e Normalização não foram atualizados:** `modelo/ModeloEntidadeRelacionamento.png` e
  `modelo/Normalizacao.pdf` ainda são os da Fase 4. Eles não têm `transacao.observacao`
  (criada na Fase 5) nem as alterações da Fase 6 (`descricao` com 200, `taxa`/`total_taxas`
  com 2 casas, `uk_alerta_usuario_cripto`, `idx_posicao_cripto`); a seção 2.10 da normalização
  ainda diz que o alerta não tem UK composta, e alguns nomes de restrição do MER diferem do DDL
  (ex.: `uk_posicao_carteira_cripto` × `uk_posicao_cart_cripto`). O Dicionário de Dados já foi
  regerado; a fonte de verdade é sempre `sql/criptoinvest_ddl.sql`.
- **DML com percentuais arredondados:** o DML grava percentuais com 2 casas, enquanto o Java
  calcula com 4 — variação do BTC `16.67` no INSERT e `8.57` no UPDATE 2.1 (no Java, `16.6667` e
  `8.5714`) e rentabilidade dos relatórios `-0.12` e `-0.10` (no Java, `-0.1168` e `-0.0999`).
  Os UPDATEs 2.6 e 2.7 e os DELETEs 3.2 e 3.3 do DML também alteram posições e transações
  diretamente, sem a regra da carteira (saldo e posição deixam de corresponder às transações), e a
  consulta 4.7 ("Total investido por carteira") soma compras e vendas pelo valor bruto — é o total
  movimentado. Os dados do DML são só exemplo: a `Main` não depende deles.
- **`TransacaoDAO.alterar` e `excluir` mexem só na linha:** corrigem ou apagam o registro, mas
  não refazem o saldo da carteira nem a posição. Operações de compra e venda devem passar por
  `Carteira.registrarTransacao` + `TransacaoDAO.registrar`.
- **Ids `int`:** as PKs e FKs são `NUMBER(10)` no banco e `int` no Java, então valem até
  2.147.483.647 — folga suficiente para o projeto.

## Fase 5 — Integração com Banco de Dados (histórico)

> Registro da entrega da Fase 5. Os métodos de teste citados abaixo (`testarInserir()`,
> `executarTestesBancoDeDados()` etc.) pertenciam à `Main` antiga, substituída na Fase 6 pelo
> roteiro descrito acima; o `CriptoativoDAO` continua e agora segue o mesmo contrato dos demais DAOs.

**Classe escolhida para a integração:** `Criptoativo` → tabela `CRIPTOATIVO`.

| Operação | Método do `CriptoativoDAO` | Comando SQL | Método de teste na `Main` (Fase 5) |
|---|---|---|---|
| Inserir | `inserir(Criptoativo)` | `INSERT` (id via `seq_criptoativo.NEXTVAL`) | `testarInserir()` |
| Exibir todos | `listarTodos()` | `SELECT ... ORDER BY id_cripto` | `testarExibirTodos()` |
| Exibir um | `buscarPorId(int)` / `buscarPorSigla(String)` | `SELECT ... WHERE` | `testarExibirPorId()` |
| Alterar | `alterar(Criptoativo)` | `UPDATE` | `testarAlterar()` |
| Excluir | `excluir(int)` | `DELETE` | `testarExcluir()` |
