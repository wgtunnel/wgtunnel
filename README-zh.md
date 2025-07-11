<h1 align="center">
WG 隧道
</h1>

<div align="center">

[![Discord](https://img.shields.io/badge/Discord-%235865F2.svg?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/rbRRNh6H7V)
[![X Community](https://img.shields.io/badge/X-000000?style=for-the-badge&logo=x&logoColor=white)](https://twitter.com/i/communities/1780655267685736818)
[![Telegram](https://img.shields.io/badge/Telegram-2CA5E0?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/wgtunnel)

</div>

<div align="center">


[![Google Play](https://img.shields.io/badge/Google_Play-414141?style=for-the-badge&logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.zaneschepke.wireguardautotunnel)
[![F-Droid](https://img.shields.io/static/v1?style=for-the-badge&message=F-Droid&color=1976D2&logo=F-Droid&logoColor=FFFFFF&label=)](https://f-droid.org/packages/com.zaneschepke.wireguardautotunnel/)


</div>


<div align="left">

这是一款可供选择的 Android 应用程序，用于 [WireGuard](https://www.wireguard.com/) and [AmneziaWG](https://docs.amnezia.org/documentation/amnezia-wg/) 增加了
功能。 使用 [wireguard-android](https://github.com/WireGuard/wireguard-android)
图书馆和  [Jetpack Compose](https://developer.android.com/jetpack/compose), 此应用程序
灵感来自官方 [WireGuard Android](https://github.com/WireGuard/wireguard-android) app.

</div>

<div align="center">

## 屏幕截图

<p float="center">
  <img label="Main" style="padding-right:25px" src="fastlane/metadata/android/en-US/images/phoneScreenshots/main_screen.png" width="200" />
  <img label="Config" style="padding-left:25px" src="fastlane/metadata/android/en-US/images/phoneScreenshots/config_screen.png" width="200" />
  <img label="Settings" style="padding-left:25px" src="fastlane/metadata/android/en-US/images/phoneScreenshots/settings_screen.png" width="200" />
  <img label="Support" style="padding-left:25px" src="fastlane/metadata/android/en-US/images/phoneScreenshots/support_screen.png" width="200" />
</p>

<div align="left">

## 灵感

最初的灵感来源于 WireGuard 这款应用程序最初的灵感来自于在不同网络中手动关闭和打开 VPN 的不便。
的不便。 创建这款应用程序的目的就是为这一问题提供免费的解决方案.

## 特点

* 通过 .conf 文件,压缩文件,手动输入或 QR 码添加隧道
* 根据 Wi-Fi SSID,以太网或移动数据自动连接到隧道
* 通过搜索按应用程序分割隧道
* 内核和用户空间模式的 WireGuard 支持
* Amnezia 支持用于 DPI/审查保护的用户空间模式
* 始终支持 VPN
* 将 Amnezia 和 WireGuard 隧道导出为 zip 文件
* 支持隧道切换、自动隧道的快速瓦片功能
* 静态快捷方式支持隧道切换和自动隧道功能
* 支持所有隧道的意图自动化
* 重启后自动重启自动隧道服务
* 重启后自动重启隧道
* 电池保护措施
* ping 失败时重启隧道（测试版）

## 文档

有关此应用程序功能和行为的基本文档，请参见
找到 [here](https://zaneschepke.com/wgtunnel-docs/overview.html).

这些文档的存储库如下所示 [这里](https://github.com/zaneschepke/wgtunnel-docs).

## 捐款

欢迎并衷心感谢任何反馈、问题、代码或翻译形式的贡献!

请阅读 [行为准则](https://github.com/zaneschepke/wgtunnel?tab=coc-ov-file#contributor-code-of-conduct) 捐款前.

## 翻译

该应用程序使用 [Weblate](https://weblate.org) 协助翻译. 

帮助将 WG Tunnel 翻译成您的语言，请访问 [Hosted Weblate](https://hosted.weblate.org/engage/wg-tunnel/).\
[![翻译状态](https://hosted.weblate.org/widgets/wg-tunnel/-/multi-auto.svg)](https://hosted.weblate.org/engage/wg-tunnel/)


## 建筑

```
$ git clone https://github.com/zaneschepke/wgtunnel
$ cd wgtunnel
```

然后构建应用程序:

```
$ ./gradlew assembleDebug
```

</span>
