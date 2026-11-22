#!/usr/bin/env python3
"""剥离 harmonyApp/build-profile.json5 里的 machine-local signingConfigs。

CI 上没有 DevEco 自动签名材料（~/.ohos/config），带着绝对路径构建会直接失败；
剥离后 hvigor 产出未签名 HAP。产物名带 -unsigned，安装需自行签名。
"""
import json
import re
import sys


def strip_comments(text: str) -> str:
    # json5 行注释与块注释（不处理字符串内的注释——build-profile 里没有）
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    text = re.sub(r"//[^\n]*", "", text)
    return text


def main(path: str) -> None:
    raw = open(path, encoding="utf-8").read()
    data = json.loads(strip_comments(raw))
    app = data["app"]

    removed = app.pop("signingConfigs", None)
    if removed is None:
        print("no signingConfigs found; nothing to do")
        return
    # product 级别的 "signingConfig": "default" 引用也一并去掉
    for product in app.get("products", []):
        product.pop("signingConfig", None)

    open(path, "w", encoding="utf-8").write(json.dumps(data, indent=2, ensure_ascii=False))
    print(f"stripped {len(removed)} signing config(s) from {path}")


if __name__ == "__main__":
    main(sys.argv[1])
