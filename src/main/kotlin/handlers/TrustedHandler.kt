package handlers

import definitions.Command
import definitions.CommandCreator
import definitions.ErrorHandler
import definitions.globals.TrustedServers
import dev.minn.jda.ktx.interactions.commands.option
import dev.minn.jda.ktx.interactions.commands.restrict
import dev.minn.jda.ktx.interactions.commands.subcommand
import dev.minn.jda.ktx.interactions.components.getOption
import dev.minn.jda.ktx.messages.reply_
import dev.minn.jda.ktx.messages.send
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import kotlin.text.toLongOrNull

class TrustedHandler(private val jda: JDA, commandCreator: CommandCreator) {
	companion object {
		private const val TRUSTED_COMMAND = "trusted"
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
				restrict(guild = true, perm = Permission.MANAGE_SERVER)
				subcommand(SHOW_SUBCOMMAND, "Show list of trusted and non-trusted servers") {
					option<String>("src", "(tmp) src ID", required = true)
				}
				// TODO: Remove
				subcommand("who", "Show who trusts a given server") {
					option<String>("server_id", "Server ID", required = true)
				}
				subcommand(ADD_SUBCOMMAND, "Add a server to the trusted list") {
					option<String>("src", "(tmp) src ID", required = true)
					option<String>("server_id", "ID of the server to add", required = true)
				}
				subcommand(REMOVE_SUBCOMMAND, "Remove a server from the trusted list") {
					option<String>("src", "(tmp) src ID", required = true)
					option<String>("server_id", "ID of the server to remove", required = true)
				}
			}
		}
	}

	private fun runTrustedCommand(event: GenericCommandInteractionEvent) {
		when (event.subcommandName) {
			SHOW_SUBCOMMAND -> runShowSubcommand(event)
			"who" -> runWhoSubcommand(event)
			ADD_SUBCOMMAND -> runAddSubcommand(event)
			REMOVE_SUBCOMMAND -> runRemoveSubcommand(event)
		}
	}

	private fun runShowSubcommand(event: GenericCommandInteractionEvent) {
		// TODO: Remove
		val serverId = event.getOption<String>("src")!!.toLongOrNull()
		if (serverId == null) {
			event.reply_("Error: The provided server ID is not valid.", ephemeral = true).queue()
			return
		}

		// TODO: Proper implementation
		val trustedServers = TrustedServers.get().getTrustedBy(serverId)
		var msg = "Trusted servers for $serverId:"  // TODO: Don't forget the server name here
		for (serverId in trustedServers) {
			msg += "\n- $serverId"
		}
		event.reply_(msg).queue()
	}

	// TODO: Remove
	private fun runWhoSubcommand(event: GenericCommandInteractionEvent) {
		val serverId = event.getOption<String>("server_id")!!.toLongOrNull()
		if (serverId == null) {
			event.reply_("Error: The provided server ID is not valid.", ephemeral = true).queue()
			return
		}

		val trustedServers = TrustedServers.get().getWhoTrusts(serverId)
		var msg = "Servers who trust $serverId:"
		for (serverId in trustedServers) {
			msg += "\n- $serverId"
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
		// TODO: Remove
		val src = event.getOption<String>("src")!!.toLongOrNull()
		if (src == null) {
			event.reply_("Error: The provided src ID is not valid.", ephemeral = true).queue()
			return
		}

		val serverId = event.getOption<String>("server_id")!!.toLongOrNull()
		if (serverId == null) {
			event.reply_("Error: The provided server ID is not valid.", ephemeral = true).queue()
			return
		}

		event.deferReply().queue()
		try {
			TrustedServers.get().setTrust(src, serverId, add)
			event.hook.send("Successfully ${if (add) "added" else "removed"} the server to the trusted list.").queue()
		} catch (e: Exception) {
			with(ErrorHandler(e)) {
				printToErrorChannel(jda, event.guild)
				replyDeferred(event)
			}
		}
	}
}