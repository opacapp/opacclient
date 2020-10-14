package de.geeksfactory.opacclient.utils

import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements

val String.html: Document
    get() = Jsoup.parse(this)

val String.jsonObject: JSONObject
    get() = JSONObject(this)

operator fun Element.get(name: String): String = this.attr(name)

val Element.text: String
    get() = this.text()

val Elements.text: String
    get() = this.text()

val TextNode.text: String
    get() = this.text()

// JSONArray extension functions
inline fun <reified T, R> JSONArray.map(transform: (T) -> R): List<R> =
        (0.until(length())).map { i -> transform(get(i) as T) }

inline fun <reified T> JSONArray.forEach(function: (T) -> Unit) =
        (0.until(length())).forEach { i -> function(get(i) as T) }
