#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""校验 GITHUB_PAT 是否仍然有效（沙箱内 git push 会被杀，只能靠 REST 判定权限）。"""
import json
import os
import subprocess
import urllib.error
import urllib.request

for k in ("HTTP_PROXY", "HTTPS_PROXY", "http_proxy", "https_proxy", "ALL_PROXY", "all_proxy"):
    os.environ.pop(k, None)
os.environ["NO_PROXY"] = "*"

t = subprocess.run(
    ["powershell", "-NoProfile", "-Command",
     "[Environment]::GetEnvironmentVariable('GITHUB_PAT','User')"],
    capture_output=True, text=True
).stdout.strip()

print("PAT 长度:", len(t), " 前缀:", t[:4])
if not t:
    raise SystemExit("GITHUB_PAT 为空")

req = urllib.request.Request("https://api.github.com/user")
req.add_header("Authorization", "Bearer " + t)
req.add_header("User-Agent", "chk")
try:
    with urllib.request.urlopen(req, timeout=30) as r:
        d = json.loads(r.read().decode())
        print("鉴权成功 200 OK, login =", d.get("login"))
except urllib.error.HTTPError as e:
    print("鉴权失败 HTTP", e.code)
    print(e.read().decode()[:300])
