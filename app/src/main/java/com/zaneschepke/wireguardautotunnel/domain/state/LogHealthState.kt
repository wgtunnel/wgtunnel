package com.zaneschepke.wireguardautotunnel.domain.state

data class LogHealthState(val isHealthy: Boolean, val timestamp: Long = System.currentTimeMillis())
