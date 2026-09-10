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
> implementado até a Fase 5 é o modelo de domínio em Java, a persistência em arquivos texto,
> o modelo relacional Oracle e a integração JDBC da classe `Criptoativo` — sem interface
> gráfica, sem cotações em tempo real e sem a camada de segurança (2FA e criptografia são
> requisitos de produto, ainda não implementados).

## Como Executar

O projeto é Java puro. Compile e execute direto com `javac`/`java`; o Maven (`pom.xml`) é
opcional e serve apenas para baixar o driver JDBC do Oracle usado na Fase 5.

**Compilar:**
```
javac -sourcepath src -d out src/com/criptoinvest/model/Main.java
```
O `-sourcepath` faz o compilador puxar sozinho as classes dos três pacotes (`model`, `dao`
e `factory`), sem precisar listar cada um.

**Executar:**
```
java -cp out com.criptoinvest.model.Main
```

Sem o driver do Oracle no classpath, as Fases 1 a 4 rodam normalmente e a Fase 5 apenas avisa
que não há conexão, sem interromper a demonstração.

### Compilar e executar com integração ao banco (Fase 5)

A integração com o Oracle exige o driver JDBC (`ojdbc8`) no classpath. Há duas formas:

**Com Maven (recomendado):**
```
mvn compile
mvn exec:java -Dexec.mainClass=com.criptoinvest.model.Main
```

**Com javac/java + driver baixado manualmente em `lib/`:**
```
javac -cp lib/ojdbc8.jar -sourcepath src -d out src/com/criptoinvest/model/Main.java

java -cp "out;lib/ojdbc8.jar" com.criptoinvest.model.Main   # Windows
java -cp "out:lib/ojdbc8.jar" com.criptoinvest.model.Main   # Linux / macOS
```
Atenção ao separador do classpath: `;` no Windows e `:` no Linux e no macOS.

### Credenciais do banco

Crie um arquivo `.env` na raiz do projeto. Ele guarda credenciais reais, está listado no
`.gitignore` e **nunca deve ser versionado**:
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
java -Ddb.user=rmXXXXXX -Ddb.password=SUA_SENHA -cp "out;lib/ojdbc8.jar" com.criptoinvest.model.Main
```

**Scripts SQL (Oracle):** execute na ordem `sql/criptoinvest_ddl.sql` (estrutura) e depois
`sql/criptoinvest_dml.sql` (dados e consultas).

## Estrutura de Classes

O código está dividido em três pacotes: o domínio em `com.criptoinvest.model`, o acesso a
dados em `com.criptoinvest.dao` e a conexão em `com.criptoinvest.factory`.

```
src/com/criptoinvest/
├── factory/
│   └── ConnectionFactory.java  → Conexão com o Oracle da FIAP (URL, usuário, senha, timeout)
├── dao/
│   └── CriptoativoDAO.java     → CRUD da classe Criptoativo (INSERT/UPDATE/DELETE/SELECT)
└── model/
    ├── Carteira.java       → Classe abstrata pai da herança (joined): saldo, transações, cálculos
    ├── CarteiraPF.java     → Carteira de Pessoa Física (limite diário de saque)
    ├── CarteiraPJ.java     → Carteira de Pessoa Jurídica (regime tributário)
    ├── Usuario.java        → Pessoa Física: 1:1 com CarteiraPF, 1:N com Empresa
    ├── Empresa.java        → Pessoa Jurídica: 1:1 com CarteiraPJ, N:1 com Usuario (dono)
    ├── Criptoativo.java    → Representa uma criptomoeda (BTC, ETH, etc.)
    ├── Transacao.java      → Registro de compra, venda ou conversão (taxa de 0,1%)
    ├── Posicao.java        → Associativa Carteira ↔ Criptoativo (saldo agregado)
    ├── Alerta.java         → Associativa Usuario ↔ Criptoativo (limite de variação)
    ├── Relatorio.java      → Snapshot de desempenho de uma carteira em determinada data
    └── Main.java           → Ponto de entrada: demonstração do domínio + testes de banco
```

### Scripts SQL

```
sql/
├── criptoinvest_ddl.sql  → DDL: DROP, CREATE (tabelas/sequences/índices) e ALTER (PKs, FKs, UKs)
└── criptoinvest_dml.sql  → DML: INSERT (população), UPDATE, DELETE e SELECT (consultas gerenciais)
```

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

Como `carteira` é uma tabela única para PF e PJ, o id da carteira **não** vem do titular: `Carteira` gera o seu próprio id a partir de um contador estático, espelhando a sequence `seq_carteira`. `Posicao` segue a mesma regra (`seq_posicao`), com contador único para todo o sistema — um contador por carteira faria duas carteiras diferentes gerarem a mesma PK.

## Conceitos de POO Aplicados

| Conceito | Implementação |
|---|---|
| **Encapsulamento** | Todos os campos são `private`/`protected`, acessados via getters/setters |
| **Herança** | `CarteiraPF` e `CarteiraPJ` estendem `Carteira` (joined inheritance) |
| **Polimorfismo dinâmico** | `getTipo()` abstrato em `Carteira`, sobrescrito nas filhas; `sacar()` sobrescrito em `CarteiraPF` para aplicar limite diário |
| **Polimorfismo estático** | Sobrecarga de `depositar`, `registrarTransacao` e `atualizarPreco` |

## Tecnologias

- **Linguagem:** Java SE
- **JDK:** compilado para Java 17 (`maven.compiler.source/target` no `pom.xml`); testado no OpenJDK 26
- **Banco de Dados:** Oracle 19c+ (FIAP) — scripts em `sql/`
- **Acesso a dados:** JDBC puro (driver `ojdbc8`), padrão DAO + Connection Factory
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
| Sprint 4 — Fase 4 | Modelo Relacional SQL (DDL Oracle, herança joined em Carteira); Normalização 1FN/2FN/3FN (`modelo/Normalizacao.pdf`); persistência em arquivos texto a partir de `ArrayList` e `HashMap` na `Main` |
| Sprint 5 — Fase 5 | Script DDL (`sql/criptoinvest_ddl.sql`) e script DML (`sql/criptoinvest_dml.sql`); classe de conexão com o Oracle da FIAP (`ConnectionFactory`); integração completa da classe `Criptoativo` com o banco (`CriptoativoDAO`: inserir, alterar, excluir e exibir); métodos de teste dessa integração na `Main` |

## Fase 5 — Integração com Banco de Dados

**Classe escolhida para a integração:** `Criptoativo` → tabela `CRIPTOATIVO`.

| Operação | Método do `CriptoativoDAO` | Comando SQL | Método de teste na `Main` |
|---|---|---|---|
| Inserir | `inserir(Criptoativo)` | `INSERT` (id via `seq_criptoativo.NEXTVAL`) | `testarInserir()` |
| Exibir todos | `listarTodos()` | `SELECT ... ORDER BY id_cripto` | `testarExibirTodos()` |
| Exibir um | `buscarPorId(int)` / `buscarPorSigla(String)` | `SELECT ... WHERE` | `testarExibirPorId()` |
| Alterar | `alterar(Criptoativo)` | `UPDATE` | `testarAlterar()` |
| Excluir | `excluir(int)` | `DELETE` | `testarExcluir()` |

A `Main` chama `executarTestesBancoDeDados()`, que testa a conexão e, em seguida, executa
inserção → listagem → busca por id → alteração → exclusão → listagem final. Se o banco não
estiver acessível, a aplicação avisa e segue sem interromper a demonstração das demais fases.
