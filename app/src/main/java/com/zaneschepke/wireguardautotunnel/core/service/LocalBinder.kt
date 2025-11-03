package com.zaneschepke.wireguardautotunnel.core.service

import android.os.Binder
import java.lang.ref.WeakReference

class LocalBinder(service: TunnelService) : Binder() {
    private val serviceRef = WeakReference(service)

    val service: TunnelService?
        get() = serviceRef.get()
}
