#!/usr/bin/env bash
# Installs the Android SDK and the verified OCR model into .tools/.
# Runs on Linux and macOS; Windows uses tools/setup.ps1 (mise run setup picks the right one).
set -euo pipefail
cd "$(dirname "$0")/.."
root=$(pwd)

# sdkmanager and the Android Gradle Plugin keep caches under $ANDROID_USER_HOME.
# Keeping that inside the repository stops them from breaking when the home
# directory contains non-ASCII characters.
export ANDROID_HOME=${ANDROID_HOME:-$root/.tools/android}
export ANDROID_SDK_ROOT=$ANDROID_HOME
export ANDROID_USER_HOME=${ANDROID_USER_HOME:-$root/.tools/android-user}

check_sha256() { # $1 expected hash, $2 file
    if command -v sha256sum >/dev/null 2>&1; then
        printf '%s  %s\n' "$1" "$2" | sha256sum --check --status
    else
        printf '%s  %s\n' "$1" "$2" | shasum -a 256 --check --status
    fi
}

case "$(uname -s)" in
    Darwin) cli_zip=commandlinetools-mac-13114758_latest.zip ;;
    Linux) cli_zip=commandlinetools-linux-13114758_latest.zip ;;
    *)
        printf 'Unsupported platform: %s (on Windows run tools/setup.ps1)\n' "$(uname -s)" >&2
        exit 1
        ;;
esac

mkdir -p "$ANDROID_HOME" "$ANDROID_USER_HOME" core/src/androidMain/assets/ocr
sdkmanager=$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager
if [[ ! -f $sdkmanager ]]; then
    tmp=$(mktemp -d)
    trap 'rm -rf "$tmp"' EXIT
    curl --fail --location --retry 2 "https://dl.google.com/android/repository/$cli_zip" -o "$tmp/commandline.zip"
    (cd "$tmp" && jar xf commandline.zip)
    rm -rf "$ANDROID_HOME/cmdline-tools/latest"
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    mv "$tmp/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
fi
chmod +x "$ANDROID_HOME"/cmdline-tools/latest/bin/*

set +o pipefail
yes 2>/dev/null | "$sdkmanager" --licenses >/dev/null || true
set -o pipefail

# build-tools 36.0.0 is what the Android Gradle Plugin resolves by default here;
# 37.0.0 matches compileSdk. Installing both keeps `gradle` offline-clean.
"$sdkmanager" 'platform-tools' 'platforms;android-37.0' 'build-tools;37.0.0' 'build-tools;36.0.0'

model=core/src/androidMain/assets/ocr/captcha.onnx
hash=b8f2ad9cbc1f2e3922a6cb9459e30824e7e2467f3fb4fd61420640e34ea0bf68
if [[ ! -f $model ]] || ! check_sha256 "$hash" "$model"; then
    curl --fail --location --retry 2 https://raw.githubusercontent.com/sml2h3/ddddocr/master/ddddocr/common_old.onnx -o "$model.tmp"
    if ! check_sha256 "$hash" "$model.tmp"; then
        rm -f "$model.tmp"
        printf 'OCR model checksum mismatch.\n' >&2
        exit 1
    fi
    mv "$model.tmp" "$model"
fi
printf 'Android SDK and verified OCR model ready.\n'
