package com.the412banner.aiwitviewer.data

import android.content.Context

/**
 * Local-only renames for cameras. Keyed by `device_sn`. The cloud copy
 * (set by the vendor app) is untouched.
 */
class LocalAliasStore(context: Context) {
    private val prefs = context.getSharedPreferences("aiwit_aliases", Context.MODE_PRIVATE)

    fun aliasFor(deviceSn: String): String? = prefs.getString(deviceSn, null)?.takeIf { it.isNotBlank() }

    fun setAlias(deviceSn: String, alias: String?) {
        val editor = prefs.edit()
        if (alias.isNullOrBlank()) editor.remove(deviceSn) else editor.putString(deviceSn, alias.trim())
        editor.apply()
    }

    /** Resolved display name: alias if set, else the vendor's cloud name, else the SN. */
    fun displayName(device: Device): String =
        aliasFor(device.device_sn) ?: device.name.ifBlank { device.device_sn }
}
