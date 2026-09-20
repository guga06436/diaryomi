package com.diaryomi.util

import java.text.Normalizer
import java.util.Locale

/**
 * Utilitário para tratamento e normalização de texto.
 * Permite comparações insensíveis a maiúsculas/minúsculas (case-insensitive)
 * e insensíveis a acentos/diacríticos e variações de espaçamento.
 */
object TextNormalizer {
    private val DIACRITICS_REGEX = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    private val MULTIPLE_SPACES_REGEX = "\\s+".toRegex()
    private val PT_BR = Locale("pt", "BR")

    /**
     * Normaliza um texto removendo acentos, convertendo para minúsculas
     * e colapsando múltiplos espaços em branco.
     * Exemplo: "  JOSÉ   da Conceição! " -> "jose da conceicao!"
     */
    fun normalize(text: String): String {
        if (text.isEmpty()) return ""
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(DIACRITICS_REGEX, "")
            .lowercase(PT_BR)
            .replace(MULTIPLE_SPACES_REGEX, " ")
            .trim()
    }

    /**
     * Verifica se [source] contém [target], desconsiderando caixa alta/baixa,
     * acentos e espaços redundantes.
     */
    fun containsIgnoreCaseAndAccents(source: String, target: String): Boolean {
        if (target.isBlank()) return true
        val normalizedSource = normalize(source)
        val normalizedTarget = normalize(target)
        return normalizedSource.contains(normalizedTarget)
    }

    /**
     * Verifica se pelo menos um (ANY) dos termos em [terms] está presente no texto [source],
     * desconsiderando caixa alta/baixa, quebras de linha/espaços múltiplos e acentuação.
     */
    fun containsAnyTerm(source: String, terms: List<String>): Boolean {
        if (terms.isEmpty()) return false
        val normalizedSource = normalize(source)
        return terms.any { term ->
            val normalizedTerm = normalize(term)
            normalizedTerm.isNotEmpty() && normalizedSource.contains(normalizedTerm)
        }
    }

    /**
     * Retorna a lista de termos de [terms] que deram match no texto [source].
     */
    fun findMatchingTerms(source: String, terms: List<String>): List<String> {
        if (terms.isEmpty()) return emptyList()
        val normalizedSource = normalize(source)
        return terms.filter { term ->
            val normalizedTerm = normalize(term)
            normalizedTerm.isNotEmpty() && normalizedSource.contains(normalizedTerm)
        }
    }

    /**
     * Verifica se TODOS os termos em [terms] estão presentes no texto [source],
     * desconsiderando caixa alta/baixa e acentuação (AND lógico).
     */
    fun containsAllTerms(source: String, terms: List<String>): Boolean {
        if (terms.isEmpty()) return true
        val normalizedSource = normalize(source)
        return terms.all { term ->
            val normalizedTerm = normalize(term)
            normalizedTerm.isEmpty() || normalizedSource.contains(normalizedTerm)
        }
    }
}
