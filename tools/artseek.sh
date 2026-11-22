#!/usr/bin/env bash
# Runs the pinned artseek CLI against the running Koma process on a rooted arm64 device.
# Usage: tools/artseek.sh <ping|roots|tree|strsearch|instances|dumpobj|whorefs|query|selfcheck|calibrate> [args]
# Env: ARTSEEK_SERIAL picks one adb device; ARTSEEK_PKG overrides the package.
set -euo pipefail
cd "$(dirname "$0")/.."
root=$(pwd)

pkg=${ARTSEEK_PKG:-moe.tnxg.koma}

die() {
    printf 'artseek: %s\n' "$*" >&2
    exit 1
}

if [[ -z ${ADB:-} ]]; then
    if [[ -x $root/.tools/android/platform-tools/adb ]]; then
        ADB=$root/.tools/android/platform-tools/adb
    else
        ADB=$(command -v adb || true)
    fi
fi
[[ -n $ADB ]] || die "adb not found; run 'mise run setup' or put adb on PATH"

"$root/tools/fetch-artseek.sh" >&2

if [[ -n ${ARTSEEK_SERIAL:-} ]]; then
    serial=$ARTSEEK_SERIAL
else
    device_list=$("$ADB" devices | awk 'NR > 1 && $2 == "device" { print $1 }')
    device_count=$(grep -c . <<< "$device_list" || true)
    case $device_count in
        0) die "no adb device; connect a rooted arm64-v8a phone and run 'mise run run'" ;;
        1) serial=$device_list ;;
        *) die "multiple devices ($(tr '\n' ' ' <<< "$device_list")); set ARTSEEK_SERIAL to one of them" ;;
    esac
fi

abi=$("$ADB" -s "$serial" shell getprop ro.product.cpu.abi | tr -d '\r')
[[ $abi == arm64-v8a ]] || die "device $serial reports $abi; artseek only ships an arm64-v8a binary"

root_id=$("$ADB" -s "$serial" shell 'su -c id' 2>/dev/null | tr -d '\r' || true)
[[ $root_id == *uid=0* ]] || die "device $serial is not rooted (su -c id failed); artseek reads /proc/<pid>/mem as root"

sub=${1:-}
case $sub in
    ping | calibrate | --help | -h | --version | -v | "") ;;
    *)
        if [[ -z $("$ADB" -s "$serial" shell pidof "$pkg" | tr -d '\r') ]]; then
            die "$pkg is not running; start it with 'mise run run'"
        fi
        ;;
esac

exec node "$root/.tools/artseek/sdk/node_modules/artseek/dist/cli.js" \
    "$@" --pkg "$pkg" --serial "$serial" \
    --binary "$root/.tools/artseek/artseek-android-arm64"
