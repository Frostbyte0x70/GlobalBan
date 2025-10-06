package definitions.db

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Wrapper around [PreparedStatement]. Used to set prepared statement arguments with reconnections if needed, and to
 * automatically keep track of the parameter index.
 */
class PrepStatement(val db: Database, val sqlStatement: String) {
    private lateinit var statement: PreparedStatement

    // Index of the next parameter to set
    private var nextParamIndex = 0

    init {
        nextParamIndex = 1
        /*
		 * If the connection has been lost, this operation won't cause an error, but an exception will be thrown when
		 * trying to execute the statement later because the reference to the statement will be outdated.
		 * To avoid this, we check if a reconnection is necessary now.
		 */
        db.ensureConnection()
        db.runWithReconnect(
            { connection -> this.statement = connection.prepareStatement(sqlStatement) }, "Prepare $sqlStatement"
        )
    }

    fun setString(value: String) {
        db.runWithReconnect({ _ -> statement.setString(nextParamIndex, value) }, "Set string: $value")
        nextParamIndex++
    }

    fun setInt(value: Int) {
        db.runWithReconnect({ _ -> statement.setInt(nextParamIndex, value) }, "Set int: $value")
        nextParamIndex++
    }

    fun setLong(value: Long) {
        db.runWithReconnect({ _ -> statement.setLong(nextParamIndex, value) }, "Set long: $value")
        nextParamIndex++
    }

    fun setDouble(value: Double) {
        db.runWithReconnect({ _ -> statement.setDouble(nextParamIndex, value) }, "Set double: $value")
        nextParamIndex++
    }

    /**
     * Sets a parameter value to null
     * @param sqlColumnType Column type, as defined in [java.sql.Types].
     */
    fun setNull(sqlColumnType: Int) {
        db.runWithReconnect({ _ -> statement.setNull(nextParamIndex, sqlColumnType) }, "Set null")
        nextParamIndex++
    }

    fun executeQuery(): ResultSet {
        val result = AtomicReference<ResultSet>()
        db.runWithReconnect({ _ -> result.set(statement.executeQuery()) }, sqlStatement)
        return result.get()
    }

    fun executeUpdate(): Int {
        val result = AtomicInteger()
        db.runWithReconnect({ _ -> result.set(statement.executeUpdate()) }, sqlStatement)
        return result.get()
    }
}