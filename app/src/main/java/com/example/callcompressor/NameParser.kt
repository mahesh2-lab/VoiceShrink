package com.example.callcompressor

data class ParsedName(
    val contact: String,
    val number: String? = null,
    val timestampStr: String? = null,
)

fun parseRecordingName(rawName: String): ParsedName {
    val nameWithoutExt = rawName.substringBeforeLast('.')
    val parts = nameWithoutExt.split('_')

    if (parts.size >= 3) {
        val firstPart = parts[0]
        if (firstPart.equals("Call", ignoreCase = true) || firstPart.equals("Rec", ignoreCase = true)) {
            val secondPart = parts[1]
            val thirdPart = parts[2]

            val isSecondNumber = secondPart.matches(Regex("^[+]?[0-9]{7,15}$"))
            val isThirdNumber = thirdPart.matches(Regex("^[+]?[0-9]{7,15}$"))

            if (!isSecondNumber && isThirdNumber) {
                return ParsedName(
                    contact = secondPart.trim(),
                    number = thirdPart,
                    timestampStr = parts.drop(3).joinToString("_").ifBlank { null },
                )
            } else if (isSecondNumber) {
                return ParsedName(
                    contact = secondPart,
                    number = secondPart,
                    timestampStr = parts.drop(2).joinToString("_").ifBlank { null },
                )
            }
        }
    }

    return ParsedName(contact = nameWithoutExt, number = null, timestampStr = null)
}

fun maskPhoneNumber(number: String): String {
    if (number.length <= 3) return number
    val visible = number.takeLast(3)
    val maskedCount = (number.length - 3).coerceAtLeast(3)
    return "•".repeat(maskedCount) + visible
}
