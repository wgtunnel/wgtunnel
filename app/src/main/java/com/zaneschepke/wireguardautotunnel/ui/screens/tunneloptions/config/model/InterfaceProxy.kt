package com.zaneschepke.wireguardautotunnel.ui.screens.tunneloptions.config.model

import com.wireguard.config.Interface

data class InterfaceProxy(
	val privateKey: String = "",
	val publicKey: String = "",
	val addresses: String = "",
	val dnsServers: String = "",
	val listenPort: String = "",
	val mtu: String = "",
	val includedApplications: Set<String> = emptySet(),
	val excludedApplication: Set<String> = emptySet(),
	val junkPacketCount: String = "",
	val junkPacketMinSize: String = "",
	val junkPacketMaxSize: String = "",
	val initPacketJunkSize: String = "",
	val responsePacketJunkSize: String = "",
	val initPacketMagicHeader: String = "",
	val responsePacketMagicHeader: String = "",
	val underloadPacketMagicHeader: String = "",
	val transportPacketMagicHeader: String = "",
) {

	fun toWgInterface(): Interface {
		return Interface.Builder().apply {
			parsePrivateKey(privateKey)
			parseAddresses(addresses)
			parseDnsServers(dnsServers)
			if (mtu.isNotBlank()) parseMtu(mtu)
			if (listenPort.isNotBlank()) parseListenPort(listenPort)
			includeApplications(includedApplications)
			excludeApplications(excludedApplication)
		}.build()
	}

	fun toAmInterface(): org.amnezia.awg.config.Interface {
		return org.amnezia.awg.config.Interface.Builder().apply {
			parsePrivateKey(privateKey)
			parseAddresses(addresses)
			parseDnsServers(dnsServers)
			if (mtu.isNotBlank()) parseMtu(mtu)
			if (listenPort.isNotBlank()) parseListenPort(listenPort)
			includeApplications(includedApplications)
			excludeApplications(excludedApplication)
			if (junkPacketCount.isNotBlank()) parseJunkPacketCount(junkPacketCount)
			if (junkPacketMinSize.isNotBlank()) parseJunkPacketMinSize(junkPacketMinSize)
			if (junkPacketMaxSize.isNotBlank()) parseJunkPacketMaxSize(junkPacketMaxSize)
			if (initPacketJunkSize.isNotBlank()) parseInitPacketJunkSize(initPacketJunkSize)
			if (responsePacketJunkSize.isNotBlank()) parseResponsePacketJunkSize(responsePacketJunkSize)
			if (initPacketMagicHeader.isNotBlank()) parseInitPacketMagicHeader(initPacketMagicHeader)
			if (responsePacketMagicHeader.isNotBlank()) parseResponsePacketMagicHeader(responsePacketMagicHeader)
			if (underloadPacketMagicHeader.isNotBlank()) parseUnderloadPacketMagicHeader(underloadPacketMagicHeader)
			if (transportPacketMagicHeader.isNotBlank()) parseTransportPacketMagicHeader(transportPacketMagicHeader)
		}.build()
	}

	companion object {
		fun from(i: Interface): InterfaceProxy {
			return InterfaceProxy(
				publicKey = i.keyPair.publicKey.toBase64().trim(),
				privateKey = i.keyPair.privateKey.toBase64().trim(),
				addresses = i.addresses.joinToString(", ").trim(),
				dnsServers = listOf(
					i.dnsServers.joinToString(", ").replace("/", "").trim(),
					i.dnsSearchDomains.joinToString(", ").trim(),
				).filter { it.isNotEmpty() }.joinToString(", "),
				listenPort =
				if (i.listenPort.isPresent) {
					i.listenPort.get().toString().trim()
				} else {
					""
				},
				mtu = if (i.mtu.isPresent) i.mtu.get().toString().trim() else "",
				includedApplications = i.includedApplications,
				excludedApplication = i.excludedApplications,
			)
		}

		fun from(i: org.amnezia.awg.config.Interface): InterfaceProxy {
			return InterfaceProxy(
				publicKey = i.keyPair.publicKey.toBase64().trim(),
				privateKey = i.keyPair.privateKey.toBase64().trim(),
				addresses = i.addresses.joinToString(", ").trim(),
				dnsServers = (i.dnsServers + i.dnsSearchDomains).joinToString(", ").replace("/", "").trim(),
				listenPort =
				if (i.listenPort.isPresent) {
					i.listenPort.get().toString().trim()
				} else {
					""
				},
				mtu = if (i.mtu.isPresent) i.mtu.get().toString().trim() else "",
				includedApplications = i.includedApplications,
				excludedApplication = i.excludedApplications,
				junkPacketCount =
				if (i.junkPacketCount.isPresent) {
					i.junkPacketCount.get()
						.toString()
				} else {
					""
				},
				junkPacketMinSize =
				if (i.junkPacketMinSize.isPresent) {
					i.junkPacketMinSize.get()
						.toString()
				} else {
					""
				},
				junkPacketMaxSize =
				if (i.junkPacketMaxSize.isPresent) {
					i.junkPacketMaxSize.get()
						.toString()
				} else {
					""
				},
				initPacketJunkSize =
				if (i.initPacketJunkSize.isPresent) {
					i.initPacketJunkSize.get()
						.toString()
				} else {
					""
				},
				responsePacketJunkSize =
				if (i.responsePacketJunkSize.isPresent) {
					i.responsePacketJunkSize.get()
						.toString()
				} else {
					""
				},
				initPacketMagicHeader =
				if (i.initPacketMagicHeader.isPresent) {
					i.initPacketMagicHeader.get()
						.toString()
				} else {
					""
				},
				responsePacketMagicHeader =
				if (i.responsePacketMagicHeader.isPresent) {
					i.responsePacketMagicHeader.get()
						.toString()
				} else {
					""
				},
				transportPacketMagicHeader =
				if (i.transportPacketMagicHeader.isPresent) {
					i.transportPacketMagicHeader.get()
						.toString()
				} else {
					""
				},
				underloadPacketMagicHeader =
				if (i.underloadPacketMagicHeader.isPresent) {
					i.underloadPacketMagicHeader.get()
						.toString()
				} else {
					""
				},
			)
		}
	}
}
