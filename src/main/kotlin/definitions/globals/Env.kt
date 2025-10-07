package definitions.globals

import ch.qos.logback.classic.Level
import parseBool
import tryGetEnvVar

/**
 * Object containing values read from environment variables
 */
data object Env {
    private const val ENV_BOT_TOKEN = "BOT_TOKEN"
    private const val ENV_MAIN_SERVER_ID = "MAIN_SERVER_ID"
    private const val ENV_DB_HOST: String = "DB_HOST"
    private const val ENV_DB_PORT: String = "DB_PORT"
    private const val ENV_DB_USER: String = "DB_USER"
    private const val ENV_DB_PASSWORD: String = "DB_PASSWORD"
    private const val ENV_DB_DATABASE: String = "DB_DATABASE"
    private const val ENV_LOG_LEVEL = "LOG_LEVEL"
    private const val ENV_COMMAND_TEST_MODE = "COMMAND_TEST_MODE"

    val DEFAULT_LOG_LEVEL: Level = Level.INFO

    /** Bot token */
    val token: String by lazy {tryGetEnvVar(ENV_BOT_TOKEN)}

    /**
     * ID of the main server for the bot. All commands can be run from this server. Some commands can only be
     * run from this server.
     */
    val mainServerId: Long by lazy {tryGetEnvVar(ENV_MAIN_SERVER_ID).toLong()}

    val dbHost: String by lazy {tryGetEnvVar(ENV_DB_HOST)}
    val dbPort: String by lazy {tryGetEnvVar(ENV_DB_PORT)}
    val dbUser: String by lazy {tryGetEnvVar(ENV_DB_USER)}
    val dbPassword: String by lazy {tryGetEnvVar(ENV_DB_PASSWORD)}
    val dbDatabase: String by lazy {tryGetEnvVar(ENV_DB_DATABASE)}

    /**
     * If true, commands will only be registered in the main server. Useful for testing, since global commands
     * take a while to update.
     */
    val commandTestMode: Boolean by lazy {
        val str = System.getenv(ENV_COMMAND_TEST_MODE)
        str?.let {parseBool(str)} ?: false
    }

    /**
     * Logging level to use for log calls. If unset in the environment configuration, defaults to [DEFAULT_LOG_LEVEL].
     */
    val logLevel: Level by lazy {
        val str = System.getenv(ENV_LOG_LEVEL)

        str?.let {
            // Raises an exception if the level name is invalid
            Level.valueOf(str)
        } ?: DEFAULT_LOG_LEVEL
    }
}