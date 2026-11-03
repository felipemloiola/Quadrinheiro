package com.kyant.backdrop

import android.os.Build
import com.example.ui.components.SdfShader
import com.kyant.backdrop.internal.AmbientHighlightShaderString
import com.kyant.backdrop.internal.DefaultHighlightShaderString
import com.kyant.backdrop.internal.RoundedRectRefractionShaderString
import com.kyant.backdrop.internal.RoundedRectRefractionWithDispersionShaderString
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.Language

sealed interface RuntimeShaderCache {

    fun obtainRuntimeShader(key: String, @Language("AGSL") string: String): RuntimeShader
}

/**
 * Motor de Pre-Warming Assíncrono de Shaders AGSL e RenderEffects do Liquid Glass.
 * Pré-compila os programas AGSL em background (`Dispatchers.Default`) assim que o app inicia,
 * evitando a compilação síncrona no primeiro callback do Choreographer (First Frame UI).
 */
object LiquidGlassShaderPrewarmer {
    private val ready = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pools = ConcurrentHashMap<String, java.util.concurrent.ConcurrentLinkedQueue<android.graphics.RuntimeShader>>()
    private val refilling = ConcurrentHashMap<String, AtomicBoolean>()
    private val sources get() = listOf(
        RoundedRectRefractionShaderString, RoundedRectRefractionWithDispersionShaderString,
        DefaultHighlightShaderString, AmbientHighlightShaderString, InteractiveHighlightSource,
        SdfShader.AGSL_SDF_LIQUID_GLASS_SOURCE
    )
    val isReady: Boolean get() = ready.get()

    fun prewarmAsync(onComplete: (() -> Unit)? = null) {
        if (ready.get()) { onComplete?.invoke(); return }
        scope.launch { prewarmSynchronously(); onComplete?.invoke() }
    }

    @Synchronized
    fun prewarmSynchronously() {
        if (ready.get()) return
        if (Build.VERSION.SDK_INT >= 33) sources.forEach { fill(it) }
        ready.set(true)
    }

    @androidx.annotation.RequiresApi(33)
    private fun fill(source: String) {
        val queue = pools.getOrPut(source) { java.util.concurrent.ConcurrentLinkedQueue() }
        repeat((1 - queue.size).coerceAtLeast(0)) {
            runCatching { android.graphics.RuntimeShader(source) }.getOrNull()?.let(queue::offer)
        }
    }

    /** Each draw node owns a distinct mutable shader; uniforms are never shared. */
    @androidx.annotation.RequiresApi(33)
    internal fun obtain(source: String): android.graphics.RuntimeShader {
        val shader = pools[source]?.poll() ?: android.graphics.RuntimeShader(source)
        val filling = refilling.getOrPut(source) { AtomicBoolean(false) }
        if (filling.compareAndSet(false, true)) scope.launch {
            try { fill(source) } finally { filling.set(false) }
        }
        return shader
    }

    fun getPrewarmedCount(): Int = pools.size
}

internal class RuntimeShaderCacheImpl : RuntimeShaderCache {

    private val runtimeShaders = mutableMapOf<String, RuntimeShader>()

    override fun obtainRuntimeShader(key: String, string: String): RuntimeShader {
        return runtimeShaders.getOrPut(key) { RuntimeShader(string) }
    }

    fun clear() {
        runtimeShaders.clear()
    }
}

