#!/usr/bin/env python3
"""准备 CI 用的 harmonyApp/build-profile.json5。

这个文件是 machine-local 的（DevEco 写入签名材料绝对路径，并钉上本机 SDK 版本），
CI 从 secret 还原后需要两步调整：
- certpath/profile/storeFile 按文件名重指向解压出的签名材料目录，其余字段原样保留；
- 删掉 compileSdkVersion/targetSdkVersion：DevEco 26 写的是新的裸版本号 "26.0.0"，
  CI 固定的 Command Line Tools 自带 hvigor 只认 "X.Y.Z(NN)"；留空后 hvigor
  用工具链自带的 SDK 推导这两个值。
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
            # profile 由开发机导出，路径是 Windows 反斜杠形式；Linux 上 os.path.basename 不切分它们。
            name = value.replace("\\", "/").rsplit("/", 1)[-1]
            target = os.path.join(material_dir, name)
            if not os.path.isfile(target):
                sys.exit(f"missing signing material: {target}")
            material[key] = target

    for product in data["app"].get("products", []):
        product.pop("compileSdkVersion", None)
        product.pop("targetSdkVersion", None)

    open(profile, "w", encoding="utf-8").write(json.dumps(data, indent=2, ensure_ascii=False))
    print(f"prepared {profile}: signing material paths -> {material_dir}, SDK version fields dropped")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
