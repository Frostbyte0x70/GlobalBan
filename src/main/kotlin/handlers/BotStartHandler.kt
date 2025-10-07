package handlers

import definitions.globals.Whitelist
import getLogger
import net.dv8tion.jda.api.JDA

/**
 * Implements tasks that should be run when the bot is started
 */
class BotStartHandler(jda: JDA) {
    private val logger = getLogger(this::class)

    init {
        // Leave all non-whitelisted servers
        val whitelist = Whitelist.get()
        for (guild in jda.guilds) {
            if (!whitelist.isWhitelisted(guild.idLong)) {
                logger.info("Leaving server '${guild.name} because it's not whitelisted'")
                guild.leave().queue()
            }
        }
    }
}