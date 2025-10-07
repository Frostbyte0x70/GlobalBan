import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import definitions.globals.Env
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.reflect.KClass

/**
 * Returns a logger for the given class. A logging [level] can optionally be specified. If [level] is not set,
 * [definitions.globals.Env.logLevel] will be used.
 */
fun getLogger(c: KClass<*>?, level: Level? = null): Logger {
	val logger = LoggerFactory.getLogger(c?.simpleName ?: "") as Logger
    logger.level = level ?: Env.logLevel
    return logger
}

/**
 * Attempts to parse a string as a boolean value
 */
fun parseBool(value: String): Boolean {
    return when(value.lowercase()) {
        "0", "false" -> false
        "1", "true" -> true
        else -> throw FatalErrorException("Invalid boolean value '$value'")
    }
}

/**
 * Attempts to read the value of an environment variable. Throws [FatalErrorException] if unset.
 */
fun tryGetEnvVar(name: String): String {
    return System.getenv(name) ?:
    throw FatalErrorException("$name environment variable missing")
}

/**
 * Given a throwable, returns a string that includes its string representation and stacktrace.
 * @param t Throwable to convert to a string
 * @return Full string representation of the throwable
 */
fun throwableToStr(t: Throwable): String {
	val writer = StringWriter()
	t.printStackTrace(PrintWriter(writer))
	return writer.toString()
}

/**
 * Given a string, truncates it to ensure it doesn't exceed maxChars in length.
 * The truncation is performed by removing the last lines of the string, ana appending a new one that lists
 * how many lines were removed.
 * @param string The string to truncate
 * @param maxChars The maximum amount of characters the final string can have
 * @return Final string, with some lines removed if required. Will always have at most maxChar characters.
 */
fun truncateLines(string: String, maxChars: Int): String {
	if (string.length > maxChars) {
		val lines = string.split("\n")
		var totalLength = 0
		var numLines = 0
		while (totalLength < maxChars) {
			totalLength += lines[numLines].length + 1 // To account for the line break
			numLines++
		}
		// Last added line didn't fit, remove it
		numLines--
		totalLength -= lines[numLines].length + 1

		// Add the extra characters needed to include the message that shows how many lines were truncated
		val truncatedMsgLength = ("\n[+${lines.size - numLines} lines]").length
		totalLength += truncatedMsgLength
		// If we exceed the max, we need to remove more lines
		// The length of the message might change since the amount of removed lines will increase. Assume it does
		// to avoid headaches.
		totalLength += 1
		while (totalLength > maxChars) {
			numLines--
			totalLength -= lines[numLines].length + 1
		}

		// Build the final string
		var message = lines.subList(0, numLines).joinToString("\n")
		message += "\n[+${lines.size - numLines} lines]"

		// Double-check that it doesn't exceed the limit
		if (message.length > maxChars) {
			getLogger(null).warn("Final truncateLines() message had " + message.length + " characters!")
			message = message.take(maxChars)
		}
		return message
	} else {
		return string
	}
}