package com.zaneschepke.wireguardautotunnel.util

import com.vdurmont.semver4j.Semver
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import kotlin.math.pow
import timber.log.Timber

object NumberUtils {
    private const val BYTES_IN_KB = 1024.0
    private val BYTES_IN_MB = BYTES_IN_KB.pow(2.0)
    private val keyValidationRegex = """^[A-Za-z0-9+/]{42}[AEIMQUYcgkosw480]=${'$'}""".toRegex()

    fun bytesToMB(bytes: Long): BigDecimal {
        return bytes.toBigDecimal().divide(BYTES_IN_MB.toBigDecimal())
    }

    fun isValidKey(key: String): Boolean {
        return key.matches(keyValidationRegex)
    }

    fun generateRandomTunnelName(): String {
        return "tunnel${randomFive()}"
    }

    private fun randomFive(): Int {
        return (Math.random() * 100000).toInt()
    }

    fun getSecondsBetween(start: Long, end: Long): Long? {
        return if (start != 0L && end != 0L) {
            val startInstant = Instant.ofEpochMilli(start)
            val endInstant = Instant.ofEpochMilli(end)
            return Duration.between(startInstant, endInstant).seconds
        } else {
            null
        }
    }

    fun compareVersions(newVersion: String, currentVersion: String): Int {
        try {
            val newSemver = Semver(newVersion, Semver.SemverType.LOOSE)
            val currentSemver = Semver(currentVersion, Semver.SemverType.LOOSE)
            return newSemver.compareTo(currentSemver)
        } catch (e: Exception) {
            Timber.e(e, "Failed to compare versions $newVersion and $currentVersion")
            return 0
        }
    }
}
