#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ANDROID_HOME:?Run this script using mise run setup}"
mkdir -p .tools/android core/src/androidMain/assets/ocr
if [[ ! -f .tools/android/cmdline-tools/latest/bin/sdkmanager ]]; then
    mkdir -p .tools/android/bootstrap
    curl --fail --location --retry 2 https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip -o .tools/android/commandline.zip
    (cd .tools/android/bootstrap && jar xf ../commandline.zip)
    mkdir -p .tools/android/cmdline-tools
    mv .tools/android/bootstrap/cmdline-tools .tools/android/cmdline-tools/latest
    rm .tools/android/commandline.zip
    rmdir .tools/android/bootstrap
fi
chmod +x .tools/android/cmdline-tools/latest/bin/*
set +o pipefail
yes 2>/dev/null | .tools/android/cmdline-tools/latest/bin/sdkmanager --licenses >/dev/null
set -o pipefail
.tools/android/cmdline-tools/latest/bin/sdkmanager 'platform-tools' 'platforms;android-35' 'build-tools;36.0.0'
model=core/src/androidMain/assets/ocr/captcha.onnx
hash=b8f2ad9cbc1f2e3922a6cb9459e30824e7e2467f3fb4fd61420640e34ea0bf68
if [[ ! -f "$model" ]] || ! printf '%s  %s\n' "$hash" "$model" | sha256sum --check --status; then
    curl --fail --location --retry 2 https://raw.githubusercontent.com/sml2h3/ddddocr/master/ddddocr/common_old.onnx -o "$model.tmp"
    printf '%s  %s\n' "$hash" "$model.tmp" | sha256sum --check
    mv "$model.tmp" "$model"
fi
printf 'Android SDK and verified OCR model ready.\n'
