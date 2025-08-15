package com.zaneschepke.wireguardautotunnel.domain.enums

enum class BackendMode(val value: Int) {
    USERSPACE(0),
    PROXIED_USERSPACE(1),
    KERNEL(2);

    companion object {
        fun fromValue(value: Int): BackendMode  =
            entries.find { it.value == value } ?: USERSPACE
    }
}