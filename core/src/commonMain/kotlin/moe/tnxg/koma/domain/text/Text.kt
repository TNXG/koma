package moe.tnxg.koma.domain.text

private val whitespace = Regex("[\\s\\u00a0\\ufeff]+")

/** Collapse education payload line breaks and spacing without accepting non-text values. */
fun cleanText(value: String?): String = value?.replace(whitespace, " ")?.trim().orEmpty()
