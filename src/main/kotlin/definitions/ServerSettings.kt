package definitions

import definitions.db.SettingsDB

/**
 * Holds the settings for a given server.
 * If the [sdb] property is set, updating the values of the properties in this class will automatically
 * sync them with the database.
 */
data class ServerSettings(val serverId: Long) {
	@Suppress("RedundantSetter")
	var sdb: SettingsDB? = null
		@Synchronized
		set(value) {
			field = value
		}

	var notificationsChannelId: Long? = null
		@Synchronized
		set(value) {
			if (sdb != null) {
				sdb!!.setNotificationChannelId(serverId, value)
			}
			field = value
		}
}
