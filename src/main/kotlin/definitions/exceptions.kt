/**
 * Thrown for fatal errors that should make the bot crash
 */
class FatalErrorException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

/**
 * Thrown when an invalid operation is performed
 */
class IllegalOperationException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

/**
 * Thrown when an error happens while performing a database operation
 */
class DbOperationException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)