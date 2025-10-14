package handlers

import definitions.Command
import definitions.CommandCreator
import definitions.ErrorHandler
import definitions.TRUSTED_CMD_PERMISSION
import definitions.globals.TrustedServers
import dev.minn.jda.ktx.interactions.commands.option
import dev.minn.jda.ktx.interactions.commands.restrict
import dev.minn.jda.ktx.interactions.commands.subcommand
import dev.minn.jda.ktx.interactions.components.getOption
import dev.minn.jda.ktx.messages.reply_
import dev.minn.jda.ktx.messages.send
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import kotlin.text.toLongOrNull

class TrustedHandler(private val jda: JDA, commandCreator: CommandCreator) {
	companion object {
		const val TRUSTED_COMMAND = "trusted"
		private const val SHOW_SUBCOMMAND = "show"
		private const val ADD_SUBCOMMAND = "add"
		private const val REMOVE_SUBCOMMAND = "remove"
	}

	init {
		with(commandCreator) {
			slash(
				TRUSTED_COMMAND,
				"View or edit the list of trusted servers for this server",
				Command(::runTrustedCommand)
			) {
				restrict(guild = true, perm = TRUSTED_CMD_PERMISSION)
				subcommand(SHOW_SUBCOMMAND, "Show list of trusted and non-trusted servers")
				subcommand(ADD_SUBCOMMAND, "Add a server to the trusted list") {
					option<String>("server_id", "ID of the server to add", required = true)
				}
				subcommand(REMOVE_SUBCOMMAND, "Remove a server from the trusted list") {
					option<String>("server_id", "ID of the server to remove", required = true)
				}
			}
		}
	}

	private fun runTrustedCommand(event: GenericCommandInteractionEvent) {
		when (event.subcommandName) {
			SHOW_SUBCOMMAND -> runShowSubcommand(event)
			ADD_SUBCOMMAND -> runAddSubcommand(event)
			REMOVE_SUBCOMMAND -> runRemoveSubcommand(event)
		}
	}

	private fun runShowSubcommand(event: GenericCommandInteractionEvent) {
		val server = event.guild!!

		val trustedServers = TrustedServers.get().getTrustedBy(server.idLong).minus(server.idLong)
		val nonTrustedServers = jda.guilds.map { it.idLong }.minus(trustedServers.toSet()).minus(server.idLong)

		var msg = "Trusted servers for ${server.name}:"
		for (serverId in trustedServers) {
			msg += "\n- ${(jda.getGuildById(serverId)?.name?.plus(" ")) ?: ""}($serverId)"
		}
		msg += "\nOther servers:"
		for (serverId in nonTrustedServers) {
			msg += "\n- ${(jda.getGuildById(serverId)?.name?.plus(" ")) ?: ""}($serverId)"
		}
		event.reply_(msg).queue()
	}

	private fun runAddSubcommand(event: GenericCommandInteractionEvent) {
		addRemove(event, true)
	}

	private fun runRemoveSubcommand(event: GenericCommandInteractionEvent) {
		addRemove(event, false)
	}

	private fun addRemove(event: GenericCommandInteractionEvent, add: Boolean) {
		// TODO: Once Componentsv2 is supported by jda-ktx, consider replacing this with a Componentsv2 modal that
		//  shows a list of checkboxes, one per server.
		val serverId = event.getOption<String>("server_id")!!.toLongOrNull()
		if (serverId == null) {
			event.reply_("Error: The provided server ID is not valid.", ephemeral = true).queue()
			return
		}

		if (serverId == event.guild!!.idLong) {
			event.reply_("You cannot ${if (add) "add" else "remove"} your own server ${if (add) "to" else "from"} " +
				"the trusted list.", ephemeral = true).queue()
			return
		}

		event.deferReply().queue()
		try {
			TrustedServers.get().setTrust(event.guild!!.idLong, serverId, add)
			event.hook.send("Successfully ${if (add) "added" else "removed"} the server ${if (add) "to" else "from"} " +
				"the trusted list.").queue()
		} catch (e: Exception) {
			with(ErrorHandler(e)) {
				printToErrorChannel(jda, event.guild)
				replyDeferred(event)
			}
		}
	}
}