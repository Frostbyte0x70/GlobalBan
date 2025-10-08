package handlers

import definitions.Command
import definitions.CommandCreator
import definitions.ErrorHandler
import definitions.globals.Settings
import dev.minn.jda.ktx.interactions.commands.option
import dev.minn.jda.ktx.interactions.commands.restrict
import dev.minn.jda.ktx.interactions.commands.subcommand
import dev.minn.jda.ktx.interactions.components.getOption
import dev.minn.jda.ktx.messages.reply_
import dev.minn.jda.ktx.messages.send
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent

/**
 * Handles the settings command
 */
class SettingsHandler(private val jda: JDA, commandCreator: CommandCreator) {
	companion object {
		private const val SETTINGS_COMMAND = "settings"
		private const val SHOW_SUBCOMMAND = "show"
		private const val NOTIFICATIONS_SUBCOMMAND = "notifications"
	}

	init {
		with(commandCreator) {
			slash(
				SETTINGS_COMMAND,
				"View or edit the settings for this server",
				Command(::runSettingslistCommand)
			) {
				restrict(guild = true, perm = Permission.MANAGE_SERVER)
				subcommand(SHOW_SUBCOMMAND, "Show current settings")
				subcommand(NOTIFICATIONS_SUBCOMMAND, "Set notifications channel") {
					option<GuildChannel>("channel", "Channel to send notifications to, omit to disable")
				}
			}
		}
	}

	private fun runSettingslistCommand(event: GenericCommandInteractionEvent) {
		when (event.subcommandName) {
			SHOW_SUBCOMMAND -> runShowSubcommand(event)
			NOTIFICATIONS_SUBCOMMAND -> runNotificationsSubcommand(event)
		}
	}

	private fun runShowSubcommand(event: GenericCommandInteractionEvent) {
		val server = event.guild!!
		val settings = Settings.get().getForServer(server.idLong)

		val msg = "Settings for ${server.name}:\n" +
			"- Notifications channel: ${settings?.notificationsChannelId?.let { "<#$it>" } ?: "None"}"

		event.reply_(msg).queue()
	}

	private fun runNotificationsSubcommand(event: GenericCommandInteractionEvent) {
		val server = event.guild!!
		val settings = Settings.get().getOrCreateForServer(server.idLong)

		val newChannel = event.getOption<GuildChannel>("channel")
		val newId = newChannel?.idLong

		event.deferReply().queue()
		try {
			if (newChannel != null) {
				if (server.getTextChannelById(newChannel.idLong) == null) {
					event.hook.send("Error: The provided channel is not a text channel.").queue()
					return
				}
				if (!server.selfMember.hasAccess(newChannel) ||
					!server.selfMember.hasPermission(newChannel, Permission.MESSAGE_SEND)) {
					event.hook.send("Error: I don't have permission to view or write to the specified channel.").queue()
					return
				}
			}

			settings.notificationsChannelId = newId

			val message = newId?.let { "Notifications channel set to <#$it>." } ?: "Notifications have been disabled."
			event.hook.send(message).queue()
		} catch (e: Exception) {
			with(ErrorHandler(e)) {
				printToErrorChannel(jda, event.guild)
				replyDeferred(event)
			}
		}
	}
}