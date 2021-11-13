package io.traxter.returnify

import java.lang.StringBuilder

abstract class ReturnError(val message: String) {
    var source: ReturnError? = null

    val errorDescription: String
        get() {
            val stringBuilder = StringBuilder()
            stringBuilder.append(
                """
                    |Error: ${this.message}
                """.trimMargin()
            )

            var sourceErr = this.source
            if (sourceErr != null) {
                stringBuilder.appendLine()
            }
            while (sourceErr != null) {
                stringBuilder.appendLine()
                stringBuilder.append(
                    """
                        |Caused by: ${sourceErr::class.simpleName}
                        |	${sourceErr.message}
                    """.trimMargin()
                )
                sourceErr = sourceErr.source
            }
            return stringBuilder.toString()
        }
}

fun <T : ReturnError> T.causedBy(source: ReturnError): T {
    this.source = source
    return this
}
