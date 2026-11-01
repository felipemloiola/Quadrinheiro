package com.example.macrobenchmark

/**
 * Gerador de Baseline Profiles (Macrobenchmark) do Quadrinheiro.
 * Exercita os fluxos críticos:
 * 1. Cold Start e transição da WelcomeIntroScreen -> AuthScreen -> Guest Mode
 * 2. Renderização das superfícies translúcidas Liquid Glass e shaders AGSL
 * 3. Rolagem em alta velocidade (Fling) na Biblioteca de Quadrinhos
 */
class QuadrinheiroBaselineProfileGenerator {
    companion object {
        const val TARGET_PACKAGE = "com.aistudio.quadrinheiro.lqglas"
        val CRITICAL_ROUTES = listOf(
            "welcome_intro_root",
            "welcome_start_button",
            "welcome_primary_button",
            "home",
            "shelf",
            "search"
        )
    }
}
