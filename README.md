<p align="center">
  <img src="docs/screenshots/icon.png" width="88" alt="Koma Pro">
</p>

<h1 align="center">Koma Pro</h1>

<p align="center">给新疆理工职业大学学生用的课表应用。看今天上什么课、几点上下课、下一节去哪里。</p>

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

当前版本 0.2.0，包名 `moe.tnxg.koma`，只支持 Android 10 及以上系统。

没有上架应用商店，也没有发布 Release。手里有 APK 就传到手机安装；没有的话，按下面的「自己打包」自己编译。

## 第一次使用

1. 打开登录页（「新疆理工职业大学 · 统一身份认证」），输入学号、教务密码和验证码。验证码在本机用 ONNX 模型自动识别，识别错了可以换一张，也可以手动输入。
2. 登录后抓取整学期课表存在手机里，断网也能看。
3. 确认开学日期。它用来算「第几周」，之后可以在「我的」里修改。

之前用过旧版（WebView/Capacitor 版本）的用户，首次打开会自动迁入原课表，不会删掉旧数据。

## 怎么看课表

### 今天

一天的课程按时间轴排成一条线，标出开始和结束时间、教室、老师。课间空档直接写「空闲 3 小时 20 分钟」，不用自己算。换天、周末、下一周都有分隔。

屏幕右边缘有一枚日期把手。轻点展开日期条选日期，6 秒无操作会自动收起。

### 课表

七列节次视图，整周一屏显示，不用上下滚动。左右滑动换周，今天那列会高亮。

点顶部周号可以打开月历；点上方某一天，回到那天行程。

### 月历

从屏幕底边升起，按周排列，显示第几周、每天几门课。可以选一整周，也可以选一天。

### 我的

显示学生姓名和学期。可以在这里重新导入课表、修改开学时间，或者退出登录并清除课表。

深浅色外观跟随系统。

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

在仓库根目录执行：

```sh
mise run setup
mise run build
```

产物在 `app/build/outputs/apk/debug/app-debug.apk`，传到手机安装即可。

## 反馈与源码

遇到问题请到 [GitHub Issues](https://github.com/TNXG/koma/issues) 反馈，源码仓库在 [https://github.com/TNXG/koma](https://github.com/TNXG/koma)。改代码请看 [开发说明](docs/DEVELOPMENT.md)。

## 开源协议

采用 AGPL-3.0 协议，版权归 TNXG 所有。
