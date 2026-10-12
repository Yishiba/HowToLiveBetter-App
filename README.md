# 人生指南 App（Android + iOS）

把开源书[《高性价比人生指南》](https://github.com/eternity4719/HowToLiveBetter)（CC BY 4.0）做成原生 App。Kotlin Multiplatform + Compose Multiplatform，解析、筛选、界面三端共享。非官方客户端。

## 结构

| 目录 | 内容 |
|---|---|
| `upstream/` | 上游仓库（git submodule，锁定 commit），内置正文快照的来源 |
| `shared/` | KMP 模块：`corpus/` 解析与拆块、`filter/` 筛选、`data/` 仓库/更新/偏好、`ui/` 全部界面 |
| `androidApp/` | Android 入口 |
| `iosApp/` | Xcode 壳工程，SwiftUI 承载 Compose |
| `tools/golden/` | 用上游 `index.html` 自己的 JS 导出标准答案，供对拍 |

正文怎么进 App：`:shared:syncCorpus` 把 `upstream/` 的 README 与 `book/*.md` 打进 compose 资源；启动后每 12 小时最多一次问 GitHub 上游 main 的 commit，比内置的新就按 commit 从 jsDelivr（失败换 fastly、raw.githubusercontent）下载，解析校验通过才切换。

## 解析器为什么可信

`CorpusParser`、`SplitRules`、`Filter` 逐行移植自上游 `index.html` 的 `parseReadme()`、`<split-rules>`、`apply()`/`fitsMe()`。`jvmTest` 拿上游原函数跑出来的 golden 逐字段比对全部条目和 17 组筛选；`diffCatchesChanges` 是比对函数自己的对照组。上游改了解析或拆分规则，测试会红。

## 常用命令

```bash
./gradlew :shared:jvmTest            # 对拍 + 更新器测试（需要 node）
./gradlew :androidApp:assembleDebug  # Android 调试包
```

iOS：用 Xcode 打开 `iosApp/iosApp.xcodeproj` 直接运行。命令行构建：

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild -project iosApp/iosApp.xcodeproj -target iosApp -sdk iphonesimulator -configuration Debug ARCHS=arm64 build
```

判测试成败看 `shared/build/test-results/jvmTest/*.xml` 里的 `failures`/`errors`/`tests`，不只看 BUILD SUCCESSFUL。

## 升级内置正文

```bash
git -C upstream fetch && git -C upstream checkout origin/main
./gradlew :shared:jvmTest
git add upstream && git commit -m "内置正文更新到 $(git -C upstream log -1 --format=%h)"
```

## 上架前待办

- 定应用名、图标、包名（`androidApp/build.gradle.kts` 的 `applicationId`、`iosApp/Configuration.xcconfig`），建议先征得原作者同意
- Android release keystore；iOS 开发者账号与 `TEAM_ID`
- 隐私政策托管页（App 不收集数据，只下载公开正文）
- 国内商店：工信部 APP 备案、软著；正文涉及自杀、毒品、性健康等，年龄分级与审核说明要提前准备
- 上架后按上游规矩在其根目录 `衍生项目.md` 提 PR 登记
