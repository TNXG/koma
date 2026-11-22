# AGENTS.md — AI 协作指南

面向在本仓库工作的 AI Agent。人类开发者先读 [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)，本文不重复它的细节，只补 Agent 视角的事实、约束和踩坑记录。两者冲突时以 DEVELOPMENT.md 为准。

Koma 是大学课表应用：Kotlin Multiplatform + Compose Multiplatform，当前只构建 Android Application；`harmonySpike/` 是独立的鸿蒙实验工程。包名 `moe.tnxg.koma`，AGPL-3.0。

## 从 0 配置环境

所有工具链由 [mise](https://mise.jdx.dev) 固定（JDK 21、Gradle 9.9.1、Node 24），不要用机器上的全局 Gradle/Java：

```sh
mise trust          # 首次信任 mise.toml
mise install        # 装固定版本工具链
mise run setup      # 装 Android SDK 到 .tools/android/ + 拉取校验 OCR 模型
```

- Android SDK 装在被忽略的 `.tools/android/`，SDK 环境变量由 mise 注入，不需要手动配 `ANDROID_HOME`。
- SDK 用户缓存放 `.tools/android-user/`（`ANDROID_USER_HOME`），不放 `$HOME`：用户目录含非 ASCII 字符时 sdkmanager 会抛 `NoSuchFileException`。
- `mise run setup` 同时装 Build Tools 36.0.0 和 37.0.0（AGP 9 解析 36.0.0，`compileSdk` 对齐 37.0.0），缺一个都无法离线构建。
- Windows 走同一套命令，mise 会分派到 `tools/*.ps1`（系统 PowerShell 5.1）。

构建与验证命令：

| 命令 | 作用 |
| --- | --- |
| `mise run test` | Kotlin 行为测试（androidHostTest） |
| `mise run build` | 出 debug APK：`app/build/outputs/apk/debug/` |
| `mise run check` | 结构规则 + 测试 + debug 包，**改完代码的最小验证** |
| `mise run run` | 装到连接的真机/模拟器并启动 |
| `mise run install-release` | 签名 release universal 包并装机 |
| `mise run build-harmony` | 鸿蒙 HAP（需先 `mise run setup-harmony`） |
| `mise run inspect` | 用 artseek 检查运行中 debug 包的 ART 堆（root 真机） |

**验证纪律**：改动必须跑 `mise run check`；涉及 UI 的改动装真机确认后再报完成，不许只凭编译通过下结论。日志验证可用 `adb logcat`；堆上状态用 `tools/artseek.sh`（不能进 Gradle/CI，只是真机检查工具）。

## tools/ 工具速查

按遇到的问题选工具，不要上来就截屏点坐标：

| 你要做什么 | 用什么 | 说明 |
| --- | --- | --- |
| 看设备/应用状态 | `adb devices`、`adb shell pidof moe.tnxg.koma` | 最轻量的探活，永远先做这个 |
| 看运行日志 | `adb logcat` | 排查崩溃、异常、登录报错的第一手证据 |
| 截屏 | `adb exec-out screencap -p > shot.png` | 确认视觉结果（布局、模糊、白边）；**不要用截屏猜坐标再点击**，交互状态见下 |
| 验证 UI 层级/组件状态 | `bash tools/artseek.sh roots` | Activity 与 ViewRootImpl 实例、窗口边界、焦点状态 |
| 看视图树 | `bash tools/artseek.sh tree` | Android View 层级；Compose 内容只显示为 AndroidComposeView 节点 |
| 找 Compose 文本 | `bash tools/artseek.sh strsearch <文本>` | 搜索存活 String 实例及其引用者，Compose 文本节点靠它 |
| 查某个类/对象的堆状态 | `bash tools/artseek.sh instances <类名>`、`dumpobj <地址>`、`whorefs <地址>` | 验证状态持有、内存泄漏、谁引用了这个对象 |
| 复杂堆查询 | `bash tools/artseek.sh query '<JSON>'` | QuerySpec 语法见 artseek 仓库 protocol.md |
| 校验分包正确性 | `python3 tools/verify-apks.py` | 每个包只含自己 ABI 的 so 且带 OCR 模型 |
| 拉取/校验 OCR 模型 | `tools/fetch-ocr-model.sh` | 幂等，setup 和 CI 共用 |

`tools/artseek.sh` 的前提：root 的 arm64 真机、`mise run run` 启动的 debug 包；多设备时用 `ARTSEEK_SERIAL` 指定。子命令 `ping` 检查可用性，`selfcheck` 校验 ART 布局，ART 升级后用 `calibrate` 重校准。也可直接用同名 MCP 工具（`artseek_*`），语义一致。

### UI 交互调试的正确姿势

需要验证「点击/输入后的行为」时：

1. 先用 `artseek.sh roots` / `tree` / `strsearch` 确认目标控件在不在层级里、状态对不对——多数「点了没反应」是状态问题，不是坐标问题。
2. 真要模拟交互时用 `adb shell input`（`input tap`、`input swipe`、`input keyevent`），坐标从 `roots` 的窗口边界和已知的布局常量（如 Dock 三项各 76dp、日期岛把手 26dp）推算，而不是肉眼从截图里量。
3. Compose 界面没有 View id，`uiautomator dump` 拿不到内部结构；要么 artseek 看状态，要么在代码里加临时日志后看 logcat，验证完删掉。

## 代码结构约束（硬规则）

`tools/check.sh` 强制，提交前必过：

- `core/src`、`ui/compose/src`、`app/src` 下每个 `.kt` 文件 **≤ 350 行**（物理行数），文件名 **不含连字符**。
- 规则在 `tools/check-structure.sh` 与 `tools/check.ps1` 各有一份实现，改规则时两处同步。
- 依赖方向固定：`app` → `ui:compose` → `core`；KMP 模块用 `com.android.kotlin.multiplatform.library` 插件，不在同一模块混用 Android Application 与 KMP 插件。
- 版本号唯一来源是 `gradle.properties` 的 `koma.version`，`versionName`、APK 文件名、关于页共用，不要另设常量。

## UI：什么组件用什么风格

### 视觉基线（总原则）

UI 是纯 Foundation 自绘基线：**不用 Material、Miuix 等库的默认视觉**。玻璃、模糊、票券轮廓等效果一律 Foundation 自绘或共享组件实现。

### 主题与颜色

- 唯一入口：`KomaTheme.colors`（`ui/theme/KomaColors.kt`）。语义 token：`bg/surface/surface2`、`line/line2`、`ink~ink5`（五级文字层级）、`accent/accent2/accentSoft`、`danger`、`scrim`、`tone*`（七种课程配色）。
- 不要在组件里写死十六进制色值；新颜色先加进 `KomaColors` 亮暗两套再引用。
- 强调色在 `KomaTheme` 里按 `LocalAppPreferences` 派生（5 种强调色），`accentSoft` 随亮暗模式自动插值，组件不要自己算浅色变体。
- 图标统一 MingCute：路径存 `ui/component/icon/KomaGlyph.kt`，经 `KomaIcon.kt` 转 `ImageVector`。新增图标照此模式；不要引入图标库依赖。选图标要贴语义（历史教训：心理课配了肌肉图标被用户指出）。
- 字体只有 Inter（400/500/600/700/800，`KomaFont.kt` 从 composeResources 加载），正文文字用 `KomaText`，不要引系统默认字体。

### 布局与结构

| 组件 | 位置 | 风格要求 |
| --- | --- | --- |
| 文字 | `ui/component/text/KomaText.kt` | 全部走它；字号随设置的 4 档界面字号缩放 |
| 按钮 | `ui/component/button/KomaButton.kt` | 复用既有按钮，不重写 |
| 弹层 | `ui/component/sheet/KomaSheet.kt`、`SheetGlass.kt` | 贴底弹层带玻璃模糊；危险操作（删除）先选范围再二次确认 |
| 页面转场 | `ui/component/page/PageTransition.kt`、`SubpageScaffold.kt` | 220ms `CubicBezierEasing(.22, .9, .24, 1)`；子页按层级轻移+淡入淡出 |
| 边缘羽化 | `ui/component/veil/EdgeVeil.kt` | 渐变模糊遮罩统一走 EdgeVeil，不各页自绘 |
| 加载 | `ui/component/loader/KomaLoader.kt` | 统一加载态 |

### 交互惯例

- **页面容器**：今天/课表/我的共用一个横向 Pager，Dock（玻璃底栏）只创建一次；未导入课表的空状态也走同一容器。离场页面不再接收返回与边缘手势。
- **转场受「减少动态效果」控制**：`reduceMotion` 开启时直接切换（时长 0），系统关闭动画时遵循 Compose 时长缩放。新增任何动画都要接这个开关。
- **模糊分层**：玻璃效果采样的是下层实际画面（Dock 采样 Pager、抽屉采样周课表），镜片本身不再模糊否则文字糊掉；模糊后高对比文字会变白斑，面层要压暗。不支持运行时着色器时退回纯色底板。
- **安全区**：边到边滚动，只给标题和底部操作区应用系统安全区，不在页面根部裁切状态栏；顶层页面独立应用安全区避免转场跳动。
- **课程票券**：同一闭合轮廓画填充、裁切和进行中高亮，16dp 圆角 + 6dp 两侧缺口，不用背景色圆形遮盖边框。
- **ProfileAvatar** 禁用入场/循环动画，首帧完整显示；`KodamaAvatar` 可以动。
- **日期岛**：贴右缘悬浮，收起只露 26dp 把手，拖拽展开，松手看速度再看过半，6 秒无操作收起。
- **周课表**：全部节次适配屏幕高度不纵向滚动；未配置时间的节次不补造钟点。

## 设计要求（来自历史会话的反复要求）

这些是用户在几十轮真实迭代里反复强调的，违反会被打回：

1. **人体工程学优先**。触点大小、上下边距、按钮位置按手感排，不要堆砌元素。「点击文本域还不一定能点进去」「你的按钮上下边距呢」都是真实返工案例。
2. **一个设置只一个入口**。设置项不许在多个页面重复出现；二级设置页必须支持手势返回。
3. **文案面向用户，不暴露技术词汇**。不给用户看 version/v1/UTF-8/1MB 限制这类内部概念；AI 导入指引要教用户怎么用 AI 软件拿课表，不点名单一厂商。除既定 slogan 外，每处文案都要审视是否多余、突兀。
4. **本地优先 + 真实数据**。手动修改（备注、调课、停课）优先于教务同步；冲突弹层逐条选择，不静默覆盖。不把测试课表或假登录当生产回退；空课表显示真实空状态，不伪造数据。
5. **隐式状态不展示**。用户不需要知道的信息（内部解析结构、缓存策略）不进 UI；头像登录后缓存到注销为止，形状只用圆形。
6. **新 UI 先看参考**。用户提供过 `../timetable/`、`../mikcb/` 两个参考项目；做新页面/新交互前先研究它们对应组件的布局和交互逻辑再动手，别自创结构。
7. **动画不是装饰**。页面切换、Drawer 展开、日期滚动都要跟手、丝滑；卡顿要修根因（如主线程同步解码导致帧时钟停摆），不许靠关动画或删模糊回避。图片提前加载避免进入页面时抖动。
8. **真机验证**。构建后装到连接设备实测关键路径（登录、导入、手势返回、模糊效果），白边、层级重叠这类问题截图里才看得见。

## 历史踩坑记录

排障先查这里，都是花过大力气才定位的问题：

- **ONNX 遥测**：onnxruntime Android 默认开微软 1DS 遥测。`KomaApplication.onCreate` 设 `ORT_DISABLE_TELEMETRY=1`，`OcrEngine.setTelemetry(false)` 兜底，manifest 合并时移除 `TelemetryInitializer` 和它带入的 `ACCESS_NETWORK_STATE`。升级 onnxruntime 后检查这三处还在。
- **鸿蒙 aki 依赖**：`@ohos/onnxruntime@1.0.0` 必须用 `overrides` 固定 `@ohos/aki@1.2.26`。`^1.2.14` 解析到 1.3.3 会因 `Persistent` 结构 8→24 字节覆盖调用方栈导致登录闪退；退回 1.2.14 又缺符号。升级这两项必须成对核对原生 ABI 和符号，不能只看 semver。
- **鸿蒙 CLT 下载**：华为云镜像只到 5.1.0，6.x 只在官网下载中心（SPA 页面，命令行拿不到稳定 URL）；注意别下成 arm64 包。
- **CAS 登录边界**：不带 `service` 的探测与提交不跟随默认门户跳转（会造成 CAS/门户循环）；验证码错误可能以 HTTP 401 携带表单返回，须识别 `FailedAuthcodeException` 并复用表单 `execution`。自动登录最多 3 轮换验证码，连续失败转手动输入。
- **schoolId 一致性**：`TimetableSnapshot.schoolId` 和 `Course.source["pluginId"]` 必须同串；`TimetableSession` 靠它判断换校/换学期，不一致会清掉开学日期。
- **接入新学校**：完整 12 步流程见 DEVELOPMENT.md「添加其他学校的教务系统」，核心是不写 `if (学校 == ...)`，靠 `PlatformServices.edu` 装配切换。
- **不要用 git worktree**：用户明确要求直接在主工作区改代码，不建 worktree（历史会话里多次强调）。
- **不要过度测试**：按验证纪律跑 `mise run check` 和覆盖改动路径的最小验证即可，不为小改动铺大套测试。
- **凭据安全**：调试时不要把用户学号、密码打进日志或回显到终端。
- **提交**：用 eval 的 `commit` 命名空间（只提交本会话改动），不手工 `git add -A`；提交前跑 `tools/check-structure.sh`（可用 `git config core.hooksPath .githooks` 启用钩子）。

## 发布

- ABI splits：`assembleRelease` 产出 arm64-v8a / armeabi-v7a / x86 / x86_64 / universal；`tools/verify-apks.py` 校验每个包只含自己的 ABI 且带 OCR 模型。
- 签名靠环境变量 `KOMA_KEYSTORE` / `KOMA_KEYSTORE_PASSWORD` / `KOMA_KEY_PASSWORD`，别名默认 `app_sign`，缺失时构建直接失败。
- CI 在 GitHub（`.github/workflows/release.yml`），推送 `v*` 标签触发；仓库 Secrets 需要 `KOMA_KEYSTORE_BASE64` + 两个密码。
- 双仓库同步：GitHub (TNXG/koma) 与 GitCode (gxdev_lab/koma)，推送时两边都要推。
