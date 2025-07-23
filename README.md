<h1 align="center">
WG Tunnel
</h1>

<div align="center">

An alternative Android client app for [WireGuard](https://www.wireguard.com/)
and [AmneziaWG](https://docs.amnezia.org/documentation/amnezia-wg/)
<br />
<br />
<a href="https://github.com/zaneschepke/wgtunnel/issues/new?assignees=zaneschepke&labels=bug&projects=&template=bug_report.md&title=%5BBUG%5D+-+Problem+with+app">Report a Bug</a>
·
<a href="https://github.com/zaneschepke/wgtunnel/issues/new?assignees=zaneschepke&labels=enhancement&projects=&template=feature_request.md&title=%5BFEATURE%5D+-+New+feature+request">Request a Feature</a>
·
<a href="https://github.com/zaneschepke/wgtunnel/discussions">Ask a Question</a>

</div>

<br/>

<div align="center">

[![Google Play](https://img.shields.io/badge/Google_Play-414141?style=for-the-badge&logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.zaneschepke.wireguardautotunnel)
[![F-Droid](https://img.shields.io/static/v1?style=for-the-badge&message=F-Droid&color=1976D2&logo=F-Droid&logoColor=FFFFFF&label=)](https://f-droid.org/packages/com.zaneschepke.wireguardautotunnel/)
[![Personal](https://img.shields.io/static/v1?style=for-the-badge&message=Personal&color=1976D2&logo=F-Droid&logoColor=FFFFFF&label=)](https://github.com/zaneschepke/fdroid)
[![Obtainium](https://img.shields.io/badge/Obtainium-414141?style=for-the-badge&logo=Obtainium&logoColor=white)](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.zaneschepke.wireguardautotunnel%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fzaneschepke%2Fwgtunnel%22%2C%22author%22%3A%22zaneschepke%22%2C%22name%22%3A%22WG%20Tunnel%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Afalse%2C%5C%22fallbackToOlderReleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22filterReleaseNotesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22verifyLatestTag%5C%22%3Atrue%2C%5C%22sortMethodChoice%5C%22%3A%5C%22date%5C%22%2C%5C%22useLatestAssetDateAsReleaseDate%5C%22%3Afalse%2C%5C%22releaseTitleAsVersion%5C%22%3Afalse%2C%5C%22trackOnly%5C%22%3Afalse%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22matchGroupToUse%5C%22%3A%5C%22%5C%22%2C%5C%22versionDetection%5C%22%3Atrue%2C%5C%22releaseDateAsVersion%5C%22%3Afalse%2C%5C%22useVersionCodeAsOSVersion%5C%22%3Afalse%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22WG%20Tunnel%5C%22%2C%5C%22appAuthor%5C%22%3A%5C%22Zane%20Schepke%5C%22%2C%5C%22shizukuPretendToBeGooglePlay%5C%22%3Afalse%2C%5C%22allowInsecure%5C%22%3Afalse%2C%5C%22exemptFromBackgroundUpdates%5C%22%3Afalse%2C%5C%22skipUpdateNotifications%5C%22%3Afalse%2C%5C%22about%5C%22%3A%5C%22%5C%22%2C%5C%22refreshBeforeDownload%5C%22%3Afalse%7D%22%2C%22overrideSource%22%3Anull%7D)

## Explanation
[中文](README-zh.md)


</div>

<div align="center">

[<img  src="https://img.shields.io/badge/Telegram-26A5E4.svg?style=for-the-badge&logo=Telegram&logoColor=white">](https://t.me/wgtunnel)
[<img src="https://img.shields.io/badge/Matrix-000000.svg?style=for-the-badge&logo=Matrix&logoColor=white">](https://matrix.to/#/#wg-tunnel-space:matrix.org)
</div>

<details open="open">
<summary>Table of Contents</summary>

- [About](#about)
- [Acknowledgements](#acknowledgements)
- [Screenshots](#screenshots)
- [Features](#features)
- [Building](#building)
- [Translation](#translation)
- [Contributing](#contributing)

</details>

<div style="text-align: left;">

## About
Inspired by the official [wireguard-android](https://github.com/WireGuard/wireguard-android) app, WG Tunnel was created to address features and support missing from the official app. This app combines support for both [WireGuard](https://www.wireguard.com/)
and [AmneziaWG](https://docs.amnezia.org/documentation/amnezia-wg/), with its primary feature of auto-tunneling (on-demand tunneling).

</div>

<div style="text-align: left;">

## Acknowledgements

Thank you to the following:

- All of the users that have helped contribute to the project with ideas, translations, feedback, bug reports, testing, and donations.
- [WireGuard](https://www.wireguard.com/) - Jason A. Donenfeld (https://github.com/WireGuard/wireguard-android)

- [AmneziaWG](https://docs.amnezia.org/documentation/amnezia-wg/) - Amnezia Team (https://github.com/amnezia-vpn/amneziawg-android)

## Screenshots

</div>
<div style="display: flex; flex-wrap: wrap; justify-content: left; gap: 10px;">
 <img label="Main" src="fastlane/metadata/android/en-US/images/phoneScreenshots/main_screen.png" width="200" />
 <img label="Settings" src="fastlane/metadata/android/en-US/images/phoneScreenshots/settings_screen.png" width="200" />
  <img label="Auto" src="fastlane/metadata/android/en-US/images/phoneScreenshots/auto_screen.png" width="200" />
  <img label="Config" src="fastlane/metadata/android/en-US/images/phoneScreenshots/config_screen.png" width="200" />
</div>

<div style="text-align: left;">

## Features

* Add tunnels via .conf file, zip, manual entry, clipboard, or QR code
* Auto-tunnel based on Wi-Fi SSID, ethernet, or mobile data
* Split tunneling by application with search
* Support for kernel and userspace modes
* Amnezia support for userspace mode for DPI/censorship protection
* Pre/Post Up/Down scripts support for all modes on a rooted device
* Always-On VPN support
* Export tunnels to zip
* Quick tile support for tunnel toggling, auto-tunneling
* Shortcuts support for tunnel toggling, auto-tunneling
* Intent automation support for all tunnels
* In app VPN kill switch with LAN bypass
* Automatic auto-tunneling service and/or tunnel restart after reboot or app update
* Battery preservation measures
* Restart tunnel on ping failure

## Building

```sh
git clone https://github.com/zaneschepke/wgtunnel
cd wgtunnel
```

```sh
./gradlew assembleDebug
```

## Translation

This app is using [Weblate](https://weblate.org) to assist with translations.

Help translate WG Tunnel into your language
at [Hosted Weblate](https://hosted.weblate.org/engage/wg-tunnel/).\
[![Translation status](https://hosted.weblate.org/widgets/wg-tunnel/-/multi-auto.svg)](https://hosted.weblate.org/engage/wg-tunnel/)

## Contributing

Any contributions in the form of feedback, issues, code, or translations are welcome and much
appreciated!

Please read
the [code of conduct](https://github.com/zaneschepke/wgtunnel?tab=coc-ov-file#contributor-code-of-conduct)
before contributing.
