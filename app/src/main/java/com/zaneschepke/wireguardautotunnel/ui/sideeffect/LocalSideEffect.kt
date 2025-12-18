package com.zaneschepke.wireguardautotunnel.ui.sideeffect

sealed class LocalSideEffect {
    data object Sort : LocalSideEffect()

    data object SaveChanges : LocalSideEffect()

    sealed class Sheet : LocalSideEffect() {

        data object ImportTunnels : Sheet()

        data object ExportTunnels : Sheet()

        data object LoggerActions : Sheet()
    }

    sealed class Modal : LocalSideEffect() {
        data object QR : Modal()

        data object DeleteTunnels : Modal()

        data object SelectTunnel : Modal()
    }

    sealed class SelectedTunnels : LocalSideEffect() {
        data object SelectAll : SelectedTunnels()

        data object Copy : SelectedTunnels()
    }
}
