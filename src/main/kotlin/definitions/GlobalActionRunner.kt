package definitions

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

/**
 * Used to perform an action across a given list of servers.
 * Each action should return a result of type [T].
 */
class GlobalActionRunner<T>(val serverIds: List<Long>) {
	private val results: MutableMap<Long, T> = mutableMapOf()

	/**
	 * Runs the given [action] across all set servers.
	 * [action] is a suspendable function that returns a result of type [T].
	 * Returns a map that maps server IDs to their result object.
	 */
	fun run(action: suspend (serverId: Long) -> T): Map<Long, T> {
		results.clear()

		runBlocking {
			val futures: MutableMap<Long, Deferred<T>> = mutableMapOf()

			for (serverId in serverIds) {
				futures[serverId] = this.async { action(serverId) }
			}
			for (entry in futures) {
				setResult(entry.key, entry.value.await())
			}
		}

		return results
	}

	@Synchronized
	private fun setResult(serverId: Long, result: T) {
		results[serverId] = result
	}
}