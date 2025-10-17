package handlers

import definitions.Command
import definitions.CommandCreator
import definitions.ErrorHandler
import definitions.GlobalActionRunner
import definitions.globals.Settings
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.commands.option
import dev.minn.jda.ktx.interactions.commands.restrict
import dev.minn.jda.ktx.interactions.components.getOption
import dev.minn.jda.ktx.messages.Mentions
import dev.minn.jda.ktx.messages.send
import getLogger
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import nothing

/**
 * Handles the alert command
 */
class AlertHandler(private val jda: JDA, commandCreator: CommandCreator) {
	companion object {
		private const val ALERT_COMMAND = "alert"
	}

	private val logger = getLogger(this::class)

	init {
		with(commandCreator) {
			slash(
				ALERT_COMMAND,
				"Send a global alert about a user",
				Command(::runAlertCommand)
			) {
				restrict(guild = true, perm = Permission.BAN_MEMBERS)
				option<User>("user", "User to send the alert about", required = true)
				option<String>("message", "Alert message", required = true)
			}
		}
	}

	private fun runAlertCommand(event: GenericCommandInteractionEvent) {
		val issuer = event.user
		val sourceServerName = event.guild!!.name
		val targetUser = event.getOption<User>("user")!!
		val message = event.getOption<String>("message")!!

		val serverIds = jda.guilds.map { it.idLong }.toList()

		event.deferReply().queue()
		try {
			val results = GlobalActionRunner<Boolean>(serverIds).run {
				serverId -> sendServerAlert(issuer, sourceServerName, targetUser, message, serverId)
			}

			val successCount = results.filterValues { it }.size
			val failureCount = results.filterValues { !it }.size
			var msg = ""

			msg += ":white_check_mark: Successfully sent an alert for **${targetUser.name}** across **$successCount** " +
				"server${if (successCount == 1) "" else "s"}."
			if (failureCount > 0) {
				msg += "\n- :warning: The alert was not sent on $failureCount " +
					"server${if (failureCount == 1) "" else "s"} due to errors."
			}

			event.hook.send(msg).queue()
		} catch (e: Exception) {
			with(ErrorHandler(e)) {
				printToErrorChannel(jda, event.guild)
				replyDeferred(event)
			}
		}
	}

	private suspend fun sendServerAlert(issuer: User, sourceServerName: String, targetUser: User,
		message: String, targetServerId: Long): Boolean {
		val server = jda.getGuildById(targetServerId)
		if (server == null) {
			logger.error("Cannot find server from ID $targetServerId")
			return false
		}

		val channelId = Settings.get().getForServer(targetServerId)?.notificationsChannelId ?: return false
		val channel = server.getTextChannelById(channelId)
		if (channel == null) {
			logger.error("Channel $channelId in server $targetServerId is not a text channel. Cannot send alert message.")
			return false
		}

		try {
			val msg = ":loudspeaker: Global alert issued by <@${issuer.idLong}> (${issuer.name}) " +
				"from *$sourceServerName* about <@${targetUser.idLong}> (${targetUser.name}): $message"
			channel.send(msg, mentions = Mentions.nothing()).await()
			return true
		} catch (e: Exception) {
			with (ErrorHandler(e)) {
				printToErrorChannel(jda, server)
			}
			return false
		}
	}
}