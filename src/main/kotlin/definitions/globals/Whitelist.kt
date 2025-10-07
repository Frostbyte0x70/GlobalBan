package definitions.globals

import IllegalOperationException
import definitions.db.WhitelistDB

/**
 * Contains the in-memory representation of the server whitelist, and methods to interact with it.
 * This is a single-instance object. It must be initialized by calling [create]. After that, the instance can be
 * retrieved using [get].
 */
object Whitelist {
    private lateinit var whitelistDB: WhitelistDB
    // TODO: This is not thread-safe
    private lateinit var whitelist: MutableList<Long>

    /**
     * Creates the single instance of this object.
     */
    fun create(whitelistDB: WhitelistDB) : Whitelist {
        this.whitelistDB = whitelistDB
        whitelist = whitelistDB.getServers().toMutableList()

        return this
    }

    /**
     * Retrieves the single instance of this object, assuming it has been created already by calling [create].
     * @throws IllegalOperationException If [create] has not been called yet.
     */
    fun get() : Whitelist {
        if (!this::whitelistDB.isInitialized) {
            throw IllegalOperationException("create() must be called before get() can be called")
        }
        return this
    }

    /**
     * Returns whether a server is whitelisted or not. The main server is always considered to be whitelisted.
     */
    fun isWhitelisted(serverId: Long): Boolean {
        return serverId == Env.mainServerId || whitelist.contains(serverId)
    }

    /**
     * Returns a list with the ID of all whitelisted servers.
     */
    fun getAllServers(): List<Long> {
        return whitelist.toList()
    }

    /**
     * Adds a server to the whitelist. If the server is already on the list, it will not be added again.
     */
    fun add(serverId: Long) {
        whitelistDB.addServer(serverId)
        if (!whitelist.contains(serverId)) {
            whitelist.add(serverId)
        }
    }

    /**
     * Removes a server from the whitelist if present.
     */
    fun remove(serverId: Long) {
        whitelistDB.removeServer(serverId)
        whitelist.remove(serverId)
    }
}