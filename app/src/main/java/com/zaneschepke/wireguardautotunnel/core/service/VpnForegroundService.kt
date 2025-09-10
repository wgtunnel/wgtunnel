package com.zaneschepke.wireguardautotunnel.core.service

import com.zaneschepke.wireguardautotunnel.util.Constants
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VpnForegroundService(override val fgsType: Int = Constants.SYSTEM_EXEMPT_SERVICE_TYPE_ID) :
    BaseTunnelForegroundService()
