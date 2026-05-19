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

## Como Executar

O projeto é Java puro, sem framework ou build tool. Compile e execute diretamente com `javac`/`java`.

**Compilar:**
```
javac src/com/criptoinvest/model/*.java -d out/
```

**Executar:**
```
java -cp out/ com.criptoinvest.model.Main
```

**Modelo relacional (Oracle):** o script DDL com tabelas, sequences, constraints e dados de exemplo está em `modelo/criptoinvest_ddl.sql`.

## Estrutura de Classes

Todas as classes estão no pacote `com.criptoinvest.model`:

```
src/com/criptoinvest/model/
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
└── Main.java           → Ponto de entrada com demonstração de todas as funcionalidades
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

## Conceitos de POO Aplicados

| Conceito | Implementação |
|---|---|
| **Encapsulamento** | Todos os campos são `private`/`protected`, acessados via getters/setters |
| **Herança** | `CarteiraPF` e `CarteiraPJ` estendem `Carteira` (joined inheritance) |
| **Polimorfismo dinâmico** | `getTipo()` abstrato em `Carteira`, sobrescrito nas filhas; `sacar()` sobrescrito em `CarteiraPF` para aplicar limite diário |
| **Polimorfismo estático** | Sobrecarga de `depositar`, `registrarTransacao` e `atualizarPreco` |

## Tecnologias

- **Linguagem:** Java SE
- **JDK:** OpenJDK 26
- **Banco de Dados:** Oracle 19c+ (DDL em `modelo/criptoinvest_ddl.sql`)

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
| Sprint 4 — Fase 4 | Modelo Relacional SQL (DDL Oracle, herança joined em Carteira) |
