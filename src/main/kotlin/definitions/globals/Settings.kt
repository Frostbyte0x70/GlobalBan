package definitions.globals

import IllegalOperationException
import definitions.ServerSettings
import definitions.db.SettingsDB

/**
 * Object that stores the settings of all servers.
 * This is a single-instance object. It must be initialized by calling [create]. After that, the instance can be
 * retrieved using [get].
 */
object Settings {
	private lateinit var settingsDB: SettingsDB
	private lateinit var settings: MutableMap<Long, ServerSettings>

	/**
	 * Creates the single instance of this object.
	 */
	fun create(settingsDB: SettingsDB): Settings {
		this.settingsDB = settingsDB
		settings = Settings.settingsDB.getAll()

		return this
	}

	/**
	 * Retrieves the single instance of this object, assuming it has been created already by calling [create].
	 * @throws IllegalOperationException If [create] has not been called yet.
	 */
	fun get(): Settings {
		if (!this::settingsDB.isInitialized) {
			throw IllegalOperationException("create() must be called before get() can be called")
		}
		return this
	}

	/**
	 * Returns the [ServerSettings] instance that corresponds to the server identified by the given [serverId],
	 * or null if no settings are saved for that server.
	 * Setting values can be directly modified in the returned instance to update them, both in memory and in the
	 * database.
	 */
	fun getForServer(serverId: Long): ServerSettings? {
		return settings[serverId]
	}

	/**
	 * Returns the [ServerSettings] instance that corresponds to the server identified by the given [serverId].
	 * If an instance is not present for the given server, creates it and returns it.
	 * Setting values can be directly modified in the returned instance to update them, both in memory and in the
	 * database.
	 */
	fun getOrCreateForServer(serverId: Long) : ServerSettings {
		var result = getForServer(serverId)
		if (result != null) {
			return result
		}

		result = ServerSettings(serverId).apply { this.sdb = settingsDB }
		settings[serverId] = result
		return result
	}
}