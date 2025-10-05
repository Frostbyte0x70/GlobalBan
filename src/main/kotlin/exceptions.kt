/**
 * Thrown for fatal errors that should make the bot crash
 */
class FatalErrorException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)