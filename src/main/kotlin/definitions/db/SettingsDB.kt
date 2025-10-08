package definitions.db

import DbOperationException
import FatalErrorException
import definitions.ServerSettings

/**
 * Enum listing all settings and their corresponding database key
 */
enum class SettingKey(val dbKey: String) {
	KEY_NOTIFICATIONS_CHANNEL_ID("notifications_channel_id");

	companion object {
		fun fromDbKey(dbKey: String): SettingKey = entries.first { it.dbKey == dbKey }
	}
}

/**
 * Class used to perform database operations related to per-server settings
 */
class SettingsDB(val db: Database) {
	companion object {
		private const val SETTINGS_TABLE_NAME = "settings"
	}

	init {
		// Create the table if it doesn't exist
		try {
			db.updateWithReconnect(
				"CREATE TABLE IF NOT EXISTS $SETTINGS_TABLE_NAME(server_id BIGINT(30) UNSIGNED NOT NULL, " +
					"setting_key VARCHAR(64) NOT NULL," +
					"setting_value VARCHAR(256) NOT NULL," +
					"PRIMARY KEY (server_id));"
			)
		} catch (e: DbOperationException) {
			throw FatalErrorException("Cannot create whitelist table", e)
		}
	}

	/**
	 * Returns the settings for all servers that have stored settings. The output is a map that maps server IDs to their
	 * corresponding settings object.
	 * The returned [ServerSettings] objects will have database writes enabled.
	 */
	fun getAll(): MutableMap<Long, ServerSettings> {
		val queryResult = db.queryWithReconnect("SELECT server_id, setting_key, setting_value FROM $SETTINGS_TABLE_NAME")
		val result = mutableMapOf<Long, ServerSettings>()

		while (queryResult.next()) {
			val serverId = queryResult.getLong(1)
			val settingKey = queryResult.getString(2)
			val settingValue = queryResult.getString(3)

			val settings = result[serverId] ?: ServerSettings(serverId)
			setSettingField(settings, SettingKey.fromDbKey(settingKey), settingValue)

			result[serverId] = settings
		}

		// Enable database writes for the created ServerSettings objects
		for (settings in result.values) {
			settings.sdb = this
		}

		return result
	}

	fun setNotificationChannelId(serverId: Long, value: Long?) =
		setLong(serverId, SettingKey.KEY_NOTIFICATIONS_CHANNEL_ID.dbKey, value)

	/**
	 * Sets the [settingValue] of the setting identified by [settingKey] for the server with the given [serverId]
	 */
	private fun setString(serverId: Long, settingKey: String, settingValue: String?) {
		with(PrepStatement(db, "INSERT INTO $SETTINGS_TABLE_NAME (server_id, setting_key, setting_value) VALUES (?, ?, ?) " +
			"ON DUPLICATE KEY UPDATE server_id = server_id")) {
			setLong(serverId)
			setString(settingKey)
			setString(settingValue ?: "NULL")
			executeUpdate()
		}
	}

	private fun setLong(serverId: Long, settingKey: String, settingValue: Long?) {
		setString(serverId, settingKey, settingValue?.toString())
	}

	/**
	 * Sets the property in the given [settingsObj] corresponding to the setting identified by [settingKey] to
	 * [settingValue]
	 */
	private fun setSettingField(settingsObj: ServerSettings, settingKey: SettingKey, settingValue: String) {
		try {
			with(settingsObj) {
				when (settingKey) {
					SettingKey.KEY_NOTIFICATIONS_CHANNEL_ID -> notificationsChannelId = settingValue.toLong()
				}
			}
		} catch (e: NumberFormatException) {
			throw DbOperationException("Failed to convert setting $settingKey with value $settingValue from string",
				cause = e)
		}
	}
}