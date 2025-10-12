package definitions

import definitions.db.SettingsDB

/**
 * Holds the settings for a given server.
 * If the [sdb] property is set, updating the values of the properties in this class will automatically
 * sync them with the database.
 */
data class ServerSettings(val serverId: Long) {
	companion object {
		/** Default value for the [ServerSettings.trustNewServers] setting */
		const val TRUST_NEW_SERVERS_DEFAULT = false
	}

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

	var trustNewServers: Boolean = TRUST_NEW_SERVERS_DEFAULT
		@Synchronized
		set(value) {
			if (sdb != null) {
				sdb!!.setTrustNewServers(serverId, value)
			}
			field = value
		}
}
