#!/usr/bin/env bash
# 判测试成败看 JUnit XML，不只看 Gradle 的退出码：
# 确认每个应该跑测试的模块都真的跑了用例（tests>0），且 failures、errors 都是 0。
# 用法：tools/ci/check-test-results.sh <模块目录>...   例：tools/ci/check-test-results.sh core/corpus core/data shared
set -euo pipefail
status=0
for module in "$@"; do
  dir="$module/build/test-results/jvmTest"
  shopt -s nullglob
  files=("$dir"/TEST-*.xml)
  if [ ${#files[@]} -eq 0 ]; then
    echo "✗ ${module}：没有测试结果（${dir}），测试没跑或被过滤掉了"; status=1; continue
  fi
  tests=0; bad=0
  for f in "${files[@]}"; do
    line=$(grep -m1 -oE '<testsuite [^>]*>' "$f")
    t=$(sed -E 's/.* tests="([0-9]+)".*/\1/' <<<"$line")
    fl=$(sed -E 's/.* failures="([0-9]+)".*/\1/' <<<"$line")
    er=$(sed -E 's/.* errors="([0-9]+)".*/\1/' <<<"$line")
    tests=$((tests + t)); bad=$((bad + fl + er))
  done
  if [ "$tests" -eq 0 ] || [ "$bad" -ne 0 ]; then
    echo "✗ ${module}：${tests} 个用例，${bad} 个失败或出错"; status=1
  else
    echo "✓ ${module}：${tests} 个用例全部通过"
  fi
done
exit $status
