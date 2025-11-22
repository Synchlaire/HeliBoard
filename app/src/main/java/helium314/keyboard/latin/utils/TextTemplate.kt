// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.latin.utils

import android.content.Context
import android.content.SharedPreferences
import helium314.keyboard.latin.settings.Settings
import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a text expansion template
 */
data class TextTemplate(
    val shortcut: String,
    val expansion: String,
    val enabled: Boolean = true
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("shortcut", shortcut)
            put("expansion", expansion)
            put("enabled", enabled)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): TextTemplate {
            return TextTemplate(
                shortcut = json.getString("shortcut"),
                expansion = json.getString("expansion"),
                enabled = json.optBoolean("enabled", true)
            )
        }
    }
}

/**
 * Manages text expansion templates
 */
object TextTemplateManager {
    private const val PREF_TEMPLATES = "text_templates"

    private var templates: MutableMap<String, TextTemplate> = mutableMapOf()
    private var initialized = false

    /**
     * Initialize templates from preferences
     */
    fun init(context: Context) {
        if (initialized) return

        val prefs = Settings.readOnlyPrefs(context)
        loadTemplates(prefs)

        // Add default templates if none exist
        if (templates.isEmpty()) {
            addDefaultTemplates()
            saveTemplates(Settings.readWritePrefs(context))
        }

        initialized = true
    }

    /**
     * Get all templates
     */
    fun getAllTemplates(): List<TextTemplate> {
        return templates.values.toList()
    }

    /**
     * Get template by shortcut
     */
    fun getTemplate(shortcut: String): TextTemplate? {
        return templates[shortcut.lowercase()]
    }

    /**
     * Add or update a template
     */
    fun addTemplate(template: TextTemplate, prefs: SharedPreferences) {
        templates[template.shortcut.lowercase()] = template
        saveTemplates(prefs)
    }

    /**
     * Remove a template
     */
    fun removeTemplate(shortcut: String, prefs: SharedPreferences) {
        templates.remove(shortcut.lowercase())
        saveTemplates(prefs)
    }

    /**
     * Check if text ends with a template shortcut
     * @return Pair of (template, shortcut length) if found, null otherwise
     */
    fun findTemplateInText(text: String): Pair<TextTemplate, Int>? {
        if (text.isEmpty()) return null

        // Find the last word (after space or at start)
        val lastSpaceIndex = text.lastIndexOf(' ')
        val word = if (lastSpaceIndex >= 0) {
            text.substring(lastSpaceIndex + 1)
        } else {
            text
        }

        // Check if this word matches a template shortcut
        val template = getTemplate(word)
        if (template != null && template.enabled) {
            return Pair(template, word.length)
        }

        return null
    }

    private fun loadTemplates(prefs: SharedPreferences) {
        templates.clear()

        val templatesJson = prefs.getString(PREF_TEMPLATES, null) ?: return

        try {
            val jsonArray = JSONArray(templatesJson)
            for (i in 0 until jsonArray.length()) {
                val template = TextTemplate.fromJson(jsonArray.getJSONObject(i))
                templates[template.shortcut.lowercase()] = template
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveTemplates(prefs: SharedPreferences) {
        val jsonArray = JSONArray()
        templates.values.forEach { template ->
            jsonArray.put(template.toJson())
        }

        prefs.edit()
            .putString(PREF_TEMPLATES, jsonArray.toString())
            .apply()
    }

    private fun addDefaultTemplates() {
        // Add some useful default templates
        templates["eml"] = TextTemplate("eml", "your@email.com")
        templates["addr"] = TextTemplate("addr", "Your address here")
        templates["ph"] = TextTemplate("ph", "Your phone number")
        templates["sig"] = TextTemplate("sig", "Best regards,\nYour Name")
        templates["shrug"] = TextTemplate("shrug", "¯\\_(ツ)_/¯")
        templates["tm"] = TextTemplate("tm", "™")
        templates["copy"] = TextTemplate("copy", "©")
        templates["reg"] = TextTemplate("reg", "®")
    }
}
