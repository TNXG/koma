#!/usr/bin/env bash
# Full verification: structural rules, behavioral tests, and the debug build.
# Fast structural rules alone are `tools/check-structure.sh` (what the commit hook runs).
set -euo pipefail
cd "$(dirname "$0")/.."
tools/check-structure.sh
gradle :core:testAndroidHostTest :ui:compose:testAndroidHostTest :app:assembleDebug
