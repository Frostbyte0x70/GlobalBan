package features

import dev.minn.jda.ktx.events.listener
import getLogger
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.guild.GuildJoinEvent

/**
 * Implements global bot listeners
 */
class GlobalListeners(jda: JDA) {
    val logger = getLogger(GlobalListeners::class)

    init {
        // Leave joined guilds if they are not whitelisted
        jda.listener<GuildJoinEvent> { event ->
            // TODO: Check full whitelist
            if (event.guild.idLong != Env.mainServerId) {
                logger.info("Leaving server '${event.guild.name} because it's not whitelisted'")
                event.guild.leave().queue()
            }
        }
    }
}