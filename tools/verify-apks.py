#!/usr/bin/env python3
"""Checks every release APK carries exactly its ABI's native library plus the OCR model."""
import glob
import re
import sys
import zipfile

ABIS = ("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
SPLIT = re.compile(r"-([a-z0-9_]+(?:-[a-z0-9]+)?)\.apk$")
NATIVE = re.compile(r"lib/([^/]+)/libonnxruntime\.so")
MODEL = "assets/ocr/captcha.onnx"


def wanted(abi: str) -> set[str]:
    return set(ABIS) if abi == "universal" else {abi}


def main(argv: list[str]) -> int:
    apks = argv[1:] or sorted(glob.glob("app/build/outputs/apk/release/koma-*.apk"))
    if not apks:
        print("no release APKs found", file=sys.stderr)
        return 1
    seen: set[str] = set()
    failed = False
    for apk in apks:
        match = SPLIT.search(apk)
        if match is None:
            print(f"unexpected APK name: {apk}", file=sys.stderr)
            failed = True
            continue
        abi = match.group(1)
        seen.add(abi)
        with zipfile.ZipFile(apk) as archive:
            names = archive.namelist()
        libs = {m.group(1) for name in names if (m := NATIVE.fullmatch(name))}
        expect = wanted(abi)
        if libs != expect:
            print(f"{apk}: native libs {sorted(libs)} != {sorted(expect)}", file=sys.stderr)
            failed = True
        if MODEL not in names:
            print(f"{apk}: missing {MODEL}", file=sys.stderr)
            failed = True
        print(f"ok {apk} ({abi}, {len(names)} entries)")
    missing = {"universal", *ABIS} - seen
    if missing:
        print(f"missing splits: {sorted(missing)}", file=sys.stderr)
        failed = True
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
