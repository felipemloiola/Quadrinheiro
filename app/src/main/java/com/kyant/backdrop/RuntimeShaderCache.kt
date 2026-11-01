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

    private val isPrewarmed = AtomicBoolean(false)
    private val prewarmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val compiledTemplates = ConcurrentHashMap<String, String>()

    val isReady: Boolean
        get() = isPrewarmed.get()

    /**
     * Inicia a pré-compilação assíncrona dos shaders AGSL fora da Main Thread.
     */
    fun prewarmAsync(onComplete: (() -> Unit)? = null) {
        if (isPrewarmed.get()) {
            onComplete?.invoke()
            return
        }
        prewarmScope.launch {
            prewarmSynchronously()
            onComplete?.invoke()
        }
    }

    /**
     * Compila e valida todos os shaders AGSL críticos do Liquid Glass (Refraction, Dispersion,
     * Highlights e SDF Meniscus) aquecendo o compilador SkSL/AGSL da GPU/CPU.
     */
    fun prewarmSynchronously() {
        if (isPrewarmed.compareAndSet(false, true)) {
            val shaderSources = mapOf(
                "Refraction" to RoundedRectRefractionShaderString,
                "RefractionWithDispersion" to RoundedRectRefractionWithDispersionShaderString,
                "Default" to DefaultHighlightShaderString,
                "Ambient" to AmbientHighlightShaderString,
                "SdfLiquidGlass" to SdfShader.AGSL_SDF_LIQUID_GLASS_SOURCE
            )
            shaderSources.forEach { (key, source) ->
                compiledTemplates[key] = source
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    runCatching {
                        val shader = android.graphics.RuntimeShader(source)
                        when (key) {
                            "Refraction", "RefractionWithDispersion" -> {
                                shader.setFloatUniform("size", 200f, 64f)
                                shader.setFloatUniform("offset", 0f, 0f)
                                shader.setFloatUniform("cornerRadii", floatArrayOf(32f, 32f, 32f, 32f))
                                shader.setFloatUniform("refractionHeight", 24f)
                                shader.setFloatUniform("refractionAmount", -32f)
                                shader.setFloatUniform("depthEffect", 1f)
                                if (key == "RefractionWithDispersion") {
                                    shader.setFloatUniform("chromaticAberration", 1f)
                                }
                            }
                            "Default" -> {
                                shader.setFloatUniform("size", 200f, 64f)
                                shader.setFloatUniform("cornerRadii", floatArrayOf(32f, 32f, 32f, 32f))
                                shader.setFloatUniform("angle", 0.7853982f)
                                shader.setFloatUniform("falloff", 1f)
                            }
                            "Ambient" -> {
                                shader.setFloatUniform("size", 200f, 64f)
                                shader.setFloatUniform("cornerRadii", floatArrayOf(32f, 32f, 32f, 32f))
                                shader.setFloatUniform("angle", 0.7853982f)
                                shader.setFloatUniform("falloff", 1f)
                            }
                            "SdfLiquidGlass" -> {
                                SdfShader.createSdfLiquidGlassRenderEffect(
                                    radiusX = 16f,
                                    radiusY = 16f,
                                    width = 200f,
                                    height = 64f,
                                    cornerRadiusPx = 32f,
                                    enableEdgeRefraction = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun getPrewarmedCount(): Int = compiledTemplates.size
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

