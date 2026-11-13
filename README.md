# Koma

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

依赖方向：`app → ui:compose → core`，`app` 也直接依赖 `core` 以构造 Android 平台实现。KMP 模块使用官方 `com.android.kotlin.multiplatform.library` 插件，不将 Android Application 插件与 KMP 插件混在一个模块。当前没有桌面、iOS 或 Web 宿主。

组件用真实目录表达所属关系，不通过连字符文件名模拟层级。Kotlin 文件不超过 350 行；`mise run check` 检查该约束。

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

- JDK、Gradle 由 mise 固定；不依赖机器上的绝对工具路径。
- Android SDK 安装到忽略的 `.tools/android/`，SDK 环境由 mise 配置。
- `setup` 获取 API 35、Build Tools 和 OCR 模型；模型按固定 SHA-256 校验。
- `build` 产物：`app/build/outputs/apk/debug/app-debug.apk`。
- `run` 需要已连接的 Android 设备或模拟器。
- 单独检查：`mise run check`。
- Android 设备测试：`mise exec -- gradle :core:connectedAndroidDeviceTest`。

## 迁移约束

UI 使用 Foundation 自定义布局与绘制，沿用 Capacitor 的亮暗配色、Inter 字体、字号、曲线和时长；不套用 Material、Miuix 等库的默认视觉。Inter 从原 WOFF2 生成相同字重的 TTF，避免换字体改变字宽。

首页采用边到边滚动：只给标题和底部操作区分别应用系统安全区，不在页面根部裁切状态栏区域。顶部保留 Web 的五层渐进模糊与羽化，模糊采样使用半分辨率顶端切片；日期区块通过派生状态隔离无关的逐帧滚动更新。底栏底部留白为 8dp 加系统手势安全区，渐隐背景覆盖手势区，避免课程内容透到手势条下方。

日期导航改为贴右边缘的悬浮日期岛，不再占用底栏。收起时只露 26dp 把手，显示日期、周几与展开箭头；展开后是纵向吸附日期条，顶部为月历入口、底部为「今」，6 秒无操作自动收起。岛体背景模糊直接采样时间线的离屏录制层，边缘用纵向羽化擦除，避免遮挡线路内容。连续选日取消上一次跳转任务；滚动驱动的日期位移直接使用手势进度，不二次缓动，标题透明度与位移在绘制层更新。

周课表：底栏「课表」进入七列节次视图，全部节次适配屏幕高度，不纵向滚动。固定表头采用左侧周号、上方星期和下方月日的紧凑布局，今日有底部细线；横滑翻周落定后更新表头。点击标题或表头周号打开贴屏幕底边的连续月历，展开时对背后的周课表施加渐变模糊，日历内容保持清晰，不额外添加底部白边。课程均来自已导入课表，未配置时间的节次不补造钟点；重叠课程保留逐门详情。当前课程时间线只在课程起止范围内显示。点击星期日期进入对应时间线，「今天」和系统返回回到今日；浏览其他周时显示短标签「今」浮钮，读屏动作是「回到本周」，不显示原全文字胶囊或抽屉底部按钮。视觉沿用现有 Foundation 组件、课程色与亮暗主题。

周数选择月历的底部使用由清晰到模糊的渐进遮罩，采样月历自身底部内容并随弹层移动；周课表底栏不添加渐隐背景。

图标：`ui/component/icon/KomaGlyph.kt` 保存原 MingCute SVG 路径，`KomaIcon.kt` 将路径绘制为原生 Compose `ImageVector`，统一处理颜色和无障碍说明；没有图标字体、emoji 替代或 Material 图标依赖。启动图标沿用原 Android 资源。字体与图标许可随资源放在 `composeResources/files/licenses/`。

应用 ID 保持 `moe.tnxg.koma`。原凭据 Keystore 别名、偏好文件和通知渠道保持兼容。首次启动在读取课表前，从原 `https://localhost/` WebView 存储提取 `koma.snapshot` 与 `koma.startDate`，写入成功才记录迁移完成；不删除原 localStorage。WebView 仅用于一次性升级数据读取，不承载界面。

网络登录需要学校服务可访问；不将测试课表或虚假登录作为生产回退。实体设备对照必须使用同一数据、日期、尺寸和主题；构建或模拟器测试通过不代表视觉已完全一致。
