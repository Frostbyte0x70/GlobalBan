package definitions.db

import DbOperationException
import FatalErrorException

/**
 * Class used to perform database operations related to the trusted server list
 */
class TrustedDB(val db: Database) {
	companion object {
		private const val TRUSTED_SERVERS_TABLE_NAME = "trusted_servers"
	}

	init {
		// Create the table if it doesn't exist
		try {
			db.updateWithReconnect(
				// source --trusts-> destination
				"CREATE TABLE IF NOT EXISTS $TRUSTED_SERVERS_TABLE_NAME(source_id BIGINT(30) UNSIGNED NOT NULL, " +
					"destination_id BIGINT(30) UNSIGNED NOT NULL," +
					"PRIMARY KEY (source_id, destination_id));"
			)
		} catch (e: DbOperationException) {
			throw FatalErrorException("Cannot create trusted servers table", e)
		}
	}

	/**
	 * Returns all the trust mappings in the database. Output is a list of pairs of server IDs. For each pair, the
	 * first server trusts the second.
	 * The result will be sorted by first server ID.
	 */
	fun getAll(): List<Pair<Long, Long>> {
		val queryResult = db.queryWithReconnect("SELECT source_id, destination_id FROM $TRUSTED_SERVERS_TABLE_NAME " +
			"ORDER BY source_id")
		val res = mutableListOf<Pair<Long, Long>>()

		while (queryResult.next()) {
			val sourceId = queryResult.getLong(1)
			val destinationId = queryResult.getLong(2)

			res.add(Pair(sourceId, destinationId))
		}

		return res
	}

	/**
	 * Set that the [sourceId] server trusts the [destinationId] server
	 */
	fun setTrusted(sourceId: Long, destinationId: Long) {
		with(PrepStatement(db, "INSERT INTO $TRUSTED_SERVERS_TABLE_NAME (source_id, destination_id) " +
			"VALUES (?, ?) ON DUPLICATE KEY UPDATE source_id = source_id")) {
			setLong(sourceId)
			setLong(destinationId)
			executeUpdate()
		}
	}

	/**
	 * Set that the [sourceId] server does not trust the [destinationId] server
	 */
	fun setNotTrusted(sourceId: Long, destinationId: Long) {
		with(PrepStatement(db, "DELETE FROM $TRUSTED_SERVERS_TABLE_NAME WHERE source_id = ? AND destination_id = ?")) {
			setLong(sourceId)
			setLong(destinationId)
			executeUpdate()
		}
	}
}