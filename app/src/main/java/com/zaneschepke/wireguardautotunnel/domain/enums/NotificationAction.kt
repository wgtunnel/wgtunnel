package com.zaneschepke.wireguardautotunnel.domain.enums

import android.content.Context
import com.zaneschepke.wireguardautotunnel.R

enum class NotificationAction {
	TUNNEL_OFF,
	AUTO_TUNNEL_OFF,
	;

	fun title(context: Context): String {
		return when (this) {
			TUNNEL_OFF -> context.getString(R.string.stop)
			AUTO_TUNNEL_OFF -> context.getString(R.string.stop)
		}
	}
}
