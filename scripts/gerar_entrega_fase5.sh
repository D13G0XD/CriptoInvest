#!/usr/bin/env bash
# ============================================================================
# CriptoInvest - VOLTZ | Gera o arquivo .zip da entrega da Fase 5
#
# Compacta todos os arquivos .sql e .java do projeto, preservando a estrutura
# de pacotes exigida pelo Java.
#
# Uso: ./scripts/gerar_entrega_fase5.sh
# Saida: entrega/CriptoInvest_Fase5_VOLTZ.zip
# ============================================================================
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SAIDA="$RAIZ/entrega"
ZIP="$SAIDA/CriptoInvest_Fase5_VOLTZ.zip"

cd "$RAIZ"
mkdir -p "$SAIDA"
rm -f "$ZIP"

zip -r "$ZIP" sql src -i '*.sql' '*.java' -x '*/target/*'

echo
echo "Arquivo gerado: $ZIP"
unzip -l "$ZIP"
