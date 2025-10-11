package handlers

import definitions.globals.Whitelist
import dev.minn.jda.ktx.events.listener
import getLogger
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.guild.GuildJoinEvent

/**
 * Implements tasks that should be run when the bot joins a server
 */
class ServerJoinHandler(jda: JDA) {
	private val logger = getLogger(this::class)

	init {
		// Leave joined guilds if they are not whitelisted
		jda.listener<GuildJoinEvent> { event ->
			if (!Whitelist.get().isWhitelisted(event.guild.idLong)) {
				logger.info("Leaving server '${event.guild.name} because it's not whitelisted'")
				event.guild.leave().queue()
			}

			// TODO: Server join setup
			// Trust default servers, send alert to all servers
		}
	}
}