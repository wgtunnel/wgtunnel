package com.zaneschepke.wireguardautotunnel.util.extensions

import android.content.pm.PackageInfo
import com.zaneschepke.wireguardautotunnel.data.entity.TunnelConfig
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import java.math.BigDecimal
import java.text.DecimalFormat

fun BigDecimal.toThreeDecimalPlaceString(): String {
    val df = DecimalFormat("#.###")
    return df.format(this)
}

fun <T> List<T>.update(index: Int, item: T): List<T> = toMutableList().apply { this[index] = item }

fun <T> List<T>.removeAt(index: Int): List<T> = toMutableList().apply { this.removeAt(index) }

typealias Tunnels = List<TunnelConf>

typealias TunnelConfigs = List<TunnelConfig>

typealias Packages = List<PackageInfo>

fun <T> MutableList<T>.addAllUnique(elements: Collection<T>, comparator: (T, T) -> Boolean) {
    addAll(elements.filterNot { new -> this.any { existing -> comparator(existing, new) } })
}

fun <T, R : Comparable<R>> List<T>.isSortedBy(selector: (T) -> R): Boolean {
    return zipWithNext().all { (a, b) -> selector(a) <= selector(b) }
}
