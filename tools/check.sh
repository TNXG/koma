#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
failed=0
while IFS= read -r file; do
    lines=$(wc -l < "$file")
    if (( lines > 350 )); then
        printf 'Over 350 lines: %s (%s)\n' "$file" "$lines" >&2
        failed=1
    fi
    if [[ ${file##*/} == *-* ]]; then
        printf 'Hyphenated component filename: %s\n' "$file" >&2
        failed=1
    fi
done < <(find core/src ui/compose/src app/src -type f -name '*.kt')
(( failed == 0 ))
gradle :core:testAndroidHostTest :ui:compose:testAndroidHostTest :app:assembleDebug
