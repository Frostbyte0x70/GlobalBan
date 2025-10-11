package definitions.globals

import IllegalOperationException
import definitions.db.TrustedDB

/**
 * Object that stores the trust map between servers.
 * This is a single-instance object. It must be initialized by calling [create]. After that, the instance can be
 * retrieved using [get].
 */
object TrustedServers {
	private lateinit var trustedDB: TrustedDB
	/** Key: Server ID. Value: List of servers trusted by the key server. */
	private lateinit var trustsMap: MutableMap<Long, MutableList<Long>>
	/** Key: Server ID. Value: List of servers that trust the key server. */
	private lateinit var trustedByMap: MutableMap<Long, MutableList<Long>>

	fun create(trustedDB: TrustedDB) {
		this.trustedDB = trustedDB

		val list = trustedDB.getAll()
		this.trustsMap = listToMap(list, true)
		this.trustedByMap = listToMap(list.sortedBy { it.second }, false)
	}

	/**
	 * Retrieves the single instance of this object, assuming it has been created already by calling [create].
	 * @throws IllegalOperationException If [create] has not been called yet.
	 */
	fun get(): TrustedServers {
		if (!this::trustedDB.isInitialized) {
			throw IllegalOperationException("create() must be called before get() can be called")
		}
		return this
	}

	/**
	 * Returns the list of servers that are trusted by the given server.
	 * Always includes the server itself.
	 */
	fun getTrustedBy(serverId: Long): List<Long> {
		val list = trustsMap[serverId] ?: listOf()
		return list.plus(serverId)
	}

	/**
	 * Returns the list of servers who trust the given server.
	 * Always includes the server itself.
	 */
	fun getWhoTrusts(serverId: Long): List<Long> {
		val list = trustedByMap[serverId] ?: listOf()
		return list.plus(serverId)
	}

	/**
	 * Sets whether the [sourceServerId] server trusts the [destinationServerId] server.
	 */
	fun setTrust(sourceServerId: Long, destinationServerId: Long, trust: Boolean) {
		if (trust) {
			trustedDB.setTrusted(sourceServerId, destinationServerId)

			var list = trustsMap[sourceServerId] ?: mutableListOf()
			if (!list.contains(destinationServerId)) list.add(destinationServerId)
			trustsMap[sourceServerId] = list

			list = trustedByMap[destinationServerId] ?: mutableListOf()
			if (!list.contains(sourceServerId)) list.add(sourceServerId)
			trustedByMap[destinationServerId] = list
		} else {
			trustedDB.setNotTrusted(sourceServerId, destinationServerId)

			var list = trustsMap[sourceServerId] ?: return
			list.remove(destinationServerId)

			list = trustedByMap[destinationServerId] ?:
				throw IllegalStateException("Trust link was present only in one of the maps")
			list.remove(sourceServerId)
		}
	}

	/**
	 * Converts a [list] of pairings to a map.
	 * If [firstElement] is set, the first element of each pair will be part of the keys on the output map. If it is
	 * set, the second elements will be used as the key.
	 * The input list must be sorted by the element of the pair marked by [firstElement]!
	 */
	private fun listToMap(list: List<Pair<Long, Long>>, firstElement: Boolean): MutableMap<Long, MutableList<Long>> {
		val res: MutableMap<Long, MutableList<Long>> = mutableMapOf()
		var currentKey: Long? = null
		var currentValue: MutableList<Long>? = null

		val it = list.iterator()
		while (it.hasNext()) {
			val itPair = it.next()
			val itKey = if (firstElement) itPair.first else itPair.second
			val itValue = if (firstElement) itPair.second else itPair.first

			if (itKey != currentKey) {
				if (currentKey != null) {
					res[currentKey] = currentValue!!
				}

				currentValue = mutableListOf<Long>().apply { add(itValue) }
			} else {
				currentValue!!.add(itValue)
			}

			currentKey = itKey
		}
		if (currentKey != null) {
			res[currentKey] = currentValue!!
		}

		return res
	}
}