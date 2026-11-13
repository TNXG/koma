#!/usr/bin/env bash
# Fetches the OCR model pinned by checksum. Safe to re-run; shared by setup.sh and CI.
set -euo pipefail
cd "$(dirname "$0")/.."

model=core/src/androidMain/assets/ocr/captcha.onnx
hash=b8f2ad9cbc1f2e3922a6cb9459e30824e7e2467f3fb4fd61420640e34ea0bf68

check_sha256() { # $1 expected hash, $2 file; macOS ships shasum instead of sha256sum
    if command -v sha256sum >/dev/null 2>&1; then
        printf '%s  %s\n' "$1" "$2" | sha256sum --check --status
    else
        printf '%s  %s\n' "$1" "$2" | shasum -a 256 --check --status
    fi
}

if [[ -f $model ]] && check_sha256 "$hash" "$model"; then
    exit 0
fi

mkdir -p "$(dirname "$model")"
tmp=$(mktemp "$model.XXXXXX")
trap 'rm -f "$tmp"' EXIT
curl --fail --location --retry 3 https://raw.githubusercontent.com/sml2h3/ddddocr/master/ddddocr/common_old.onnx -o "$tmp"
if ! check_sha256 "$hash" "$tmp"; then
    printf 'OCR model checksum mismatch: %s\n' "$tmp" >&2
    exit 1
fi
mv "$tmp" "$model"
printf 'OCR model ready: %s\n' "$model"
