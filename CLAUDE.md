# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CriptoInvest is an academic Java SE project (FIAP — Engenharia de Software, team VOLTZ) modeling a cryptocurrency portfolio management platform. It is currently a pure domain-model layer with no build tool, no framework, and no entry point — only class definitions.

## Commands

This is a plain Java project with no Maven or Gradle. Compile and run directly with `javac`/`java`.

**Compile all classes:**
```
javac src/*.java -d out/
```

**Run a class (e.g., a manual test main):**
```
java -cp out/ ClassName
```

There is no test framework configured. Verification is done by writing a `main` method in a scratch class, compiling, and running it.

## Architecture

All classes live in `src/` with no package declaration. The domain model is structured around a single root aggregate (`Usuario`) that owns `Carteira` and `Empresa` instances.

### Key design decisions

**Fixed-size arrays instead of collections.** Every relationship uses plain arrays with a manual counter:
- `Usuario`: up to 10 `Carteira[]`, 10 `Empresa[]`
- `Carteira`: up to 100 `Transacao[]`

When adding items, always check the counter before writing to the array. The counter fields (`totalCarteiras`, `totalTransacoes`, etc.) serve as the "size" of the array.

**Package-private fields with getters/setters.** Fields are declared without an access modifier (package-private), not `private`. Internal code across classes still accesses fields directly (e.g., `transacoes[i].criptoativo.sigla`, `criptoativo.variacao24h`). Getters/setters exist for external access but are not enforced internally.

**Transaction fee is fixed at 0.1%.** `Transacao` calculates `taxa = calcularValorBruto() * 0.001` at construction time. `calcularValorComTaxa()` adds tax for COMPRA and subtracts it for VENDA.

**`Relatorio` is a snapshot, not live.** Its financial fields (`valorTotalCarteira`, `totalInvestido`, etc.) are captured from `Carteira` at construction time and do not reflect subsequent changes to the wallet.

**`Alerta` reads `criptoativo.variacao24h` directly.** This field is only updated when `Criptoativo.atualizarPreco()` is called, and starts at `0` in the constructor. An alert will not fire unless the price has been explicitly updated at least once.

**`Transacao.precoUnitario` is a snapshot; `calcularValorAtual()` is live.** At construction, `precoUnitario` captures `criptoativo.precoAtual`. `calcularValorBruto()` and `taxa` use this snapshot forever. `calcularValorAtual()` reads `criptoativo.precoAtual` at call time, so its result changes as the asset price is updated.

**`calcularLucro()` is only meaningful for COMPRA.** For VENDA and CONVERSAO it always returns `0`.

### Transaction types

Valid `tipo` values are the string constants `"COMPRA"`, `"VENDA"`, and `"CONVERSAO"`. The constructor defaults to `"COMPRA"` if an invalid type is passed.

### Class relationships

```
Usuario   1 ──── * Carteira      (personal wallets)
Usuario   1 ──── * Empresa       (corporate CNPJs)
Empresa   1 ──── 1 Carteira      (auto-created in Empresa constructor)
Carteira  1 ──── * Transacao
Transacao * ──── 1 Criptoativo
Relatorio * ──── 1 Carteira      (snapshot at creation time)
Alerta    * ──── 1 Criptoativo
```

### Known bugs / gotchas

**`Carteira.calcularValorTotal()` double-counts assets with multiple COMPRA transactions.** The method iterates over every COMPRA transaction and adds `saldoCripto * precoAtual` for each one. If the same asset was bought twice, the net balance is added twice (once per COMPRA row), inflating the total. Any fix must deduplicate by asset sigla before summing.

**`Empresa` uses `idEmpresa` as the wallet ID.** `Empresa`'s constructor passes `id` directly to `new Carteira(id, ...)`. If a user-created `Carteira` shares the same numeric ID, both objects will have identical `idCarteira` values. IDs are not enforced as unique anywhere in the model.