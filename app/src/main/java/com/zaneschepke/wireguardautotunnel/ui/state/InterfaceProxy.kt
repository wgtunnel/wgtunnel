package com.zaneschepke.wireguardautotunnel.ui.state

import com.wireguard.config.Interface
import com.zaneschepke.wireguardautotunnel.util.extensions.ifNotBlank
import com.zaneschepke.wireguardautotunnel.util.extensions.joinAndTrim
import com.zaneschepke.wireguardautotunnel.util.extensions.toTrimmedList
import java.util.*

data class InterfaceProxy(
    val privateKey: String = "",
    val publicKey: String = "",
    val addresses: String = "",
    val dnsServers: String = "",
    val listenPort: String = "",
    val mtu: String = "",
    val includedApplications: Set<String> = emptySet(),
    val excludedApplications: Set<String> = emptySet(),
    val junkPacketCount: String = "",
    val junkPacketMinSize: String = "",
    val junkPacketMaxSize: String = "",
    val initPacketJunkSize: String = "",
    val responsePacketJunkSize: String = "",
    val initPacketMagicHeader: String = "",
    val responsePacketMagicHeader: String = "",
    val underloadPacketMagicHeader: String = "",
    val transportPacketMagicHeader: String = "",
    val i1: String = "",
    val i2: String = "",
    val i3: String = "",
    val i4: String = "",
    val i5: String = "",
    val j1: String = "",
    val j2: String = "",
    val j3: String = "",
    val itime: String = "",
    val preUp: String = "",
    val postUp: String = "",
    val preDown: String = "",
    val postDown: String = "",
) {

    fun toWgInterface(): Interface {
        return Interface.Builder()
            .apply {
                parseAddresses(addresses)
                parsePrivateKey(privateKey)
                dnsServers.ifNotBlank { parseDnsServers(it) }
                listenPort.ifNotBlank { parseListenPort(it) }
                mtu.ifNotBlank { parseMtu(it) }
                includeApplications(includedApplications)
                excludeApplications(excludedApplications)
                preUp.toTrimmedList().forEach { parsePreUp(it) }
                postUp.toTrimmedList().forEach { parsePostUp(it) }
                preDown.toTrimmedList().forEach { parsePreDown(it) }
                postDown.toTrimmedList().forEach { parsePostDown(it) }
            }
            .build()
    }

    fun isAmneziaEnabled(): Boolean {
        return listOf(
                junkPacketCount,
                junkPacketMinSize,
                junkPacketMaxSize,
                initPacketJunkSize,
                responsePacketJunkSize,
                initPacketMagicHeader,
                responsePacketMagicHeader,
                underloadPacketMagicHeader,
                transportPacketMagicHeader,
                i1,
                i2,
                i3,
                i4,
                i5,
                j1,
                j2,
                j3,
                itime,
            )
            .any { it.isNotBlank() }
    }

    fun toAmneziaCompatibilityConfig(): InterfaceProxy {
        return copy(
            junkPacketCount = "4",
            junkPacketMinSize = "40",
            junkPacketMaxSize = "70",
            initPacketJunkSize = "0",
            responsePacketJunkSize = "0",
            initPacketMagicHeader = "1",
            responsePacketMagicHeader = "2",
            underloadPacketMagicHeader = "3",
            transportPacketMagicHeader = "4",
            i1 = "",
            i2 = "",
            i3 = "",
            i4 = "",
            i5 = "",
            j1 = "",
            j2 = "",
            j3 = "",
            itime = "",
        )
    }

    fun resetAmneziaProperties(): InterfaceProxy {
        return copy(
            junkPacketCount = "",
            junkPacketMinSize = "",
            junkPacketMaxSize = "",
            initPacketJunkSize = "",
            responsePacketJunkSize = "",
            initPacketMagicHeader = "",
            responsePacketMagicHeader = "",
            underloadPacketMagicHeader = "",
            transportPacketMagicHeader = "",
            i1 = "",
            i2 = "",
            i3 = "",
            i4 = "",
            i5 = "",
            j1 = "",
            j2 = "",
            j3 = "",
            itime = "",
        )
    }

    // TODO fix this later when we get amnezia to properly return 0
    fun isAmneziaCompatibilityModeSet(): Boolean {
        return (initPacketJunkSize.toIntOrNull() ?: 0) == 0 &&
            (responsePacketJunkSize.toIntOrNull() ?: 0) == 0 &&
            initPacketMagicHeader.toLongOrNull() == 1L &&
            responsePacketMagicHeader.toLongOrNull() == 2L &&
            underloadPacketMagicHeader.toLongOrNull() == 3L &&
            transportPacketMagicHeader.toLongOrNull() == 4L
    }

    fun isCompatibleWithStandardWireGuard(): Boolean {
        return isAmneziaCompatibilityModeSet()
    }

    fun toAmInterface(): org.amnezia.awg.config.Interface {
        return org.amnezia.awg.config.Interface.Builder()
            .apply {
                parseAddresses(addresses)
                parsePrivateKey(privateKey)
                dnsServers.ifNotBlank { parseDnsServers(it) }
                listenPort.ifNotBlank { parseListenPort(it) }
                mtu.ifNotBlank { parseMtu(it) }
                includeApplications(includedApplications)
                excludeApplications(excludedApplications)
                preUp.toTrimmedList().forEach { parsePreUp(it) }
                postUp.toTrimmedList().forEach { parsePostUp(it) }
                preDown.toTrimmedList().forEach { parsePreDown(it) }
                postDown.toTrimmedList().forEach { parsePostDown(it) }
                junkPacketCount.ifNotBlank { parseJunkPacketCount(it) }
                junkPacketMinSize.ifNotBlank { parseJunkPacketMinSize(it) }
                junkPacketMaxSize.ifNotBlank { parseJunkPacketMaxSize(it) }
                initPacketJunkSize.ifNotBlank { parseInitPacketJunkSize(it) }
                responsePacketJunkSize.ifNotBlank { parseResponsePacketJunkSize(it) }
                initPacketMagicHeader.ifNotBlank { parseInitPacketMagicHeader(it) }
                responsePacketMagicHeader.ifNotBlank { parseResponsePacketMagicHeader(it) }
                underloadPacketMagicHeader.ifNotBlank { parseUnderloadPacketMagicHeader(it) }
                transportPacketMagicHeader.ifNotBlank { parseTransportPacketMagicHeader(it) }
                i1.ifNotBlank { parseI1(it) }
                i2.ifNotBlank { parseI2(it) }
                i3.ifNotBlank { parseI3(it) }
                i4.ifNotBlank { parseI4(it) }
                i5.ifNotBlank { parseI5(it) }
                j1.ifNotBlank { parseJ1(it) }
                j2.ifNotBlank { parseJ2(it) }
                j3.ifNotBlank { parseJ3(it) }
                itime.ifNotBlank { parseItime(it) }
            }
            .build()
    }

    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()

        if (privateKey.isBlank()) {
            errors.add("Private key is required")
        } else if (!isValidBase64(privateKey) || privateKey.length != 44) {
            errors.add("Invalid private key format (must be 44-character Base64)")
        }

        // Addresses validation (basic)
        if (addresses.isBlank()) {
            errors.add("Addresses are required")
        } // More detailed CIDR validation can be added if needed

        listenPort.ifNotBlank {
            val port = it.toIntOrNull()
            if (port == null) errors.add("Listen port must be an integer")
            else if (port !in 1..65535) errors.add("Listen port must be between 1 and 65535")
        }

        mtu.ifNotBlank {
            val mtuValue = it.toIntOrNull()
            if (mtuValue == null) errors.add("MTU must be an integer")
            else if (mtuValue !in 576..9200) errors.add("MTU should be between 576 and 9200")
        }

        junkPacketCount.ifNotBlank {
            val count = it.toIntOrNull()
            if (count == null) errors.add("Junk packet count must be an integer")
            else if (count !in 1..128) errors.add("Junk packet count must be between 0 and 128")
        }

        junkPacketMinSize.ifNotBlank {
            val min = it.toIntOrNull()
            if (min == null) errors.add("Junk packet min size must be an integer")
            else if (min !in 1..1279) errors.add("Junk packet min size must be between 1 and 1279")
        }

        junkPacketMaxSize.ifNotBlank {
            val max = it.toIntOrNull()
            if (max == null) errors.add("Junk packet max size must be an integer")
            else if (max !in 2..1280) errors.add("Junk packet max size must be between 2 and 1280")
        }

        if (junkPacketMinSize.isNotBlank() && junkPacketMaxSize.isNotBlank()) {
            val min = junkPacketMinSize.toIntOrNull() ?: 0
            val max = junkPacketMaxSize.toIntOrNull() ?: 0
            if (min >= max) errors.add("Junk packet min size must be less than max size")
        }

        initPacketJunkSize.ifNotBlank {
            val size = it.toIntOrNull()
            if (size == null) errors.add("Init packet junk size must be an integer")
            else if (size !in 0..64) errors.add("Init packet junk size must be between 0 and 64")
        }

        responsePacketJunkSize.ifNotBlank {
            val size = it.toIntOrNull()
            if (size == null) errors.add("Response packet junk size must be an integer")
            else if (size !in 0..64)
                errors.add("Response packet junk size must be between 0 and 64")
        }

        val h1 = initPacketMagicHeader.toIntOrNull()
        val h2 = responsePacketMagicHeader.toIntOrNull()
        val h3 = underloadPacketMagicHeader.toIntOrNull()
        val h4 = transportPacketMagicHeader.toIntOrNull()
        val headers = listOf(h1, h2, h3, h4)
        if (headers.any { it != null }) {
            if (headers.any { it == null })
                errors.add("All magic headers must be set if any is set")
            else {
                val hs = headers.filterNotNull()
                if (hs.any { it !in 1..4 }) errors.add("Magic headers must be between 1 and 4")
                if (hs.distinct().size != 4)
                    errors.add("Magic headers must be a unique permutation of 1-4")
            }
        }

        fun validateHexBlob(field: String?, name: String) {
            field?.let {
                if (!it.matches(Regex("^<b 0x[0-9a-fA-F]+>$"))) {
                    errors.add("$name must be in format <b 0xHEXSTRING>")
                }
            }
        }

        validateHexBlob(i1, "i1")
        validateHexBlob(i2, "i2")
        validateHexBlob(i3, "i3")
        validateHexBlob(i4, "i4")
        validateHexBlob(i5, "i5")
        validateHexBlob(j1, "j1")
        validateHexBlob(j2, "j2")
        validateHexBlob(j3, "j3")

        itime.ifNotBlank {
            val time = it.toIntOrNull()
            if (time == null) errors.add("itime must be an integer")
            else if (time < 0) errors.add("itime must be non-negative")
        }

        return errors
    }

    /**
     * Mimics QUIC (HTTP/3) traffic by setting i1 to a QUIC initial packet and i2 to a follow-up
     * frame. Adds j1 for extra obfuscation. Compatible with standard WireGuard servers.
     */
    fun setQuicMimic(): InterfaceProxy {
        return copy(
            i1 =
                "<b 0xc1ff000012508394c8f03e51570800449f0dbc195a0000f3a694c75775b4e546172ce9e047cd0b5bee5181648c727adc87f7eae54473ec6cba6bdad4f59823174b769f12358abd292d4f3286934484fb8b239c38732e1f3bbbc6a003056487eb8b5c88b9fd9279ffff3b0f4ecf95c4624db6d65d4113329ee9b0bf8cdd7c8a8d72806d55df25ecb66488bc119d7c9a29abaf99bb33c56b08ad8c26995f838bb3b7a3d5c1858b8ec06b839db2dcf918d5ea9317f1acd6b663cc8925868e2f6a1bda546695f3c3f33175944db4a11a346afb07e78489e509b02add51b7b203eda5c330b03641179a31fbba9b56ce00f3d5b5e3d7d9c5429aebb9576f2f7eacbe27bc1b8082aaf68fb69c921aa5d33ec0c8510410865a178d86d7e54122d55ef2c2bbc040be46d7fece73fe8a1b24495ec160df2da9b20a7ba2f26dfa2a44366dbc63de5cd7d7c94c57172fe6d79c901f025c0010b02c89b395402c009f62dc053b8067a1e0ed0a1e0cf5087d7f78cbd94afe0c3dd55d2d4b1a5cfe2b68b86264e351d1dcd858783a240f893f008ceed743d969b8f735a1677ead960b1fb1ecc5ac83c273b49288d02d7286207e663c45e1a7baf50640c91e762941cf380ce8d79f3e86767fbbcd25b42ef70ec334835a3a6d792e170a432ce0cb7bde9aaa1e75637c1c34ae5fef4338f53db8b13a4d2df594efbfa08784543815c9c0d487bddfa1539bc252cf43ec3686e9802d651cfd2a829a06a9f332a733a4a8aed80efe3478093fbc69c8608146b3f16f1a5c4eac9320da49f1afa5f538ddecbbe7888f435512d0dd74fd9b8c99e3145ba84410d8ca9a36dd884109e76e5fb8222a52e1473da168519ce7a8a3c32e9149671b16724c6c5c51bb5cd64fb591e567fb78b10f9f6fee62c276f282a7df6bcf7c17747bc9a81e6c9c3b032fdd0e1c3ac9eaa5077de3ded18b2ed4faf328f49875af2e36ad5ce5f6cc99ef4b60e57b3b5b9c9fcbcd4cfb3975e70ce4c2506bcd71fef0e53592461504e3d42c885caab21b782e26294c6a9d61118cc40a26f378441ceb48f31a362bf8502a723a36c63502229a462cc2a3796279a5e3a7f81a68c7f81312c381cc16a4ab03513a51ad5b54306ec1d78a5e47e2b15e5b7a1438e5b8b2882dbdad13d6a4a8c3558cae043501b68eb3b040067152>",
            i2 = "<b 0x0000000000010000000000000000000000000000000000000000000000000000>",
            j1 = "<b 0x1234567890abcdef>",
            itime = "120",
        )
    }

    /**
     * Mimics DNS query traffic with a single i1 packet. DNS is typically single-packet, so no
     * additional i/j packets are set. Compatible with standard WireGuard servers.
     */
    fun setDnsMimic(): InterfaceProxy {
        return copy(
            i1 = "<b 0x123401000001000000000000076578616d706c6503636f6d0000010001>",
            i2 = "",
            j1 = "",
            itime = "120",
        )
    }

    /**
     * Mimics SIP (VoIP) traffic with i1 as an INVITE packet and i2 as a follow-up (e.g., TRYING
     * response). Adds j1 for extra obfuscation. Compatible with standard WireGuard servers.
     */
    fun setSipMimic(): InterfaceProxy {
        return copy(
            i1 =
                "<b 0x494e56495445207369703a626f624062696c6f78692e636f6d205349502f322e300d0a5669613a205349502f322e302f55445020706333332e61746c616e74612e636f6d3b6272616e63683d7a39684734624b3737366173646864730d0a4d61782d466f7277617264733a2037300d0a546f3a20426f62203c7369703a626f624062696c6f78692e636f6d3e0d0a46726f6d3a20416c696365203c7369703a616c6963654061746c616e74612e636f6d3e3b7461673d313932383330313737340d0a43616c6c2d49443a20613834623463373665363637313040706333332e61746c616e74612e636f6d0d0a435365713a2033313431353920494e564954450d0a436f6e746163743a203c7369703a616c69636540706333332e61746c616e74612e636f6d3e0d0a436f6e74656e742d547970653a206170706c69636174696f6e2f7364700d0a436f6e74656e742d4c656e6774683a20300d0a0d0a>",
            i2 =
                "<b 0x5349502f322e302031303020547279696e670d0a5669613a205349502f322e302f55445020706333332e61746c616e74612e636f6d3b6272616e63683d7a39684734624b3737366173646864730d0a546f3a20426f62203c7369703a626f624062696c6f78692e636f6d3e0d0a46726f6d3a20416c696365203c7369703a616c6963654061746c616e74612e636f6d3e3b7461673d313932383330313737340d0a43616c6c2d49443a20613834623463373665363637313040706333332e61746c616e74612e636f6d0d0a435365713a2033313431353920494e564954450d0a436f6e74656e742d4c656e6774683a20300d0a0d0a>",
            j1 = "<b 0xabcdef1234567890>",
            itime = "120",
        )
    }

    private fun isValidBase64(str: String): Boolean {
        return try {
            Base64.getDecoder().decode(str)
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        fun from(i: Interface): InterfaceProxy {
            val dnsString =
                listOf(
                        i.dnsServers.joinToString(", ").replace("/", "").trim(),
                        i.dnsSearchDomains.joinAndTrim(),
                    )
                    .filter { it.isNotEmpty() }
                    .joinToString(", ")
                    .takeIf { it.isNotBlank() }
            return InterfaceProxy(
                publicKey = i.keyPair.publicKey.toBase64().trim(),
                privateKey = i.keyPair.privateKey.toBase64().trim(),
                addresses = i.addresses.joinToString(", ").trim(),
                dnsServers = dnsString ?: "",
                listenPort =
                    if (i.listenPort.isPresent) i.listenPort.get().toString().trim() else "",
                mtu = if (i.mtu.isPresent) i.mtu.get().toString().trim() else "",
                includedApplications = i.includedApplications.toMutableSet(),
                excludedApplications = i.excludedApplications.toMutableSet(),
                preUp = i.preUp.joinAndTrim(),
                postUp = i.postUp.joinAndTrim(),
                preDown = i.preDown.joinAndTrim(),
                postDown = i.postDown.joinAndTrim(),
            )
        }

        fun from(i: org.amnezia.awg.config.Interface): InterfaceProxy {
            val dnsString =
                (i.dnsServers + i.dnsSearchDomains)
                    .joinToString(", ")
                    .replace("/", "")
                    .trim()
                    .takeIf { it.isNotBlank() }
            return InterfaceProxy(
                publicKey = i.keyPair.publicKey.toBase64().trim(),
                privateKey = i.keyPair.privateKey.toBase64().trim(),
                addresses = i.addresses.joinToString(", ").trim(),
                dnsServers = dnsString ?: "",
                listenPort =
                    if (i.listenPort.isPresent) i.listenPort.get().toString().trim() else "",
                mtu = if (i.mtu.isPresent) i.mtu.get().toString().trim() else "",
                includedApplications = i.includedApplications.toMutableSet(),
                excludedApplications = i.excludedApplications.toMutableSet(),
                preUp = i.preUp.joinAndTrim(),
                postUp = i.postUp.joinAndTrim(),
                preDown = i.preDown.joinAndTrim(),
                postDown = i.postDown.joinAndTrim(),
                junkPacketCount =
                    if (i.junkPacketCount.isPresent) i.junkPacketCount.get().toString() else "",
                junkPacketMinSize =
                    if (i.junkPacketMinSize.isPresent) i.junkPacketMinSize.get().toString() else "",
                junkPacketMaxSize =
                    if (i.junkPacketMaxSize.isPresent) i.junkPacketMaxSize.get().toString() else "",
                initPacketJunkSize =
                    if (i.initPacketJunkSize.isPresent) i.initPacketJunkSize.get().toString()
                    else "",
                responsePacketJunkSize =
                    if (i.responsePacketJunkSize.isPresent)
                        i.responsePacketJunkSize.get().toString()
                    else "",
                initPacketMagicHeader =
                    if (i.initPacketMagicHeader.isPresent) i.initPacketMagicHeader.get().toString()
                    else "",
                responsePacketMagicHeader =
                    if (i.responsePacketMagicHeader.isPresent)
                        i.responsePacketMagicHeader.get().toString()
                    else "",
                underloadPacketMagicHeader =
                    if (i.underloadPacketMagicHeader.isPresent)
                        i.underloadPacketMagicHeader.get().toString()
                    else "",
                transportPacketMagicHeader =
                    if (i.transportPacketMagicHeader.isPresent)
                        i.transportPacketMagicHeader.get().toString()
                    else "",
                i1 = if (i.i1.isPresent) i.i1.get() else "",
                i2 = if (i.i2.isPresent) i.i2.get() else "",
                i3 = if (i.i3.isPresent) i.i3.get() else "",
                i4 = if (i.i4.isPresent) i.i4.get() else "",
                i5 = if (i.i5.isPresent) i.i5.get() else "",
                j1 = if (i.j1.isPresent) i.j1.get() else "",
                j2 = if (i.j2.isPresent) i.j2.get() else "",
                j3 = if (i.j3.isPresent) i.j3.get() else "",
                itime = if (i.itime.isPresent) i.itime.get().toString() else "",
            )
        }
    }
}
