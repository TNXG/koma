#!/usr/bin/env bash
# Fast structural checks for Kotlin sources: file length and component file names.
#
# Scope is deliberate: no Gradle, no tests, no formatting. Those live in
# `mise run check` (and CI), because a gate that costs minutes gets skipped with
# `--no-verify` until nobody runs it at all.
#
# Usage:
#   tools/check-structure.sh             # every tracked Kotlin source
#   tools/check-structure.sh a.kt b.kt   # only these files (pre-commit passes staged paths)
set -euo pipefail
cd "$(dirname "$0")/.."

limit=350
failed=0

check_file() {
    local file=$1
    [[ -f $file ]] || return 0
    local lines
    lines=$(wc -l < "$file")
    if (( lines > limit )); then
        printf 'Over %s lines: %s (%s)\n' "$limit" "$file" "$lines" >&2
        failed=1
    fi
    if [[ ${file##*/} == *-* ]]; then
        printf 'Hyphenated component filename: %s\n' "$file" >&2
        failed=1
    fi
}

if (( $# > 0 )); then
    for file in "$@"; do
        check_file "$file"
    done
else
    while IFS= read -r file; do
        check_file "$file"
    done < <(find core/src ui/compose/src app/src -type f -name '*.kt')
fi

(( failed == 0 ))
