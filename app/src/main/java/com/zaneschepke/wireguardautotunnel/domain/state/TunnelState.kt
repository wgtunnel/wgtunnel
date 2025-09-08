package com.zaneschepke.wireguardautotunnel.domain.state

import com.zaneschepke.wireguardautotunnel.domain.enums.BackendMode
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import org.amnezia.awg.crypto.Key

data class TunnelState(
    val status: TunnelStatus = TunnelStatus.Down,
    val backendState: BackendMode = BackendMode.Inactive,
    val statistics: TunnelStatistics? = null,
    val pingStates: Map<Key, PingState>? = null,
    val logHealthState: LogHealthState? = null,
) {

    fun health(): Health {
        val now = System.currentTimeMillis()

        if (pingStates == null && logHealthState == null && statistics == null)
            return Health.UNKNOWN

        if (logHealthState?.isHealthy == false) return Health.UNHEALTHY

        val healthLogs =
            logHealthState?.isHealthy == true &&
                (now - logHealthState.timestamp) <= LOG_HEALTH_SUCCESS_TIMEOUT_MS

        if (pingStates?.any { !it.value.isReachable } == true) return Health.UNHEALTHY

        if (statistics != null) {
            if (statistics.isTunnelStale()) {
                return Health.STALE
            }
            if ((logHealthState == null || !healthLogs) && pingStates == null) {
                return Health.HEALTHY
            }
        } else {
            if (!healthLogs) {
                return Health.UNKNOWN
            }
        }

        if (healthLogs) {
            return Health.HEALTHY
        }

        return Health.UNKNOWN
    }

    enum class Health {
        UNKNOWN,
        UNHEALTHY,
        HEALTHY,
        STALE,
    }

    companion object {
        const val LOG_HEALTH_SUCCESS_TIMEOUT_MS = 2 * 60 * 1000L // 2 minutes
    }
}
