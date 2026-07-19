package com.project.myscale.data.model

// Theme names are proper nouns and stay untranslated
enum class ThemeOption(val label: String, val isDark: Boolean) {
    FOREST("Forest", false),
    MIDNIGHT("Midnight", true),
    SUNSET("Sunset", false)
}
