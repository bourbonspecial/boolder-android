package com.boolder.boolder.domain.glossary

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ClimbingGlossaryTest {

    // the real glossary shipped with the app, so that the file itself is checked too
    private val glossary = ClimbingGlossary.fromJson(File("src/main/assets/glossary.json").readText())

    private fun termsIn(name: String) = glossary.findTerms(name).map { name.substring(it.range) }

    @Test
    fun `finds the qualifiers of a problem name`() {
        assertEquals(listOf("Assis"), termsIn("La Marie-Rose (Assis)"))
        assertEquals(listOf("rallongé", "par le bas"), termsIn("Samarkand (rallongé par le bas)"))
    }

    @Test
    fun `prefers the longest term`() {
        assertEquals(listOf("sans convention"), termsIn("La Quille (sans convention)"))
        assertEquals(listOf("retour-aller"), termsIn("Knees (retour-aller)"))
    }

    @Test
    fun `matches whole words only`() {
        assertEquals(emptyList<String>(), termsIn("Bassine Murène"))
        assertEquals(listOf("Bas"), termsIn("Le Bas-Cul"))
    }

    @Test
    fun `returns the term matched`() {
        val match = glossary.findTerms("Le Réta").single()

        assertEquals("réta", match.term.term)
        assertEquals("mantel", match.term.en.translation)
    }

    @Test
    fun `glossary file has unique lowercase terms, each with a translation`() {
        val terms = Json.parseToJsonElement(File("src/main/assets/glossary.json").readText())
            .jsonObject.getValue("terms").jsonArray
            .map { it.jsonObject }

        val texts = terms.map { it.getValue("term").jsonPrimitive.content }

        assertEquals(texts.size, texts.toSet().size)
        assertTrue(texts.all { it == it.lowercase() && it == it.trim() })
        assertTrue(terms.all { it.getValue("en").jsonObject.getValue("translation").jsonPrimitive.content.isNotBlank() })
    }
}
