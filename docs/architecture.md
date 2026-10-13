# 架构

界面与交互的设计见 [ux.md](ux.md)。

目标：选成熟、稳定的技术；新功能能加成一个独立模块，不用改动已有功能的代码。

## 模块

```
androidApp ─┐                         iosApp（Xcode 壳，SwiftUI 承载 Compose）
            ├─► shared ◄──────────────┘
            │     导航（NavHost）、Koin 组装、内置正文快照、Shared.framework、桌面预览
            │
            ├─► feature/checklist 清单（首页 Tab）
            ├─► feature/find      查找 Tab：搜索、遇到事了、章节目录、章节页
            ├─► feature/entry     条目详情、分享
            ├─► feature/settings  关于与设置、免责声明
            │        │
            │        ▼
            ├─► core/ui      设计系统：配色、图标、徽章、正文渲染、界面文案
            ├─► core/data    选用哪份正文、联网更新、本机偏好、筛选状态
            └─► core/corpus  纯领域逻辑：正文模型、解析、拆块、筛选、行内 Markdown 切分
```

依赖规则（新增代码时请遵守）：

| 模块 | 可以依赖 | 不可以依赖 |
|---|---|---|
| `core/corpus` | 只能是 Kotlin 标准库与 kotlinx.serialization | Compose、IO、网络 |
| `core/data` | `core/corpus` | Compose、任何 feature |
| `core/ui` | `core/corpus`、Compose | `core/data`、任何 feature |
| `feature/*` | 全部 `core/*` | 其他 feature（跳转交给 `shared` 的 NavHost） |
| `shared` | 全部 | — |

每个模块的 Gradle 配置都来自 `build-logic/` 里的约定插件，模块自己的 `build.gradle.kts` 只写与众不同的部分：

| 插件 | 用于 | 内容 |
|---|---|---|
| `lifeguide.kmp.library` | `core/corpus`、`core/data` | 目标平台（Android、iosArm64、iosSimulatorArm64、jvm）、SDK 级别、JVM 17、测试依赖；Android namespace 由模块路径推出 |
| `lifeguide.kmp.compose` | `core/ui`、`shared` | 在上一行基础上加 Compose 编译器、Compose Multiplatform、常用 UI 依赖 |
| `lifeguide.kmp.feature` | `feature/*` | 在上一行基础上加 core 依赖、ViewModel、Navigation、Koin |

## 技术选型与理由

| 关注点 | 选型 | 理由 |
|---|---|---|
| 跨端 | Kotlin Multiplatform + Compose Multiplatform | JetBrains 官方方案，iOS 已稳定；逻辑与界面都只写一份 |
| 构建 | Gradle 版本目录 + `build-logic` 约定插件（Now in Android 的做法） | 版本只在 `gradle/libs.versions.toml` 一处；加模块不复制配置 |
| Android 库插件 | `com.android.kotlin.multiplatform.library` | AGP 9 起 KMP 模块官方推荐的插件，旧的 `com.android.library` 用于 KMP 已弃用 |
| 依赖注入 | Koin | KMP 上最成熟的 DI，纯 Kotlin、无代码生成；`KoinGraphTest` 在 JVM 上把整张依赖图造一遍，缺绑定会直接测红 |
| 状态 | AndroidX ViewModel（KMP 版）+ StateFlow，一个页面一个 ViewModel | 官方组件，生命周期与导航返回栈绑定 |
| 导航 | AndroidX Navigation Compose（KMP 版），类型安全路由 | 官方组件；每个 feature 导出 `NavGraphBuilder.xxxScreen()` 和 `NavController.navigateToXxx()` |
| 网络 | Ktor Client（OkHttp / Darwin / CIO 引擎） | KMP 标准选择；引擎在 `core/data` 用 expect/actual 选定 |
| 存储 | DataStore Preferences（KMP 版）+ Okio | 官方组件；正文快照用 Okio 写文件，先写临时目录再原子改名 |
| 日志 | Kermit | KMP 上常用的日志库，Android 走 Logcat，iOS 走 os_log |
| 版本策略 | 只用正式版（不用 alpha、beta、RC） | 稳定；升级时整批升，跑全部测试与两端构建 |

刻意没用的：

| 没用的 | 原因 |
|---|---|
| Room / SQLDelight | 正文是只读的 Markdown，全书约 2 MB 文本，一次解析进内存，600 多条在内存里检索足够快。收藏等需要查询的功能出现时再引入 SQLDelight |
| Navigation 3 | 还在快速演进 |
| 图标库 | 只用到 8 枚，直接内联路径 |

