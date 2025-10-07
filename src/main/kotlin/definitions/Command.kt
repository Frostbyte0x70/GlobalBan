package definitions

import definitions.globals.Env
import definitions.globals.Whitelist
import dev.minn.jda.ktx.messages.reply_
import getLogger
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent

/**
 * Wraps the execution of commands. Contains some tasks and checks common to all commands, such as checking if
 * the command can be used in the current server.
 * If the checks fail, an error message will be displayed to the user who ran the command.
 *
 * @param innerCall Function to run if the checks pass. Receives a [GenericCommandInteractionEvent], returns nothing.
 * @param mainServerOnly True if the command can only be used in the main server set in the bot config
 */
class Command(private val innerCall: (GenericCommandInteractionEvent) -> Unit, val mainServerOnly: Boolean = false) {
	private val logger = getLogger(this::class)

	fun run(event: GenericCommandInteractionEvent) {
		logger.info("Command run by ${event.user.name} on ${event.guild?.name ?: "DMs"}: ${event.commandString}")

		val serverId = event.guild?.idLong

		if (serverId != null) {
			// The command was run in a server, check that it can be run here

			if (!Whitelist.get().isWhitelisted(serverId)) {
				event.reply_("Error: Server is not whitelisted.", ephemeral = true).queue()
				return
			}

			if (mainServerOnly && serverId != Env.mainServerId) {
				event.reply_("Error: This command can only be used in the main server.", ephemeral = true).queue()
				return
			}
		}

		innerCall(event)
	}
}