package definitions.db

import DbOperationException
import FatalErrorException
import getLogger
import java.sql.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Class that handles database connections and operations
 */
class Database(val host: String, val port: String, val user: String, val password: String, val database: String) {
    private val logger = getLogger(this::class)
    var connection: Connection

    companion object {
        // Amount of seconds to wait before determining that a database connection has been lost
        private const val DB_PING_TIMEOUT = 2
    }

    init {
        try {
            this.connection = connect()
        } catch (e: DbOperationException) {
            throw FatalErrorException(cause = e)
        }
    }

    /**
     * Runs a database operation that could throw an error. If it does and the error is due to the DB connection
     * being lost, it will attempt to reconnect if possible.
     * <br></br>**Note**: Some operations will not fail immediately if the connection has been lost (for example,
     * [Connection.prepareStatement]), but will throw an error later on, even if a reconnect is performed.
     * In those cases, it's convenient to run [ensureConnection] first to check if a reconnection is necessary.
     * @param dbOperation The operation to run. Can be anything capable of throwing an [SQLException].
     * @param operation A string that describes the operation performed. Used for error messages.
     * @throws DbOperationException If the operation throws an error for reasons other than a disconnect, if the
     * reconnect attempt fails or if the operation throws an error after a successful reconnection.
     */
    fun runWithReconnect(dbOperation: (Connection) -> Unit, operation: String?) {
        try {
            dbOperation(connection)
        } catch (_: SQLRecoverableException) {
            // DB connection lost, reconnect and try again
            logger.warn("Database connection lost. Attempting to reconnect.")
            connection = connect()
            try {
                dbOperation(connection)
            } catch (e2: SQLException) {
                throw DbOperationException("Error when retrying DB operation.\nOperation: $operation", e2)
            }
        } catch (e: SQLException) {
            throw DbOperationException("Error when performing DB operation.\nOperation: $operation", e)
        }
    }

    /**
     * Executes a simple SQL query, attempting a reconnection if required. Should not be used for queries with
     * parameters or that are meant to run multiple times. Use [PrepStatement] for that.
     * @param query The query to execute
     * @return Query result
     */
    fun queryWithReconnect(query: String): ResultSet {
        val result = AtomicReference<ResultSet>()
        runWithReconnect({ connection: Connection ->
            val statement = connection.createStatement()
            statement.closeOnCompletion()
            result.set(statement.executeQuery(query))
        }, query)
        return result.get()
    }

    /**
     * Executes a simple SQL update query, attempting a reconnection if required. Should not be used for queries with
     * parameters or that are meant to run multiple times. Use [PrepStatement] for that.
     * @param query The query to execute
     * @return Number of affected rows, or 0 for statements that return nothing.
     */
    fun updateWithReconnect(query: String): Int {
        val result = AtomicInteger()
        runWithReconnect({ connection: Connection ->
            val statement = connection.createStatement()
            statement.closeOnCompletion()
            result.set(statement.executeUpdate(query))
        }, query)
        return result.get()
    }

    /**
     * Checks if the database connection has been lost and reconnects if that's the case.
     * @throws DbOperationException If the reconnect attempt fails
     */
    fun ensureConnection() {
        val fail: Boolean = try {
            !connection.isValid(DB_PING_TIMEOUT)
        } catch (_: SQLException) {
            true
        }
        if (fail) {
            logger.warn("Database ping failed. Attempting to reconnect.")
            connection = connect()
        }
    }

    private fun connect(): Connection {
        val url = "jdbc:mysql://$host:$port/$database"
        try {
            val connection = DriverManager.getConnection(url, user, password)
            logger.debug("Database connection successful")
            return connection
        } catch (e: SQLException) {
            throw DbOperationException("Database connection failed", e)
        }
    }
}