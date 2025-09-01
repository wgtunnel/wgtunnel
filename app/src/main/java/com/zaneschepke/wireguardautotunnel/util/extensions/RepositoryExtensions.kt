package com.zaneschepke.wireguardautotunnel.util.extensions

import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository

suspend fun TunnelRepository.saveTunnelsUniquely(
    tunnels: List<TunnelConf>,
    existing: List<TunnelConf>,
) {
    val uniqueTunnels = generateUniquelyNamedConfigs(tunnels, existing)
    saveAll(uniqueTunnels)
}

private fun generateUniquelyNamedConfigs(
    incoming: List<TunnelConf>,
    existing: List<TunnelConf>,
): List<TunnelConf> {
    val usedNames = existing.map { it.tunName }.toMutableSet()
    val result = mutableListOf<TunnelConf>()
    val regex = Regex("(.+)\\s*\\((\\d+)\\)$")

    for (tun in incoming) {
        var baseName = tun.tunName
        var uniqueName = tun.tunName
        var counter = 1

        val matchResult = regex.find(baseName)
        if (matchResult != null) {
            baseName = matchResult.groupValues[1].trimEnd()
            counter = matchResult.groupValues[2].toIntOrNull()?.plus(1) ?: 1
            uniqueName = "$baseName ($counter)"
        }

        while (uniqueName in usedNames) {
            uniqueName = "$baseName ($counter)"
            counter++
        }

        usedNames.add(uniqueName)
        result.add(tun.copy(tunName = uniqueName))
    }
    return result
}
