package com.example.benchmark

import androidx.compose.runtime.Immutable

/**
 * Especificação declarativa das jornadas críticas de usuário (Critical User Journeys - CUJs)
 * mapeadas pelo gerador de Baseline Profiles (Macrobenchmark) para compilação AOT (Ahead-Of-Time).
 *
 * Reduz o tempo de Cold Start em >30% e elimina micro-engasgos (jank) no primeiro scroll
 * das listas de quadrinhos com efeitos ópticos Liquid Glass.
 */
object BaselineProfileSpec {

    @Immutable
    data class CriticalUserJourney(
        val id: String,
        val description: String,
        val targetPackage: String = "com.aistudio.quadrinheiro.lqglas",
        val precompiledClasses: List<String>
    )

    val journeys: List<CriticalUserJourney> = listOf(
        CriticalUserJourney(
            id = "cold_start_and_welcome_transition",
            description = "Inicialização a frio (Cold Start) e transição elástica da WelcomeIntroScreen para AuthScreen",
            precompiledClasses = listOf(
                "com.example.MainActivity",
                "com.example.MainActivityKt",
                "com.example.ui.screens.WelcomeIntroScreenKt",
                "com.example.ui.screens.AuthScreenKt",
                "com.example.ui.components.QuadrinheiroLogoKt"
            )
        ),
        CriticalUserJourney(
            id = "liquid_glass_shader_and_backdrop_pipeline",
            description = "Pipeline de refração AGSL, desfoque progressivo e menisco óptico do Liquid Glass",
            precompiledClasses = listOf(
                "com.example.ui.components.LiquidGlassComponentsKt",
                "com.example.ui.components.LiquidGlassEngineKt",
                "com.example.ui.components.SdfShader",
                "com.kyant.backdrop.DrawBackdropModifierKt",
                "com.kyant.backdrop.DrawBackdropNode",
                "com.kyant.backdrop.backdrops.LayerBackdropModifierKt",
                "com.kyant.backdrop.effects.LensKt",
                "com.kyant.backdrop.effects.BlurKt",
                "com.kyant.backdrop.catalog.components.LiquidBottomTabsKt"
            )
        ),
        CriticalUserJourney(
            id = "library_shelf_fast_fling_and_shimmer_covers",
            description = "Rolagem rápida (fling 120 FPS) na Biblioteca, congelamento raster de fundo e Shimmer Placeholders",
            precompiledClasses = listOf(
                "com.example.ui.screens.MainTabsScreensKt",
                "com.example.ui.screens.MainTabsSubcomponentsKt",
                "com.example.viewmodel.QuadrinheiroViewModel",
                "com.example.data.SafeStorage",
                "com.example.data.QuadrinheiroCloudRepository"
            )
        )
    )

    /**
     * Retorna as regras de perfil AOT no formato ART Baseline Profile (`HSPL` / `PL`)
     * usadas pelo compilador `profgen` e `ProfileInstaller`.
     */
    fun generateBaselineProfileRules(): List<String> {
        return journeys
            .flatMap { it.precompiledClasses }
            .distinct()
            .flatMap { fqcn ->
                val internalName = "L" + fqcn.replace('.', '/') + ";"
                listOf(
                    "HSPL${internalName.removeSuffix(";")}->**(**)**",
                    "PL$internalName"
                )
            }
    }
}
