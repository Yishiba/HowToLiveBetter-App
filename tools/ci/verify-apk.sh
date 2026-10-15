#!/usr/bin/env bash
# 发版前验签：APK 必须签了名，而且签名证书的 SHA-256 指纹必须是登记过的那一个。
# 换了密钥签出来的包，装不上用户手机里的旧版（Android 拒绝不同签名的更新），所以宁可在 CI 里拦下。
# 用法：tools/ci/verify-apk.sh <apk> <期望的证书 SHA-256，64 位十六进制，大小写和冒号都可以>
set -euo pipefail
# 任何没被下面的检查接住的退出都报出行号，不许静默失败（v0.1.0 第一次发版就是在这里无声退出的）
trap 'echo "✗ 验签脚本在第 ${LINENO} 行意外退出" >&2' ERR
apk="$1"
want=$(tr -d ':[:space:]' <<<"${2:-}" | tr 'A-F' 'a-f')
if [ ${#want} -ne 64 ]; then
  echo "✗ 没有登记签名证书指纹（仓库变量 RELEASE_CERT_SHA256），不发版。见 docs/release.md"; exit 1
fi
sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
# 管道里的 ls 找不到时会让整条赋值失败，在 set -e 下无声退出，所以先单独列出来再判
signers=$(ls -d "${sdk}"/build-tools/*/apksigner 2>/dev/null || true)
if [ -z "${signers}" ]; then
  echo "✗ 在 ${sdk}/build-tools 下找不到 apksigner，那里有：$(ls "${sdk}/build-tools" 2>&1 | tr '\n' ' ')"; exit 1
fi
apksigner=$(sort -V <<<"${signers}" | tail -1)
echo "用 ${apksigner}"
if ! out=$("$apksigner" verify --print-certs "$apk" 2>&1); then
  echo "✗ ${apk} 没签名或签名无效："; echo "$out"; exit 1
fi
got=$( (grep -m1 'Signer #1 certificate SHA-256 digest:' <<<"${out}" || true) | awk '{print $NF}')
if [ -z "${got}" ]; then
  echo "✗ apksigner 的输出里没找到证书指纹，原样输出如下："; echo "${out}"; exit 1
fi
if [ "$got" != "$want" ]; then
  echo "✗ 签名证书不对：得到 ${got}，登记的是 ${want}"; exit 1
fi
echo "✓ ${apk} 已签名，证书指纹与登记的一致（${got:0:16}…）"
