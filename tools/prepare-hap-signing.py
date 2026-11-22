#!/usr/bin/env python3
"""把 CI 解出来的 HAP 签名材料路径写进 build-profile.json5。

build-profile.json5 本身是 machine-local 文件（含开发机绝对路径，已 gitignore），
CI 从 secret 还原后，certpath/profile/storeFile 仍指向开发机路径。这里按文件名
把它们重指向解压出的签名材料目录，其余字段（keyAlias、口令、signAlg）原样保留。
"""
import json
import os
import re
import sys


def strip_comments(text: str) -> str:
    # json5 行注释与块注释（不处理字符串内的注释——build-profile 里没有）
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return re.sub(r"//[^\n]*", "", text)


def main(profile: str, material_dir: str) -> None:
    raw = open(profile, encoding="utf-8").read()
    data = json.loads(strip_comments(raw))

    for config in data["app"].get("signingConfigs", []):
        material = config["material"]
        for key in ("certpath", "profile", "storeFile"):
            value = material.get(key)
            if not value:
                continue
            target = os.path.join(material_dir, os.path.basename(value))
            if not os.path.isfile(target):
                sys.exit(f"missing signing material: {target}")
            material[key] = target

    open(profile, "w", encoding="utf-8").write(json.dumps(data, indent=2, ensure_ascii=False))
    print(f"rewrote signing material paths to {material_dir}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
