package handlers

import definitions.Command
import definitions.CommandCreator
import definitions.ErrorHandler
import definitions.globals.Env
import definitions.globals.Whitelist
import dev.minn.jda.ktx.interactions.commands.option
import dev.minn.jda.ktx.interactions.commands.restrict
import dev.minn.jda.ktx.interactions.commands.subcommand
import dev.minn.jda.ktx.interactions.components.getOption
import dev.minn.jda.ktx.messages.reply_
import dev.minn.jda.ktx.messages.send
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent

/**
 * Handles whitelist-related commands
 */
class WhitelistHandler(private val jda: JDA, commandCreator: CommandCreator) {
	companion object {
		private const val WHITELIST_COMMAND = "whitelist"
		private const val SHOW_SUBCOMMAND = "show"
		private const val ADD_SUBCOMMAND = "add"
		private const val REMOVE_SUBCOMMAND = "remove"
	}

	init {
		with(commandCreator) {
			slash(
				WHITELIST_COMMAND,
				"(Owner only) Manage the list of servers the bot can be added to",
				Command(::runWhitelistCommand, mainServerOnly = true)
			) {
				restrict(guild = true, perm = Permission.ADMINISTRATOR)
				subcommand(SHOW_SUBCOMMAND, "Show whitelisted servers")
				subcommand(ADD_SUBCOMMAND, "Add a server to the whitelist") {
					option<String>("server_id", "ID of the server to add", required = true)
				}
				subcommand(REMOVE_SUBCOMMAND, "Remove a server from the whitelist") {
					option<String>("server_id", "ID of the server to remove", required = true)
				}
			}
		}
	}

	private fun runWhitelistCommand(event: GenericCommandInteractionEvent) {
		when (event.subcommandName) {
			SHOW_SUBCOMMAND -> runShowSubcommand(event)
			ADD_SUBCOMMAND -> runAddSubcommand(event)
			REMOVE_SUBCOMMAND -> runRemoveSubcommand(event)
		}
	}

	private fun runShowSubcommand(event: GenericCommandInteractionEvent) {
		val servers = Whitelist.get().getAllServers()
		var text = "Whitelisted servers:\n"

		if (!servers.contains(Env.mainServerId)) {
			// Main server is implicitly whitelisted
			text += "- ${(jda.getGuildById(Env.mainServerId)?.name?.plus(" ")) ?: ""}(${Env.mainServerId}) " +
				"(Implicit - Main server)\n"
		}

		for (serverId in servers) {
			text += "- ${(jda.getGuildById(serverId)?.name?.plus(" ")) ?: ""}($serverId)\n"
		}
		event.reply_(text.removeSuffix("\n")).queue()
	}

	private fun runAddSubcommand(event: GenericCommandInteractionEvent) {
		val serverId = event.getOption<String>("server_id")!!.toLongOrNull()
		if (serverId == null) {
			event.reply_("Error: The provided server ID is not valid.", ephemeral = true).queue()
			return
		}

		event.deferReply(true).queue()
		try {
			Whitelist.get().add(serverId)
			event.hook.send("Successfully added the server to the whitelist.").queue()
		} catch (e: Exception) {
			with(ErrorHandler(e)) {
				printToErrorChannel(jda, event.guild)
				replyDeferred(event)
			}
		}
	}

	private fun runRemoveSubcommand(event: GenericCommandInteractionEvent) {
		val serverId = event.getOption<String>("server_id")!!.toLongOrNull()
		if (serverId == null) {
			event.reply_("Error: The provided server ID is not valid.", ephemeral = true).queue()
			return
		}

		event.deferReply(true).queue()
		try {
			Whitelist.get().remove(serverId)

			// If the removed server is no longer considered whitelisted, leave it.
			// (It could still be whitelisted if it's the main server)
			if (!Whitelist.get().isWhitelisted(serverId)) {
				jda.getGuildById(serverId)?.leave()?.complete()
			}

			event.hook.send("Successfully removed the server from the whitelist.").queue()
		} catch (e: Exception) {
			with(ErrorHandler(e)) {
				printToErrorChannel(jda, event.guild)
				replyDeferred(event)
			}
		}
	}
}