#!/usr/bin/env bash
# Fetches the OCR model pinned by checksum. Safe to re-run; shared by setup.sh and CI.
set -euo pipefail
cd "$(dirname "$0")/.."

model=core/src/androidMain/assets/ocr/captcha.onnx
hash=b8f2ad9cbc1f2e3922a6cb9459e30824e7e2467f3fb4fd61420640e34ea0bf68

if [[ -f $model ]] && printf '%s  %s\n' "$hash" "$model" | sha256sum --check --status; then
    exit 0
fi

mkdir -p "$(dirname "$model")"
tmp=$(mktemp "$model.XXXXXX")
trap 'rm -f "$tmp"' EXIT
curl --fail --location --retry 3 https://raw.githubusercontent.com/sml2h3/ddddocr/master/ddddocr/common_old.onnx -o "$tmp"
printf '%s  %s\n' "$hash" "$tmp" | sha256sum --check
mv "$tmp" "$model"
printf 'OCR model ready: %s\n' "$model"
