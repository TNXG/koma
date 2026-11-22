#!/usr/bin/env bash
# Fetches the pinned artseek SDK (npm) and the arm64-v8a native binary into .tools/artseek/.
# The binary is pinned by checksum because it runs on the device as root.
# Safe to re-run; the wrapper calls this lazily on first use.
set -euo pipefail
cd "$(dirname "$0")/.."

sdk_version=0.1.0
native_version=0.1.1
native_hash=f5681e611500a1888c04839b33cd9739fb1ddae32b49270f00918cad17459fd9
native_url=https://github.com/std-microblock/artseek/releases/download/v${native_version}/artseek-android-arm64
native=.tools/artseek/artseek-android-arm64
sdk_root=.tools/artseek/sdk
cli=$sdk_root/node_modules/artseek/dist/cli.js

check_sha256() { # $1 expected hash, $2 file; macOS ships shasum instead of sha256sum
    if command -v sha256sum >/dev/null 2>&1; then
        printf '%s  %s\n' "$1" "$2" | sha256sum --check --status
    else
        printf '%s  %s\n' "$1" "$2" | shasum -a 256 --check --status
    fi
}

mkdir -p .tools/artseek

if [[ ! -f $native ]] || ! check_sha256 "$native_hash" "$native"; then
    tmp=$(mktemp "$native.XXXXXX")
    trap 'rm -f "$tmp"' EXIT
    curl --fail --silent --show-error --location --retry 3 "$native_url" -o "$tmp"
    if ! check_sha256 "$native_hash" "$tmp"; then
        printf 'artseek binary checksum mismatch: %s\n' "$tmp" >&2
        exit 1
    fi
    chmod +x "$tmp"
    mv "$tmp" "$native"
    printf 'artseek native binary ready: %s\n' "$native"
fi

# npm 0.1.0 is the latest published SDK; its native binary lacks the merged
# Class.fields_ layout fix, so the wrapper always passes the v0.1.1 binary above.
if [[ ! -f $cli ]]; then
    npm install --prefix "$sdk_root" "artseek@$sdk_version" \
        --no-audit --no-fund --ignore-scripts
fi
