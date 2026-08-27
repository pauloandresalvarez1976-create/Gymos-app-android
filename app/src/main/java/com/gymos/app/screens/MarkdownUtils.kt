package com.gymos.app.screens

fun limpiarMarkdown(texto: String): String {
    return texto
        .replace(Regex("#{1,6}\\s*"), "")
        .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
        .replace(Regex("\\*(.*?)\\*"), "$1")
        .replace(Regex("\\|---\\|---\\|---\\|"), "")
        .replace(Regex("\\|---|"), "")
        .replace(Regex("-{3,}"), "")
        .replace(Regex("^\\|", RegexOption.MULTILINE), "")
        .replace(Regex("\\|$", RegexOption.MULTILINE), "")
        .replace(Regex("\\|"), " —")
        .replace(Regex("> ", RegexOption.MULTILINE), "")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}