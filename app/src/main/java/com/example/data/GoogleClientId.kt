package com.example.data

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

internal fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

/** Resolve a real configured OAuth client ID without a compile-time dependency on generated resources. */
internal object GoogleClientId {
    private const val PROVISIONED_WEB_CLIENT_ID = "637011560599-sdht9lfjpl2058idv8usoovo1jpd40ar.apps.googleusercontent.com"

    fun optional(context: Context): String {
        val packages = listOf(context.packageName, "com.example").distinct()
        for (pkg in packages) {
            val id = context.resources.getIdentifier("default_web_client_id", "string", pkg)
            if (id != 0) {
                val value = runCatching { context.getString(id).trim() }.getOrDefault("")
                    .takeUnless { it.startsWith("YOUR_", ignoreCase = true) }
                    .orEmpty()
                if (value.isNotEmpty()) return value
            }
        }
        return PROVISIONED_WEB_CLIENT_ID
    }

    fun required(context: Context): String {
        return optional(context).ifBlank {
            throw IllegalStateException(
                "Login Google indisponível: configure o Web Client ID real do projeto Firebase. Nenhum ID de demonstração será utilizado."
            )
        }
    }
}

