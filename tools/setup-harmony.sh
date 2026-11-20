#!/usr/bin/env bash
# Repairs executable permissions on the unzipped HarmonyOS Command Line Tools
# (26.0.0 linux-x64) and verifies hvigorw/ohpm run.
#
# The official zip stores ELF binaries without +x and represents some tools
# (clang, llvm-strip, ...) as small text files whose content is the real
# target name; this script converts them to symlinks.
set -euo pipefail
cd "$(dirname "$0")/.."
clt=.tools/ohos/command-line-tools

if [[ ! -d $clt ]]; then
    printf 'Missing %s.\n' "$clt" >&2
    printf 'Download commandline-tools-linux-x64-<ver>.zip from the Huawei download\n' >&2
    printf 'center and unzip it into %s (expect %s/command-line-tools/... layout,\n' "$clt-dir" >&2
    printf 'i.e. the zip must be extracted with `jar xf`/`unzip` so that\n' >&2
    printf '%s exists.\n' "$clt" >&2
    exit 1
fi

# ELF magic -> chmod +x (fast, avoids spawning `file` per entry)
python3 - "$clt" <<'EOF'
import os, sys
root = sys.argv[1]
n = 0
for dirpath, dirs, files in os.walk(root):
    for f in files:
        p = os.path.join(dirpath, f)
        try:
            if os.path.islink(p) or not os.access(p, os.R_OK):
                continue
            with open(p, 'rb') as fh:
                if fh.read(4) == b'\x7fELF':
                    st = os.stat(p)
                    os.chmod(p, st.st_mode | 0o111)
                    n += 1
        except OSError:
            pass
print(f'chmod +x on {n} ELF binaries')
EOF

# Text alias files (<500B, single word matching an existing sibling) -> symlink
cd "$clt"
for dir in sdk/default/*/native/llvm/bin sdk/default/hms/native/BiSheng/bin sdk/default/openharmony/toolchains; do
    [[ -d $dir ]] || continue
    for f in "$dir"/*; do
        [[ -f $f && ! -L $f ]] || continue
        size=$(stat -c%s "$f") || continue
        (( size < 500 )) || continue
        target=$(head -c 200 "$f" | tr -d '\n') || continue
        [[ $target =~ ^[a-zA-Z0-9._-]+$ && -e $dir/$target ]] || continue
        [[ $target == "$f" ]] && continue
        ln -sf "$(basename "$target")" "$f"
    done
done
find bin hvigor/bin ohpm/bin tool/node/bin -maxdepth 1 -type f -exec chmod u+x {} + 2>/dev/null || true
find sdk -name "es2abc" -o -name "restool" -o -name "hdc" | xargs -r chmod u+x

# libxml2.so.2: konan and BiSheng ld.lld need it; Arch ships libxml2.so.16 only.
for lib in "$HOME/.konan/dependencies/cpf-llvm-19-x86_64-linux-dev-21/lib" \
           sdk/default/hms/native/BiSheng/lib; do
    [[ -d $lib && ! -e $lib/libxml2.so.2 ]] || continue
    for cand in /usr/lib/libxml2.so.2 /usr/lib/libxml2.so.16 /usr/lib/libxml2.so; do
        [[ -e $cand ]] && { ln -sf "$cand" "$lib/libxml2.so.2"; echo "linked $lib/libxml2.so.2 -> $cand"; break; }
    done
done

# Verify
export DEVECO_NODE_HOME="$PWD/tool/node" PATH="$PWD/bin:$PATH"
printf 'hvigor: '; hvigorw --version | tail -1
printf 'ohpm:   '; ohpm -v | tail -1
printf 'HarmonyOS toolchain ready.\n'
