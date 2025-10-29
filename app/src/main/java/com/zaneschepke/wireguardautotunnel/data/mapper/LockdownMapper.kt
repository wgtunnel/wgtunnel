package com.zaneschepke.wireguardautotunnel.data.mapper

import com.zaneschepke.wireguardautotunnel.data.entity.LockdownSettings as Entity
import com.zaneschepke.wireguardautotunnel.domain.model.LockdownSettings as Domain

fun Entity.toDomain(): Domain =
    Domain(id = id, bypassLan = bypassLan, metered = metered, dualStack = dualStack)

fun Domain.toEntity(): Entity =
    Entity(id = id, bypassLan = bypassLan, metered = metered, dualStack = dualStack)
