package com.zaneschepke.wireguardautotunnel.viewmodel.event

sealed class UiEvent {
    data object SortTunnels : UiEvent()
}
