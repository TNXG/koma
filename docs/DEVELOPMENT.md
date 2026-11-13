# 开发说明

面向修改本项目的人。用户文档见 [README](../README.md)。

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
- `mise run build` 产物：`app/build/outputs/apk/debug/app-debug.apk`。
- `mise run run` 需要已连接的 Android 设备或模拟器。
- 单独检查：`mise run check`。
- 设备测试：`mise exec -- gradle :core:connectedAndroidDeviceTest`。

## 迁移约束

- **视觉基线**：UI 用 Foundation 自绘，沿用 Capacitor 的亮暗配色、Inter 字体、字号、曲线和时长，不套用 Material、Miuix 等库的默认视觉。Inter 从原 WOFF2 生成相同字重的 TTF，避免换字体改变字宽。
- **首页与顶部模糊**：采用边到边滚动，只给标题和底部操作区分别应用系统安全区，不在页面根部裁切状态栏。顶部保留五层渐进模糊与羽化，采样半分辨率顶端切片；日期区块通过派生状态隔离逐帧滚动更新。
- **底栏玻璃**：参考 [SukiSU FloatingBottomBar](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/main/manager/app/src/main/java/com/sukisu/ultra/ui/component/FloatingBottomBar.kt) 的三层绘制（原始来源 compose-miuix-ui / Kyant0 AndroidLiquidGlass，Apache-2.0）。不支持运行时着色器时退回纯色底板和可见选中底色。Dock 在时间线与周课表外只创建一次，三项各 76dp 加内边距共 236dp，底部居中，保留 8dp 加手势安全区。时间线与周课表通过同一个横向 Pager 切换，Dock 采样 Pager 当前实际画面。系统关闭动画时遵循 Compose 动画时长缩放。
- **日期岛**：贴右边缘悬浮，收起时只露 26dp 把手（显示日期、周几与展开箭头）；展开后是纵向吸附日期条，顶部为月历入口、底部为「今」，6 秒无操作自动收起。背景模糊采样时间线离屏录制层，边缘纵向羽化。
- **周课表**：底栏「课表」进入七列节次视图，全部节次适配屏幕高度，不纵向滚动。固定表头为左侧周号、上方星期、下方月日，今日有底部细线；横滑翻周落定后更新表头。点击标题或周号打开贴底边的连续月历，展开时对背后周课表施加渐变模糊。未配置时间的节次不补造钟点；重叠课程保留逐门详情。浏览其他周时显示短标签「今」浮钮，读屏动作是「回到本周」。
- **月历遮罩**：底部使用由清晰到模糊的渐进遮罩，采样月历自身底部内容并随弹层移动。
- **图标与许可**：`ui/component/icon/KomaGlyph.kt` 保存 MingCute SVG 路径，`KomaIcon.kt` 绘制为原生 Compose `ImageVector`。启动图标为自适应图标，`app/icon.svg` 是 108dp 画布的设计源，前景与 `res/drawable/ic_launcher_foreground.xml` 共用坐标（内容框 42dp，落在 66dp 安全圆内），背景为白到 `#F2F2F1` 竖向渐变，`ic_launcher_monochrome.xml` 供单色着色；minSdk 为 29，`mipmap-anydpi-v26` 是唯一生效的图标资源。字体与图标许可放在 `composeResources/files/licenses/`。
- **兼容与数据迁移**：应用 ID 保持 `moe.tnxg.koma`，原 Keystore 别名、偏好文件和通知渠道保持兼容。首次启动在读取课表前，从旧 WebView 存储读取 `koma.snapshot` 与 `koma.startDate`，写入成功才记录迁移完成，不删除原 localStorage。WebView 仅用于一次性升级读取，不承载界面。
- **真实数据**：网络登录需要学校服务可访问，不把测试课表或虚假登录作为生产回退。

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
8. **修改登录页副标题**：修改 `ui/compose/src/commonMain/kotlin/moe/tnxg/koma/feature/login/LoginScreen.kt` 里的副标题文案（目前是「新疆理工职业大学 · 统一身份认证」）。这是 UI 里唯一硬编码学校名的地方，其他界面不要出现学校名。
9. **处理节次时间**：如果新学校拿不到 `TIMES` 接口，`zfTimeGrid` 会返回空，系统会回退到同校上次的 grid 或 `DEFAULT_TIME_GRID`。如果新学校上下课时间不同，提供自己的默认 grid，避免学生看到错的钟点。
10. **补充测试并检查**：照 `core/src/androidHostTest/kotlin/moe/tnxg/koma/data/education/XjvutEduServiceTest.kt` 的 `EducationFixture` 编排「登录 → 周次 → 课表」全链路拦截器，断言 `importTimetable()` 返回的 `TimetableSnapshot`；然后跑 `mise run test` 和 `mise run check`。
11. **真机走一遍完整流程**：首次登录、验证码识别错后重试、退出后再登录、杀进程重开能否免登录重新导入、改开学日期、换学期导入。
12. **多学校并存原则**：不要在校内实现里写 `if (学校 == ...)`。一台设备只持有一个 `EduService` 实例，靠 `schoolId` 区分快照。以后要加「选学校」入口时，只需在登录页前加一层选择，把 `PlatformServices.edu` 换成对应实现。
