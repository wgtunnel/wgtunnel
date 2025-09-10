package com.zaneschepke.wireguardautotunnel.core.service

import android.os.Binder

class LocalBinder(val service: TunnelService) : Binder()
