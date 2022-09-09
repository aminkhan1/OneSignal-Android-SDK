package com.onesignal.core.internal.influence

internal enum class InfluenceType {
    DIRECT,
    INDIRECT,
    UNATTRIBUTED,
    DISABLED
    ;

    fun isAttributed() = isDirect() || isIndirect()

    fun isDirect() = this == DIRECT

    fun isIndirect() = this == INDIRECT

    fun isUnattributed() = this == UNATTRIBUTED

    fun isDisabled() = this == DISABLED

    companion object {
        @JvmStatic
        fun fromString(value: String?) = value?.let {
            values().findLast { it.name.equals(value, ignoreCase = true) }
        } ?: UNATTRIBUTED
    }
}
