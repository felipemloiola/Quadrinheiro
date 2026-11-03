package com.example

enum class LauncherIconStyle(val id: String, val label: String, val background: Long) {
    DEFAULT("default", "Padrão", 0xFFF3F4F7),
    BLACK("black", "Preto", 0xFF000000),
    WHITE("white", "Branco", 0xFFF3F4F7),
    PURPLE("purple", "Roxo", 0xFF8B5CF6),
    BLUE("blue", "Azul", 0xFF2E7CF6),
    GREEN("green", "Verde", 0xFF28A745),
    ORANGE("orange", "Laranja", 0xFFE67E22),
    RED("red", "Vermelho", 0xFFD9383A);
    val alias: String get() = "com.example.LauncherColor${id.replaceFirstChar { it.uppercase() }}"
    companion object { fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: DEFAULT }
}
