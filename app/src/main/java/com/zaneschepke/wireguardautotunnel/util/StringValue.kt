package com.zaneschepke.wireguardautotunnel.util

import android.content.Context
import androidx.annotation.StringRes

sealed class StringValue {
    data class DynamicString(val value: String) : StringValue()

    data object Empty : StringValue()

    class StringResource(@StringRes val resId: Int, vararg val args: Any) : StringValue()

    fun asString(context: Context?): String {
        return when (this) {
            is Empty -> ""
            is DynamicString -> value
            is StringResource -> {
                val stringArgs =
                    args
                        .map { arg ->
                            when (arg) {
                                is Int -> context?.getString(arg) ?: arg.toString()
                                else -> arg
                            }
                        }
                        .toTypedArray()
                context?.getString(resId, *stringArgs).orEmpty()
            }
        }
    }
}
