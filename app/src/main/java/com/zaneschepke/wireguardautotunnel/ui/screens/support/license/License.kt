import kotlinx.serialization.Serializable

@Serializable
data class LicenseFileEntry(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val name: String? = null,
    val spdxLicenses: List<SpdxLicense> = emptyList(),
    val scm: Scm? = null,
)

@Serializable data class SpdxLicense(val identifier: String, val name: String, val url: String)

@Serializable data class Scm(val url: String)
