object Constants {
	const val VERSION_NAME = "3.9.3"
    const val JVM_TARGET = "17"
    const val VERSION_CODE = 39300
    const val TARGET_SDK = 35
    const val MIN_SDK = 26
    const val APP_ID = "com.zaneschepke.wireguardautotunnel"
    const val APP_NAME = "wgtunnel"

    // build types
    const val RELEASE = "release"
    const val NIGHTLY = "nightly"
	const val PRERELEASE = "prerelease"

    val allowedLicenses = listOf("MIT", "Apache-2.0", "BSD-3-Clause")
    val allowedLicenseUrls = listOf("https://github.com/journeyapps/zxing-android-embedded/blob/master/COPYING",
        "https://github.com/RikkaApps/Shizuku-API/blob/master/LICENSE")
}
