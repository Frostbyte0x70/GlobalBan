import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.core.config.Configurator
import kotlin.reflect.KClass

/**
 * Returns a logger for the given class. A logging [level] can optionally be specified. If [level] is not set,
 * [Env.logLevel] will be used.
 */
fun getLogger(c: KClass<*>, level: Level? = null): Logger {
    val logger = LogManager.getLogger(c)
    Configurator.setLevel(logger, level ?: Env.logLevel)
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