## 数据流

```
内置快照（compose 资源）─┐
                         ├─► CorpusRepository.state ──► Checklist / Find / Section / Entry / Settings 的 ViewModel
下载快照（CorpusStore）──┘          ▲
                                    │ reload()
ContentUpdateService ──► CorpusUpdater（GitHub API → jsDelivr / fastly / raw 镜像 → 校验 → 原子安装）
        │
        └─ events ──► AppViewModel ──► Snackbar

FilterRepository（查找页的搜索词与筛选，持久化到 UserPreferences）──► FindViewModel（防抖后检索）、EntryViewModel（高亮）
ChecklistRepository（核心条目 + 我加的 + 勾选，按「我的情况」过滤）──► ChecklistViewModel、EntryViewModel（底部按钮）
ContentUpdateService 更新成功后记下新增和改动的条目（RecentChanges）──► 清单顶部提示、各列表的小圆点
```

进程级单例都在 `core/data/.../di/DataModule.kt`。不该随页面结束而取消的活（下载更新、写偏好）跑在 `AppScope` 里。

## 新增一个功能（以「收藏」为例）

1. 新建 `feature/favorites/build.gradle.kts`，内容只有 `plugins { alias(libs.plugins.lifeguide.kmp.feature) }`，在 `settings.gradle.kts` 里 `include(":feature:favorites")`。
2. 需要持久化的状态放进 `core/data`，例如新建 `FavoritesRepository`，在 `dataModule` 里登记，并在 `core/data` 的 jvmTest 里写测试。
3. 在 feature 模块里写 `FavoritesViewModel`、界面，以及 `FavoritesNavigation.kt`（路由、`favoritesScreen()`、`navigateToFavorites()`、`favoritesModule`）。
4. 在 `shared`：给 `build.gradle.kts` 加依赖；在 `di/Koin.kt` 的 `appModules` 里加 `favoritesModule`；在 `App.kt` 的 NavHost 里加 `favoritesScreen(...)`。
5. 在 `KoinGraphTest.everyDefinitionResolves` 里加一行 `koin.get<FavoritesViewModel>()`。

已有功能的代码不用动。

## 测试

| 测试 | 位置 | 守住什么 |
|---|---|---|
| `GoldenTest` | `core/corpus` | 解析、拆块、筛选与上游网页 JS 逐字段一致（687 条，17 组筛选），自带对照组 |
| `InlineMdTest` | `core/corpus` | 行内 Markdown 切分 |
| `CorpusUpdaterTest` | `core/data` | 镜像回退、截断下载、全部镜像挂掉、远端更旧 |
| `CorpusRepositoryTest` | `core/data` | 内置版与下载版之间的选择、旧快照清理、并发只加载一次 |
| `UpstreamAcceptanceTest` | `core/data` | 这份正文能通过 App 联网更新时的校验（上游格式一变，两边解析会一起错，对拍照样通过，只有这条能抓到） |
| `GuideTest` | `core/corpus` | 产品规则在真实正文上成立：核心条目的定义、标题不重复（清单按标题记勾）、每节都归入了场景、「遇到事了」指向存在的节、更新差异不把改条号当改动 |
| `FilterTest` | `core/corpus` | 清单的人群过滤：没填情况时带标签的条目不进清单 |
| `ChecklistTest` | `core/data` | 清单由核心条目和「我加的」组成，勾选按标题记 |
| `KoinGraphTest` | `shared` | 依赖图完整；内置正文确实打进了资源 |

判断成败看 `tools/ci/check-test-results.sh`（读 JUnit XML，要求用例数大于 0、失败数为 0），不只看 Gradle 的退出码。CI 配置在 `.github/workflows/`：

| 工作流 | 何时跑 | 做什么 |
|---|---|---|
| `ci.yml` | 推送到 main、提 PR | 全部单元测试、Android 调试包、iOS 模拟器构建 |
| `upstream-compat.yml` | 每天北京时间 09:00，也可手动触发 | 把 `upstream/` 换成上游最新的 main，跑 `core/corpus`、`core/data` 的测试；失败时开一个带 `upstream-compat` 标签的 issue，已有未关闭的就追加评论 |

夜间任务报警后：先看是上游改了格式，还是改了解析规则（`index.html`），照着改 Kotlin 端，测试通过后按 README 的步骤升级内置正文。
