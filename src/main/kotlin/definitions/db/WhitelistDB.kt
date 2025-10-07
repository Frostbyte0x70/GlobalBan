package definitions.db

import DbOperationException
import FatalErrorException
import java.sql.SQLException

/**
 * Class used to perform database operations related to the server whitelist
 */
class WhitelistDB(val db: Database) {
	companion object {
		private const val WHITELIST_TABLE_NAME = "Whitelist"
	}

	init {
		// Create the table if it doesn't exist
		try {
			db.updateWithReconnect(
				"CREATE TABLE IF NOT EXISTS $WHITELIST_TABLE_NAME(`server_id` BIGINT(30) UNSIGNED NOT NULL, " +
					"PRIMARY KEY (`server_id`));"
			)
		} catch (e: DbOperationException) {
			throw FatalErrorException("Cannot create whitelist table", e)
		}
	}

	/**
	 * Returns a list with the IDs of all the whitelisted servers
	 */
	fun getServers(): List<Long> {
		val res = mutableListOf<Long>()
		try {
			db.queryWithReconnect("SELECT * FROM $WHITELIST_TABLE_NAME").use { result ->
				while (result.next()) {
					res.add(result.getLong(1))
				}
			}
		} catch (e: SQLException) {
			throw DbOperationException(cause = e)
		}
		return res
	}

	/**
	 * Adds a server to the whitelist. If the server is already on the list, it will not be added again.
	 */
	fun addServer(id: Long) {
		with(PrepStatement(db, "INSERT INTO $WHITELIST_TABLE_NAME (server_id) VALUES (?) ON DUPLICATE KEY UPDATE")) {
			setLong(id)
			executeUpdate()
		}
	}

	/**
	 * Removes a server from the whitelist if present.
	 */
	fun removeServer(id: Long) {
		with(PrepStatement(db, "DELETE FROM $WHITELIST_TABLE_NAME WHERE server_id = ?")) {
			setLong(id)
			executeUpdate()
		}
	}
}