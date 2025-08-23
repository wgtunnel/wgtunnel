package com.zaneschepke.wireguardautotunnel.data.model

import android.content.Context
import com.zaneschepke.wireguardautotunnel.R

enum class DnsProtocol(val value: Int) {
    SYSTEM(0),
    DOH(1);

    fun asString(context: Context): String {
        return when (this) {
            SYSTEM -> context.getString(R.string.system)
            DOH -> context.getString(R.string.doh)
        }
    }

    companion object {
        fun fromValue(value: Int): DnsProtocol =
            DnsProtocol.entries.find { it.value == value } ?: SYSTEM
    }
}

data class DnsSettings(
    val protocol: DnsProtocol = DnsProtocol.SYSTEM,
    val endpoint: String? = null,
)

enum class DnsProvider(private val systemAddress: String, private val dohAddress: String) {
    CLOUDFLARE("1.1.1.1", "https://1.1.1.1/dns-query"),
    ADGUARD("94.140.14.14", "https://94.140.14.14/dns-query");

    fun asAddress(protocol: DnsProtocol): String {
        return when (protocol) {
            DnsProtocol.SYSTEM -> systemAddress
            DnsProtocol.DOH -> dohAddress
        }
    }

    companion object {
        fun fromAddress(address: String): DnsProvider {
            return entries.find { it.systemAddress == address || it.dohAddress == address }
                ?: CLOUDFLARE
        }
    }
}
