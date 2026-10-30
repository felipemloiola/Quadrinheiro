package com.example.data

import android.content.Context

/** Resolve a real configured OAuth client ID without a compile-time dependency on generated resources. */
internal object GoogleClientId {
    fun optional(context: Context): String {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (id == 0) return ""
        return runCatching { context.getString(id).trim() }.getOrDefault("")
            .takeUnless { it.startsWith("YOUR_", ignoreCase = true) }
            .orEmpty()
    }

    fun required(context: Context): String {
        return optional(context).ifBlank {
            throw IllegalStateException(
                "Login Google indisponível: configure o Web Client ID real do projeto Firebase. Nenhum ID de demonstração será utilizado."
            )
        }
    }
}
