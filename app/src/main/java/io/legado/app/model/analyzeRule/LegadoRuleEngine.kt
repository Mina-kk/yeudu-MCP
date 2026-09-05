package io.legado.app.model.analyzeRule

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Stable public facade around LegadoTeam/legado rule analyzers.
 * The analyzers in this package are adapted from the GPL-3.0 upstream repository.
 */
@Keep
class LegadoRuleEngine {
    enum class Kind { CSS, XPATH, JSON_PATH, REGEX }

    @Keep
    data class Output(
        @SerializedName("values") val values: List<String>,
        @SerializedName("first") val first: String?,
        @SerializedName("count") val count: Int,
    )

    fun extract(content: String, rawRule: String, kind: Kind = detect(rawRule)): Output {
        val reversed = rawRule.trimStart().startsWith("-") && !rawRule.trimStart().startsWith("--")
        val rule = if (reversed) rawRule.trimStart().substring(1) else rawRule
        val normalized = normalize(rule, kind)
        var values = when (kind) {
            Kind.CSS -> AnalyzeByJSoup(content).getStringList(normalized)
            Kind.XPATH -> AnalyzeByXPath(content).getStringList(normalized)
            Kind.JSON_PATH -> AnalyzeByJSonPath(content).getStringList(normalized)
            Kind.REGEX -> AnalyzeByRegex.getElements(content, arrayOf(normalized)).mapNotNull { it.firstOrNull() }
        }
        if (reversed) values = values.asReversed()
        return Output(values, values.firstOrNull(), values.size)
    }

    fun elements(content: String, rawRule: String, kind: Kind = detect(rawRule)): List<String> {
        val reversed = rawRule.trimStart().startsWith("-") && !rawRule.trimStart().startsWith("--")
        val rule = if (reversed) rawRule.trimStart().substring(1) else rawRule
        val normalized = normalize(rule, kind)
        var result = when (kind) {
            Kind.CSS -> AnalyzeByJSoup(content).getElements(normalized).map { it.outerHtml() }
            Kind.XPATH -> AnalyzeByXPath(content).getElements(normalized).orEmpty().map { it.toString() }
            Kind.JSON_PATH -> AnalyzeByJSonPath(content).getStringList(normalized)
            Kind.REGEX -> AnalyzeByRegex.getElements(content, arrayOf(normalized)).mapNotNull { it.firstOrNull() }
        }
        if (reversed) result = result.asReversed()
        return result
    }

    companion object {
        fun detect(rule: String): Kind = when {
            rule.startsWith("@XPath:", true) || rule.trimStart().startsWith("//") -> Kind.XPATH
            rule.startsWith("@Json:", true) || rule.trimStart().startsWith("$.") -> Kind.JSON_PATH
            rule.startsWith(":") -> Kind.REGEX
            else -> Kind.CSS
        }

        private fun normalize(rule: String, kind: Kind): String = when (kind) {
            Kind.XPATH -> rule.removePrefix("@XPath:").removePrefix("@xpath:")
            Kind.JSON_PATH -> rule.removePrefix("@Json:").removePrefix("@json:")
            Kind.REGEX -> rule.removePrefix(":")
            Kind.CSS -> rule.removePrefix("@@")
        }
    }
}
