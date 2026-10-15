#!/usr/bin/env bash
# 一次性准备发版签名：生成密钥、密码存进 macOS 钥匙串、上传 GitHub Secrets、登记证书指纹。
# 密码随机生成，全程不显示、不落盘、不出现在命令行参数里（经管道交给 keytool 和 gh）。
#
# 用法（在你自己的终端里跑，不要让别人或 AI 代跑）：
#   tools/release/setup-signing.sh            # 正式：生成并上传
#   tools/release/setup-signing.sh --dry-run  # 演练：只在临时目录生成，不碰钥匙串和 GitHub
set -euo pipefail

DRY_RUN=0
[ "${1:-}" = "--dry-run" ] && DRY_RUN=1

ALIAS=lifeguide
KEYCHAIN_SERVICE="LifeGuide 发版密钥"
if [ ${DRY_RUN} -eq 1 ]; then
  DIR=$(mktemp -d)
else
  DIR="${HOME}/LifeGuideRelease"
fi
KEYSTORE="${DIR}/lifeguide-release.jks"

say() { printf '%s\n' "$*"; }
die() { printf '✗ %s\n' "$*" >&2; exit 1; }

# 最先检查：已经有密钥就停，覆盖掉旧密钥，已装的用户就再也收不到更新
[ -e "${KEYSTORE}" ] && die "${KEYSTORE} 已经存在。密钥只生成一次，不要覆盖。"

command -v keytool >/dev/null || die "找不到 keytool（装了 JDK 就有）"
command -v openssl >/dev/null || die "找不到 openssl"
if [ ${DRY_RUN} -eq 0 ]; then
  command -v security >/dev/null || die "这个脚本只支持 macOS（要用钥匙串存密码）"
  command -v gh >/dev/null || die "找不到 gh（GitHub CLI）"
  gh auth status >/dev/null 2>&1 || die "gh 还没登录：先运行 gh auth login"
  repo=$(gh repo view --json nameWithOwner --jq .nameWithOwner) || die "当前目录不是 GitHub 仓库"
  say "目标仓库：${repo}"
fi

if [ ${DRY_RUN} -eq 0 ] && security find-generic-password -s "${KEYCHAIN_SERVICE}" >/dev/null 2>&1; then
  die "钥匙串里已经有「${KEYCHAIN_SERVICE}」，说明之前生成过。不要重复生成。"
fi

mkdir -p "${DIR}"
chmod 700 "${DIR}"

# 32 位随机密码，只用字母数字，免得各处转义出问题
PW=$(openssl rand -base64 48 | tr -dc 'A-Za-z0-9' | cut -c1-32)
[ ${#PW} -eq 32 ] || die "生成密码失败"

# keytool 用 :env 从环境变量读密码，不进命令行参数（ps 看不到）
LG_PW="${PW}" keytool -genkeypair -keystore "${KEYSTORE}" -storetype PKCS12 \
  -alias "${ALIAS}" -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass:env LG_PW -dname "CN=LifeGuide Android" >/dev/null 2>&1 || die "keytool 生成密钥失败"
chmod 600 "${KEYSTORE}"

FP=$(LG_PW="${PW}" keytool -list -v -keystore "${KEYSTORE}" -alias "${ALIAS}" -storepass:env LG_PW 2>/dev/null \
  | grep -m1 'SHA256:' | awk '{print $2}' | tr -d ':' | tr 'A-F' 'a-f')
[ ${#FP} -eq 64 ] || die "读不出证书指纹"

if [ ${DRY_RUN} -eq 1 ]; then
  say "✓ 演练通过：密钥生成在 ${KEYSTORE}，证书指纹 ${FP:0:16}…"
  say "  （演练没碰钥匙串和 GitHub，临时目录可以删掉）"
  exit 0
fi

# 密码进钥匙串（-w 后面紧跟的值不会回显；之后用 security find-generic-password -s "${KEYCHAIN_SERVICE}" -w 取）
security add-generic-password -a "${ALIAS}" -s "${KEYCHAIN_SERVICE}" -w "${PW}" -U

# 上传 GitHub：值都从标准输入给 gh，不出现在命令行参数里
base64 -i "${KEYSTORE}" | gh secret set LIFEGUIDE_KEYSTORE_BASE64
printf '%s' "${PW}" | gh secret set LIFEGUIDE_KEYSTORE_PASSWORD
printf '%s' "${PW}" | gh secret set LIFEGUIDE_KEY_PASSWORD   # PKCS12 的两个密码相同
printf '%s' "${ALIAS}" | gh secret set LIFEGUIDE_KEY_ALIAS
gh variable set RELEASE_CERT_SHA256 --body "${FP}"
unset PW

say ""
say "✓ 完成。"
say "  密钥文件：${KEYSTORE}"
say "  密码：在钥匙串里，名字是「${KEYCHAIN_SERVICE}」"
say "  证书指纹：${FP}（已登记成仓库变量 RELEASE_CERT_SHA256）"
say ""
say "现在就做的一件事：备份。把密钥文件拷到 U 盘或别的离线位置，密码另存进密码管理器。"
say "密钥一旦丢了，已经装上 App 的人就再也收不到更新。"
