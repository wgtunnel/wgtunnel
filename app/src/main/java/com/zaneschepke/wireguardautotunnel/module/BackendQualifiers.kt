package com.zaneschepke.wireguardautotunnel.module

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TunnelShell

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppShell
