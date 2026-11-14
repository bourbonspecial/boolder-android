package com.boolder.boolder.domain.glossary

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * French climbing terms found in problem names ("assis", "rallongé", "du bas"...),
 * with an English explanation for climbers who don't speak French.
 *
 * The terms live in `assets/glossary.json`, a file shared as-is with the iOS app.
 */
class ClimbingGlossary(terms: List<GlossaryTerm>) {

    private val termsByText: Map<String, GlossaryTerm> = terms.associateBy { it.term.lowercase() }

    // Longest terms first, so that "sans convention" wins over "sans".
    // Whole words only: "bas" matches in "Le Bas-Cul" but not in "Bassine".
    private val regex: Regex? = termsByText.keys
        .sortedByDescending { it.length }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(prefix = "(?<!\\p{L})(", separator = "|", postfix = ")(?!\\p{L})") { Regex.escape(it) }
        ?.toRegex(RegexOption.IGNORE_CASE)

    /** The glossary terms found in [text], in order and not overlapping. */
    fun findTerms(text: String): List<GlossaryMatch> {
        val regex = regex ?: return emptyList()

        return regex.findAll(text).mapNotNull { result ->
            termsByText[result.value.lowercase()]?.let { GlossaryMatch(range = result.range, term = it) }
        }.toList()
    }

    companion object {
        private const val ASSET_NAME = "glossary.json"

        private val json = Json { ignoreUnknownKeys = true }

        @Volatile
        private var instance: ClimbingGlossary? = null

        fun fromJson(text: String): ClimbingGlossary =
            ClimbingGlossary(json.decodeFromString<GlossaryFile>(text).terms)

        /** The glossary from the app's assets, read once (it is about 15 KB). */
        fun get(context: Context): ClimbingGlossary =
            instance ?: synchronized(this) {
                instance ?: context.assets.open(ASSET_NAME)
                    .bufferedReader()
                    .use { fromJson(it.readText()) }
                    .also { instance = it }
            }
    }
}

data class GlossaryMatch(val range: IntRange, val term: GlossaryTerm)

@Serializable
data class GlossaryTerm(
    val term: String,
    val category: String,
    val en: GlossaryDefinition
)

@Serializable
data class GlossaryDefinition(
    val translation: String,
    val explanation: String? = null
)

@Serializable
private data class GlossaryFile(val terms: List<GlossaryTerm>)
