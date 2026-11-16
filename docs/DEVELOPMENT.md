# 开发说明

面向修改本项目的人。用户文档见 [README](../README.md)。

源码同时托管在 [GitHub](https://github.com/TNXG/koma) 与 [GitCode](https://gitcode.com/gxdev_lab/koma)，两边内容一致；Issue、PR 与 CI 在 GitHub 上进行。

Kotlin Multiplatform + Compose Multiplatform，当前只构建 Android Application。

## 工程结构

```text
./
├── app/                         Android 宿主：Activity、权限回调、应用配置
│   └── src/main/
├── core/                        KMP：领域、教务协议、平台接口
│   └── src/
│       ├── commonMain/kotlin/moe/tnxg/koma/
│       │   ├── domain/           model、calendar、timeline、week、captcha
│       │   ├── data/education/   协议与正方解析
│       │   └── platform/         存储、凭据、OCR、通知接口
│       ├── androidMain/          CAS HTTP、Cookie、Keystore、ONNX、通知
│       ├── commonTest/
│       ├── androidHostTest/
│       └── androidDeviceTest/
├── ui/compose/                   KMP：共享 Compose 界面与应用状态
│   └── src/
│       ├── commonMain/
│       │   ├── composeResources/font/
│       │   └── kotlin/moe/tnxg/koma/
│       │       ├── app/          session、state、导航入口
│       │       ├── feature/
│       │       │   ├── onboarding/
│       │       │   ├── login/form/
│       │       │   ├── semester/
│       │       │   ├── calendar/
│       │       │   └── home/     header、timeline/course、week、dock、profile
│       │       └── ui/           theme、component/text、button、sheet、loader
│       └── androidMain/          Android UI 边界
├── tools/                       环境初始化与结构检查
├── mise.toml                    固定工具链、SDK 环境、构建命令
└── capacitor/                   原界面与行为的只读迁移基线，不参与新工程构建
```

依赖方向：`app` → `ui:compose` → `core`，`app` 也直接依赖 `core` 来构造 Android 实现。KMP 模块使用 `com.android.kotlin.multiplatform.library` 插件，不把 Android Application 插件与 KMP 插件混在一个模块。当前没有桌面、iOS 或 Web 宿主。

`tools/check.sh` 会检查 `core/src`、`ui/compose/src`、`app/src` 下每个 `.kt` 文件不超过 350 行、文件名不带连字符，然后跑测试和 `:app:assembleDebug`。

## 环境与命令

安装 mise 后，在项目根目录执行：

```sh
mise trust
mise install
mise run setup
mise run test
mise run build
mise run run
```

- JDK、Gradle 由 mise 固定，不依赖机器上的绝对工具路径。
- Android SDK 安装在被忽略的 `.tools/android/`，SDK 环境由 mise 配置。
- `mise run setup` 装 API 37、Build Tools 和 OCR 模型（按 SHA-256 校验）。
- 三个平台同一套命令：`mise run setup`、`mise run check`、`mise run run`、`mise run install-release` 通过 mise 的 `run_windows` 分派到 `tools/*.ps1`，其余平台走 `tools/*.sh`。Windows 用系统自带的 PowerShell 5.1，不依赖 WSL、Git Bash 或额外安装的 `unzip`。
- `tools/setup.sh` 按 `uname -s` 选 `commandlinetools-mac-*` / `commandlinetools-linux-*`；OCR 模型统一由 `tools/fetch-ocr-model.sh` 拉取并校验，哈希检查优先 `sha256sum`，macOS 回退到 `shasum -a 256`。
- 结构规则（350 行上限、文件名不含连字符）在 `tools/check-structure.sh` 与 `tools/check.ps1` 各有一份实现，两者都按物理行数计，改规则时同时改两处。
- SDK 缓存放在 `.tools/android-user/`（`ANDROID_USER_HOME`），不放 `$HOME`：用户目录含非 ASCII 字符时，sdkmanager 写 `~/.android/cache` 会抛 `NoSuchFileException`，AGP 也会读不到缓存。
- `mise run setup` 同时装 Build Tools 36.0.0 和 37.0.0：AGP 9 默认解析 36.0.0，37.0.0 对齐 `compileSdk`，两个都装好才能离线构建。
- `mise run build` 产物：`app/build/outputs/apk/debug/app-debug.apk`。
- `mise run run` 需要已连接的 Android 设备或模拟器。
- 单独检查：`mise run check`。
- 设备测试：`mise exec -- gradle :core:connectedAndroidDeviceTest`。
- 验证码 OCR 使用 ONNX Runtime Android 1.32.0；该版本默认开启微软 1DS 遥测，应用在 `KomaApplication.onCreate` 里先设置 `ORT_DISABLE_TELEMETRY=1`，`OcrEngine` 再用 `setTelemetry(false)` 兜底，并在 manifest 合并时移除 `ai.onnxruntime.TelemetryInitializer` 与它带入的 `ACCESS_NETWORK_STATE`。升级后可单独运行模型设备测试：`mise exec -- gradle :core:connectedAndroidDeviceTest -Pandroid.testInstrumentationRunnerArguments.class=moe.tnxg.koma.ocr.OcrModelInstrumentedTest`，覆盖原图与裁切视图的实际推理。

## 发布与 CI

- **分包**：`app/build.gradle.kts` 开启 ABI splits，`assembleRelease` 一次产出 `koma-<versionName>-arm64-v8a.apk`、`-armeabi-v7a`、`-x86`、`-x86_64` 与 `-universal`。onnxruntime 的原生库按 split 过滤，`assets/ocr/captcha.onnx` 随每个包一起打入；`tools/verify-apks.py` 校验每个包只含自己的 ABI 且带模型。
- **版本号**：`komaVersionName` 是唯一来源，同时用于 `versionName` 与 APK 文件名。ABI split 与 universal 的 `versionCode` 相同，切换包形态时用 `adb install -r`。
- **签名**：`KOMA_KEYSTORE`、`KOMA_KEYSTORE_PASSWORD`、`KOMA_KEY_PASSWORD` 由环境变量或 `~/.gradle/gradle.properties` 提供，别名默认 `app_sign`；缺失时 `verifyReleaseSigning` 直接失败。
- **CI**：`.github/workflows/release.yml` 在推送 `v*` 标签或手动触发时运行，固定 JDK 21、Gradle 9.9.1、API 37，先按 SHA-256 拉取 OCR 模型，再从 Secrets 解出 keystore 构建、验签、上传 artifact；tag 构建额外发布 GitHub Release。
- **Secrets**：仓库需要 `KOMA_KEYSTORE_BASE64`（keystore 的 base64）、`KOMA_KEYSTORE_PASSWORD`、`KOMA_KEY_PASSWORD`。上传示例：`base64 -w0 ~/android.p12 | gh secret set KOMA_KEYSTORE_BASE64`，另两个用 `gh secret set <名字>` 交互输入。
- **本地出发布包**：`export KOMA_KEYSTORE_PASSWORD=... KOMA_KEY_PASSWORD=...` 后执行 `mise run install-release`（安装 universal 包）。

## 迁移约束

- **视觉基线**：UI 用 Foundation 自绘，沿用 Capacitor 的亮暗配色、Inter 字体、字号、曲线和时长，不套用 Material、Miuix 等库的默认视觉。Inter 从原 WOFF2 生成相同字重的 TTF，避免换字体改变字宽。
- **首页与顶部模糊**：采用边到边滚动，只给标题和底部操作区分别应用系统安全区，不在页面根部裁切状态栏。顶部保留五层渐进模糊与羽化，采样半分辨率顶端切片；日期区块通过派生状态隔离逐帧滚动更新。
- **底栏玻璃与页面转场**：参考 [SukiSU FloatingBottomBar](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/main/manager/app/src/main/java/com/sukisu/ultra/ui/component/FloatingBottomBar.kt) 的三层绘制（原始来源 compose-miuix-ui / Kyant0 AndroidLiquidGlass，Apache-2.0）。不支持运行时着色器时退回纯色底板和可见选中底色。Dock 在三个主页面外只创建一次，三项各 76dp 加内边距共 236dp，底部居中，保留 8dp 加手势安全区。今天、课表、我的通过同一个横向 Pager 切换并保留页面状态，Dock 采样 Pager 当前实际画面。主页面与「我的」二三级页面共用 220ms 转场曲线；子页面按层级向前／向后轻移并淡入淡出，离场页面不再接收返回与边缘手势。设置中的「减少动态效果」开启时直接切换，关闭后恢复转场；系统关闭动画时也遵循 Compose 动画时长缩放。
- **日期岛**：贴右边缘悬浮，收起时只露 26dp 把手（日期、周几、展开箭头和常驻月历入口）；把手可横向拖拽，面板跟随手指展开或收起，松手先看速度再看是否过半吸附。展开后是纵向吸附日期条，底部为「今」，6 秒无操作自动收起。背景模糊采样时间线离屏录制层，边缘纵向羽化。
- **周课表**：底栏「课表」进入七列节次视图，全部节次适配屏幕高度，不纵向滚动。固定表头为左侧周号、上方星期、下方月日，今日有底部细线；横滑翻周落定后更新表头。点击标题或周号打开贴底边的连续月历，展开时对背后周课表施加渐变模糊。未配置时间的节次不补造钟点；重叠课程保留逐门详情。浏览其他周时显示短标签「今」浮钮，读屏动作是「回到本周」。
- **课程抽屉**：周课表点击课程打开贴底弹层，结构照参考实现：48dp 课程色徽标 + 课名 + 周次说明、时间/教师/地点/备注四行独立磁贴、相关课程折叠面板、底部「调课 / 停课」一排与删除主按钮（先选删除本节或删除本课，再破坏性确认）；弹层用周课表实际画面作背景模糊，叠加半透明底色与顶缘柔光，层级高于底部 Dock。备注分两层：课程简介全周共享，本节课备注只对当前教学周生效；停课/恢复按本周或全部周次；调课只移动当前这一周，目标周用「− / 字段 / ＋」，星期用胶囊，起止节次与地点点开选择器，目标行实时预览，未改动时确认置灰。手动修改全部本地优先。
- **课表同步**：教务账号页可开关自动更新，默认每天首次进入前台同步一次，失败保留本机课表并在账号页显示状态；同步以三方基线比较，本地备注、停课、调课不被静默覆盖，出现冲突时弹层逐条选择保留本机或采用教务；采用教务排课时仍保留本地备注。执行 `tools/check.sh` 会同时校验结构规则、测试与调试包；提交钩子只跑 `tools/check-structure.sh`（目标两秒内，可用 `git config core.hooksPath .githooks` 启用）。
- **图标与许可**：`ui/component/icon/KomaGlyph.kt` 保存 MingCute SVG 路径，`KomaIcon.kt` 绘制为原生 Compose `ImageVector`。启动图标为自适应图标，`app/icon.svg` 是 108dp 画布的设计源，前景与 `res/drawable/ic_launcher_foreground.xml` 共用坐标（内容框 42dp，落在 66dp 安全圆内），背景为白到 `#F2F2F1` 竖向渐变，`ic_launcher_monochrome.xml` 供单色着色；minSdk 为 29，`mipmap-anydpi-v26` 是唯一生效的图标资源。字体与图标许可放在 `composeResources/files/licenses/`。
- **兼容与数据迁移**：应用 ID 保持 `moe.tnxg.koma`，原 Keystore 别名、偏好文件和通知渠道保持兼容。首次启动在读取课表前，从旧 WebView 存储读取 `koma.snapshot` 与 `koma.startDate`，写入成功才记录迁移完成，不删除原 localStorage。WebView 仅用于一次性升级读取，不承载界面。
- **首次使用**：没有本地快照时显示欢迎页，使用 slogan「列车终将开往下一站，那舞台呢？我们呢？」；点击「开始」先选择学校，选择后才创建登录连接。当前仅列出已接入的新疆理工职业大学，不提供尚未实现的手动添加或跳过入口。导入后确认开学日期；已有完整课表直接进入首页，只有快照但未确认日期则恢复日期设置。「重新导入」也先选学校，返回不清除离线课表；登录页返回会取消当前操作，退出登录并清除课表后重新显示欢迎页。
- **真实数据**：网络登录需要学校服务可访问，不把测试课表或虚假登录作为生产回退。
- **CAS 登录边界**：不带 `service` 的登录探测与凭据提交只读取当前 CAS 响应，不跟随其默认门户跳转；随后从教务 SSO 入口换票并验证课表索引页。门户会话不属于教务 Cookie 白名单，跟随默认门户会造成 CAS／门户循环。`LoginSessionTest` 覆盖已有 CAS 会话恢复与首次登录提交这两条路径。
- **验证码拒绝**：CAS 的验证码错误可能以 HTTP 401 携带登录表单返回，仍须识别 `FailedAuthcodeException`，复用该表单的 `execution`。自动登录最多尝试 3 轮，每轮失败后更换验证码，尝试期间保持登录中且不展示失败；连续失败后提供一张新图供手动输入，提示「自动登录失败，请手动输入验证码」。`XjvutEduServiceTest` 覆盖 200／401 失败后的刷新与重试，`AccountSyncTest` 覆盖换图、无中途失败提示及手动提交。

## 添加其他学校的教务系统

接入前先看现有实现：唯一接口是 `core/src/commonMain/kotlin/moe/tnxg/koma/data/education/EduService.kt`（包含 `prepare`、`refreshCaptcha`、`login`、`importTimetable`、`logout`），领域模型在 `core/src/commonMain/kotlin/moe/tnxg/koma/domain/model/Timetable.kt`（`Course`、`Semester`、`TimeSlot`、`Term`、`TimetableSnapshot`、`courseIdOf`）。

按下面 12 步接入新学校：

1. **判断是不是正方 `jwglxt`**：是的话，可以直接复用 `core/src/commonMain/kotlin/moe/tnxg/koma/data/education/zhengfang/` 里的 `WeeklyParser.kt`（`parseZfWeekly`）和 `TermCalendar.kt`（`zfTermWeeks`、`zfTimeGrid`、`termLabel`、`guessTerm`、`DEFAULT_TIME_GRID`），工作量主要在地址和登录；不是正方就需要自己写解析器。
2. **新建学校目录**：在 `core/src/androidMain/kotlin/moe/tnxg/koma/data/education/<school>/` 下建三个文件（目录名全小写、不带连字符）：
   - `Endpoints.kt`：放 CAS、SSO、课表、周次、节次时间等 URL 常量，以及 `isIndex(url)` 判断是否已登录。
   - `LoginSession.kt`：实现 `prepare`、`refreshCaptcha`、`login`、`restore`、`invalidate`、`reset`（参考 `xjvut/LoginSession.kt` 和 `cas/Cas.kt`）。
   - `TimetableImporter.kt`：实现 `fetch(): TimetableSnapshot`（参考 `xjvut/TimetableImporter.kt`）。
3. **放置非正方解析器**：如果目标学校不是正方，把解析器放到 `core/src/commonMain/kotlin/moe/tnxg/koma/data/education/<school>/`，写成纯函数——输入 HTML 或 JSON，输出 `Course`、`Semester`、`TimeSlot`，这样就能直接在 `commonTest` 里写单元测试。
4. **实现 `EduService`**：新建 `core/src/androidMain/kotlin/moe/tnxg/koma/data/education/<School>EduService.kt`，照 `XjvutEduService.kt` 的骨架来写：用 `Mutex` 串行化操作、切到 `Dispatchers.IO`、登录或导入抛异常时调用 `session.invalidate()`。
5. **放行域名**：修改 `core/src/androidMain/kotlin/moe/tnxg/koma/data/education/http/CookieVault.kt` 的 `allowed(host)`，把新学校的 CAS 域名和教务域名加进去。不加的话 Cookie 存不下来，登录后会立刻掉线。
6. **替换平台装配**：修改 `core/src/androidMain/kotlin/moe/tnxg/koma/platform/AndroidPlatformServices.kt`，把 `edu` 换成新学校的 `EduService` 实例，同步改好 cookie 文件名和凭据 key（`vault.load/save/remove("<schoolId>")`）。
7. **统一 `schoolId`**：`TimetableSnapshot.schoolId` 和 `Course.source["pluginId"]` 必须用同一个字符串。`ui/compose/src/commonMain/kotlin/moe/tnxg/koma/app/session/TimetableSession.kt` 靠 `schoolId` 和 `term` 判断是否换校或换学期，变了就会清空开学日期。
8. **修改学校名称**：修改 `ui/compose/src/commonMain/kotlin/moe/tnxg/koma/feature/login/SchoolScreen.kt` 的 `SCHOOL_NAME`。学校选择卡片和登录页副标题共用该名称；学校列表必须与平台实际装配的教务服务一致。
9. **处理节次时间**：如果新学校拿不到 `TIMES` 接口，`zfTimeGrid` 会返回空，系统会回退到同校上次的 grid 或 `DEFAULT_TIME_GRID`。如果新学校上下课时间不同，提供自己的默认 grid，避免学生看到错的钟点。
10. **补充测试并检查**：照 `core/src/androidHostTest/kotlin/moe/tnxg/koma/data/education/XjvutEduServiceTest.kt` 的 `EducationFixture` 编排「登录 → 周次 → 课表」全链路拦截器，断言 `importTimetable()` 返回的 `TimetableSnapshot`；然后跑 `mise run test` 和 `mise run check`。
11. **真机走一遍完整流程**：首次登录、验证码识别错后重试、退出后再登录、杀进程重开能否免登录重新导入、改开学日期、换学期导入。
12. **多学校并存原则**：不要在校内实现里写 `if (学校 == ...)`。当前选择页仅展示平台已装配的新疆理工职业大学，不能只增加学校名称而继续调用同一个服务。接入多校时，选择结果必须切换 `PlatformServices.edu` 及对应 Cookie、凭据命名空间，一台设备同时只持有一个 `EduService` 实例，靠 `schoolId` 区分快照。
