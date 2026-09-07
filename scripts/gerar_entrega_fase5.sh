#!/usr/bin/env bash
# ============================================================================
# CriptoInvest - VOLTZ | Gera o arquivo .zip da entrega da Fase 5
#
# Compacta todos os arquivos .sql e .java do projeto, preservando a estrutura
# de pacotes exigida pelo Java.
#
# O zip e montado em um arquivo temporario e so substitui a entrega quando
# termina bem, para que uma falha no meio do caminho nao deixe o projeto sem
# o arquivo ja existente.
#
# Uso: ./scripts/gerar_entrega_fase5.sh
# Saida: entrega/CriptoInvest_Fase5_VOLTZ.zip
# ============================================================================
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SAIDA="$RAIZ/entrega"
ZIP="$SAIDA/CriptoInvest_Fase5_VOLTZ.zip"
TEMP="$ZIP.novo"

if ! command -v zip >/dev/null 2>&1; then
    echo "Erro: o utilitario 'zip' nao esta instalado ou nao esta no PATH." >&2
    echo "A entrega existente foi preservada, nada foi alterado." >&2
    echo >&2
    echo "  Debian/Ubuntu: sudo apt install zip" >&2
    echo "  macOS........: brew install zip" >&2
    echo "  Windows......: compacte as pastas sql/ e src/ manualmente, ou use" >&2
    echo "                 um Git Bash que inclua o zip." >&2
    exit 1
fi

cd "$RAIZ"
mkdir -p "$SAIDA"

rm -f "$TEMP"
trap 'rm -f "$TEMP"' EXIT

zip -r "$TEMP" sql src -i '*.sql' '*.java' -x '*/target/*'
mv -f "$TEMP" "$ZIP"

echo
echo "Arquivo gerado: $ZIP"
# A listagem e so informativa: se falhar, o zip ja foi gerado e o script
# nao deve terminar com erro por causa dela.
if command -v unzip >/dev/null 2>&1; then
    unzip -l "$ZIP" || true
fi
