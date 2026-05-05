# CriptoInvest

**Sistema de Gestão de Investimentos em Criptoativos**

Projeto acadêmico desenvolvido pela equipe **VOLTZ Engenharia de Software** para a disciplina de Engenharia de Software — FIAP.

---

## Sobre o Projeto

O CriptoInvest é uma plataforma web voltada para investidores pessoa física e jurídica que desejam gerenciar seus portfólios de criptoativos de forma centralizada, segura e com acompanhamento diário de desempenho.

O sistema resolve problemas reais enfrentados por investidores no mercado cripto: fragmentação de ativos em múltiplas exchanges, falta de acompanhamento em tempo real, ausência de relatórios gerenciais e dificuldade na gestão de investimentos distribuídos entre múltiplas empresas (CNPJs).

## Contexto

O mercado de criptoativos ultrapassou a marca de 2,5 trilhões de dólares em capitalização global, e o Brasil ocupa a 5ª posição no Índice Global de Adoção de Criptomoedas (Chainalysis). Apesar desse crescimento, investidores ainda carecem de ferramentas que combinem gestão multiempresa, relatórios robustos e segurança em uma única plataforma.

## Público-Alvo

O sistema atende três perfis principais de usuários:

- **Empresários** com múltiplos CNPJs que buscam controle centralizado dos investimentos em criptoativos de cada empresa.
- **Investidores experientes** que utilizam aplicativos financeiros e desejam monitoramento em tempo real com dashboards intuitivos.
- **Investidores iniciantes** que estão entrando no mercado cripto e precisam de uma interface simples e didática.

## Funcionalidades

- **Dashboard de portfólio** com visão consolidada dos criptoativos, gráficos de evolução patrimonial e indicadores de rentabilidade.
- **Gestão de investimentos** com registro de aportes, histórico de transações e simulador de rentabilidade.
- **Gestão multiempresa** com cadastro de múltiplos CNPJs, separação de portfólios por empresa e visão gerencial unificada.
- **Segurança** com autenticação em dois fatores (2FA), criptografia de dados e conformidade com a LGPD.
- **Relatórios e exportação** com relatórios diários de performance, exportação em PDF/CSV/XML e integração com declaração de IR.
- **Alertas** configuráveis para variações de preço dos criptoativos monitorados.

## Estrutura de Classes

O projeto é composto por 7 classes:

```
src/
├── Usuario.java       → Cadastro do usuário com 2FA e vinculação a carteiras e empresas
├── Empresa.java       → Representa um CNPJ vinculado ao usuário
├── Carteira.java      → Agrupa transações e calcula valor total e rentabilidade
├── Criptoativo.java   → Representa uma criptomoeda (BTC, ETH, etc.)
├── Transacao.java     → Registro de compra, venda ou conversão de um criptoativo (taxa de 0,1%)
├── Relatorio.java     → Snapshot de desempenho de uma carteira em determinada data
└── Alerta.java        → Monitora variação de preço e dispara quando ultrapassa o limite
```

### Diagrama de Classes (resumo dos relacionamentos)

```
Usuario   1 ──── * Carteira
Usuario   1 ──── * Empresa
Empresa   1 ──── 1 Carteira
Carteira  1 ──── * Transacao
Transacao * ──── 1 Criptoativo
Relatorio * ──── 1 Carteira
Alerta    * ──── 1 Criptoativo
```

## Tecnologias

- **Linguagem:** Java (SE)
- **Back-end (previsto):** Java

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

