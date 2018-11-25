package de.geeksfactory.opacclient.utils

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements

val String.html: Document
    get() = Jsoup.parse(this)

operator fun Element.get(name: String): String = this.attr(name)

val Element.text: String
    get() = this.text()

val Elements.text: String
    get() = this.text()

val TextNode.text: String
    get() = this.text()