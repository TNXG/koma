<p align="center">
  <img src="docs/screenshots/icon.png" width="88" alt="Koma Pro">
</p>

<h1 align="center">Koma Pro</h1>

<p align="center"><strong>列车终将开往下一站，那舞台呢？我们呢？</strong></p>

<p align="center">课表应用。看今天上什么课、几点上下课、下一节去哪里。</p>

<table>
  <tr>
    <td><a href="docs/screenshots/home.png"><img src="docs/screenshots/home.png" width="200" alt="今天的课程时间线"></a></td>
    <td><a href="docs/screenshots/week.png"><img src="docs/screenshots/week.png" width="200" alt="整周课表"></a></td>
    <td><a href="docs/screenshots/calendar.png"><img src="docs/screenshots/calendar.png" width="200" alt="月历选日期"></a></td>
    <td><a href="docs/screenshots/profile.png"><img src="docs/screenshots/profile.png" width="200" alt="我的：重新导入、修改开学时间"></a></td>
  </tr>
  <tr>
    <td align="center">今天</td>
    <td align="center">课表</td>
    <td align="center">月历</td>
    <td align="center">我的</td>
  </tr>
</table>

## 安装

当前版本 0.0.1，包名 `moe.tnxg.koma`，只支持 Android 10 及以上系统。

没有上架应用商店。打 `v*` 标签后 CI 自动构建并发布 Release，每个版本提供 `arm64-v8a`、`armeabi-v7a`、`x86`、`x86_64` 四个分包和覆盖全部架构的 `universal` 包：按手机架构选分包体积最小，不确定就装 `universal`。手里有 APK 就直接传到手机安装；没有的话，按下面的「自己打包」自己编译。

## 第一次使用

1. 欢迎页可选择「开始使用」登录教务，或「导入离线课表」「暂不登录」。进入学校选择和教务登录后，只显示登录相关操作。
2. 教务登录需要学号、教务密码和验证码。验证码在本机自动识别，也可手动输入；登录后抓取整学期课表，确认开学日期即可使用。
3. 不登录时可浏览空课表、个人资料和设置，之后随时导入。选择会保存在本机，下次启动无需再次跳过登录。
4. 课表保存在本机，断网也能看。「我的 → 教务账号」可再次登录或导入 JSON；开学日期可在「学期与课表」里修改。

### JSON 导入与 AI 识图

在导入页选择 UTF-8 `.json` 文件，或直接粘贴 JSON。点击「解析并预览」，核对学期、首周周一日期、周数和排课项数，再确认导入。解析失败不会覆盖原课表。JSON 课表不会自动连接教务同步；同学期再次导入会沿用本地修改及冲突处理。

导入页提供可展开的 **Koma JSON v1 格式指南**、完整例子，以及可复制的 **AI 识图提示词**：将提示词和课表截图发送给支持识图的 AI，补齐图片中缺少的开学日期、周数、节次钟点，再把生成的 JSON 带回导入。发送截图前请遮住姓名、学号等隐私；AI 结果须自行核对。

格式使用 `version`、`semester`、`timeGrid`、`courses` 和可选的 `studentName`。星期用 1–7 表示周一至周日；`weeks` 必须列出实际上课周次，单双周也展开成数组；节次时间用 `HH:mm`，不自动套用其他学校的作息。完整字段与取值限制以应用内格式指南为准。

之前用过旧版（WebView/Capacitor 版本）的用户，首次打开会自动迁入原课表，不会删掉旧数据。

## 怎么看课表

### 今天

一天的课程按时间轴排成一条线，标出开始和结束时间、教室、老师。课间空档直接写「空闲 3 小时 20 分钟」，不用自己算。换天、周末、下一周都有分隔。

屏幕右边缘有一枚日期把手。轻点或向左滑动展开日期条，向右滑动收起，6 秒无操作会自动收起。把手底部的月历按钮一直在，收起时也能直接打开月历。

### 课表

七列节次视图，整周一屏显示，不用上下滚动。左右滑动换周，今天那列会高亮。

点顶部周号可以打开月历；点上方某一天，回到那天行程。

### 月历

从屏幕底边升起，按周排列，显示第几周、每天几门课。可以选一整周，也可以选一天。

### 我的

头图、头像、学期进度和入口都在这一页。

- **个人资料**：改称呼；头像与封面可以从相册选择，选过之后就不再随机。自动生成的头像登录后固定，身份头像静态显示，返回页面不再重复入场或摇摆；退出登录再登录才会换一个。随机封面在本机缓存三天。
- **教务账号**：登录教务、重新导入或通过 JSON 导入课表，也可清除本机课表。未登录和 JSON 课表会明确显示离线状态。
- **学期与课表**：看周次进度，改开学日期。
- **设置**：主题与强调色、课表显示、课程卡片、首页落点、课前提醒，都可一键恢复默认。
- **关于**：版本、开源协议、隐私与权限、开源许可、反馈问题。

设置和关于的每一页都能用左上角按钮、系统返回或从屏幕左缘右滑返回。

## 常见问题

| 遇到的情况 | 怎么办 |
| --- | --- |
| 周次对不上 | 到「我的」修改开学时间 |
| 课表有变动 | 到「我的」重新导入课表 |
| 验证码识别错了 | 换一张，或手动输入 |
| 登录提示被暂时锁定 | 密码连续输错会触发临时锁定，等 10–15 分钟再试；仍不行就到统一身份认证网页登录或联系网络信息中心解锁 |
| 某些节次没有钟点 | 课表里没配置时间的节次不显示钟点，应用不会猜 |
| 想清除课表 | 到「我的」退出登录并清除课表 |

## 隐私

课表保存在本机，账号凭据用 Android Keystore 加密保存在本机，验证码在本机识别。

没有广告，没有第三方统计。

## 自己打包

先装 [mise](https://mise.jdx.dev)，然后在仓库根目录执行：

```sh
mise trust
mise install
mise run setup
mise run build
```

Windows、macOS、Linux 用同一套命令。`mise run setup` 按平台分派：macOS / Linux 走 `tools/setup.sh`，Windows 走 `tools/setup.ps1`，装好 Android SDK（API 37、Build Tools 36.0.0 与 37.0.0、platform-tools）并按 SHA-256 校验 OCR 模型；Windows 只需要系统自带的 PowerShell，不用 WSL 或 Git Bash。

产物在 `app/build/outputs/apk/debug/app-debug.apk`，传到手机安装即可；连了设备也可以直接 `mise run run` 装上并启动。

## 反馈与源码

遇到问题请到 [GitHub Issues](https://github.com/TNXG/koma/issues) 反馈。源码同时托管在 [GitHub](https://github.com/TNXG/koma) 与 [GitCode](https://gitcode.com/gxdev_lab/koma)，两边内容一致。改代码请看 [开发说明](docs/DEVELOPMENT.md)。

## 开源协议

采用 AGPL-3.0 协议，版权归 GengXing Developer Lab & TNXG 所有。
