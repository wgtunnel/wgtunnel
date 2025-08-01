
import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project
import org.semver4j.Semver

fun Project.languageList(): List<String> {
	return fileTree("../app/src/main/res") { include("**/strings.xml") }
		.asSequence()
		.map { stringFile -> stringFile.parentFile.name }
		.map { valuesFolderName -> valuesFolderName.replace("values-", "") }
		.filter { valuesFolderName -> valuesFolderName != "values" }
		.map { languageCode -> languageCode.replace("-r", "_") }
		.distinct()
		.sorted()
		.toList() + "en"
}


fun buildLanguagesArray(languages: List<String>): String {
    return languages.joinToString(separator = ", ") { "\"$it\"" }
}

// Get the Git commit hash
fun Project.getGitCommitHash(): String {
    var grgit: Grgit? = null
    try {
        grgit = Grgit.open(mapOf("currentDir" to projectDir))
        return grgit.head().abbreviatedId
    } catch (e: Exception) {
        logger.warn("Failed to get Git commit hash: ${e.message}. Using fallback.")
        return "unknown"
    } finally {
        grgit?.close()
    }
}

// Get commit count since last commit for versionCode increment
fun Project.getCommitCountSinceLastCommit(): Int {
    var grgit: Grgit? = null
    try {
        grgit = Grgit.open(mapOf("currentDir" to projectDir))
        val headCommit = grgit.head()
        val log = grgit.log(mapOf(
            "includes" to listOf(headCommit.id)
        ))
        return log.size
    } catch (e: Exception) {
        logger.warn("Failed to get commit count: ${e.message}. Using fallback.")
        return 0
    } finally {
        grgit?.close()
    }
}

// Get versionCode increment for nightly/pre-release
fun Project.getVersionCodeIncrement(): Int {
    val isNightlyBuild = gradle.startParameter.taskNames.any { it.lowercase().contains("nightly") }
    val isPreReleaseBuild = gradle.startParameter.taskNames.any { it.lowercase().contains("prerelease") }
    if (!isNightlyBuild && !isPreReleaseBuild) return 0

    return System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
        ?: System.getenv("CI_BUILD_NUMBER")?.toIntOrNull()
        ?: getCommitCountSinceLastCommit()
}

// Compute versionName dynamic bumping for nightly/pre-release
fun Project.computeVersionName(): String {
    val isNightlyBuild = isNightlyBuild()
    val isPreReleaseBuild = isPrereleaseBuild()

    // Static version from Constants.kt
    val baseVersion = Semver.parse(Constants.VERSION_NAME) ?: Semver.of(0, 0, 0)

    return when {
        isNightlyBuild -> {
            // Bump patch for nightly
            val nightlyVersion = Semver.of(
                baseVersion.major,
                baseVersion.minor,
                baseVersion.patch + 1
            )
            "${nightlyVersion}-nightly+git.${getGitCommitHash()}"
        }
        isPreReleaseBuild -> {
            // Bump minor for pre-release
            val preReleaseVersion = Semver.of(
                baseVersion.major,
                baseVersion.minor,
                0 + 1,
            )
            "${preReleaseVersion}-beta+git.${getGitCommitHash()}"
        }
        else -> Constants.VERSION_NAME
    }
}

fun Project.isNightlyBuild(): Boolean {
    return gradle.startParameter.taskNames.any { it.lowercase().contains(Constants.NIGHTLY) }
}

fun Project.isPrereleaseBuild(): Boolean {
    return gradle.startParameter.taskNames.any { it.lowercase().contains(Constants.PRERELEASE) }
}

// Compute versionCode (static baseline, dynamic bumping for nightly/pre-release)
fun Project.computeVersionCode(): Int {
    val isNightlyBuild = isNightlyBuild()
    val isPreReleaseBuild = isPrereleaseBuild()
    var versionCode = Constants.VERSION_CODE

    if (isPreReleaseBuild) {
        versionCode += 100 // Minor bump
    }
    if (isNightlyBuild) {
        versionCode += 1 // Patch bump
    }

    return versionCode + getVersionCodeIncrement()
}