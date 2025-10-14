package handlers

import CommandErrorException
import definitions.ErrorHandler
import definitions.GlobalActionRunner
import definitions.ServerSettings.Companion.TRUST_NEW_SERVERS_DEFAULT
import definitions.TRUSTED_CMD_PERMISSION
import definitions.globals.Env
import definitions.globals.Settings
import definitions.globals.TrustedServers
import definitions.globals.Whitelist
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.interactions.components.button
import dev.minn.jda.ktx.messages.send
import getLogger
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import utils.canUseCommand
import utils.getCommandByName
import kotlin.time.Duration

/**
 * Implements tasks that should be run when the bot joins a server
 */
class ServerJoinHandler(private val jda: JDA) {
	private val logger = getLogger(this::class)

	init {
		jda.listener<GuildJoinEvent> { event ->
			// Leave joined guilds if they are not whitelisted
			if (!Whitelist.get().isWhitelisted(event.guild.idLong)) {
				logger.info("Leaving server '${event.guild.name} because it's not whitelisted'")
				event.guild.leave().queue()
			}

			// Mark main server as trusted
			try {
				TrustedServers.setTrust(event.guild.idLong, Env.mainServerId, true)
			} catch (e: Exception) {
				with (ErrorHandler(e)) {
					printToErrorChannel(jda, event.guild)
				}
			}

			// Send alert to all servers
			val serverIds = jda.guilds.map { it.idLong }.toList()
			GlobalActionRunner<Unit>(serverIds).run {
				serverId -> handleNewServer(event.guild, serverId)
			}
		}
	}

	/**
	 * Handles the addition of [newServer] to the server list for [targetServerId]. A message will be sent in
	 * [targetServerId], and the new server will be added to its trusted list if the "trust new servers" option is
	 * enabled in its config.
	 */
	private suspend fun handleNewServer(newServer: Guild, targetServerId: Long) {
		val trust = Settings.getForServer(targetServerId)?.trustNewServers ?: TRUST_NEW_SERVERS_DEFAULT
		var nowTrusted = false
		if (trust) {
			try {
				TrustedServers.setTrust(targetServerId, newServer.idLong, true)
				nowTrusted = true
			} catch (e: Exception) {
				with (ErrorHandler(e)) {
					printToErrorChannel(jda, jda.getGuildById(targetServerId))
				}
			}
		}

		val server = jda.getGuildById(targetServerId)
		if (server == null) {
			logger.error("Cannot find server from ID $targetServerId")
			return
		}

		val channelId = Settings.get().getForServer(targetServerId)?.notificationsChannelId ?: return
		val channel = server.getTextChannelById(channelId)
		if (channel == null) {
			logger.error("Channel $channelId in server $targetServerId is not a text channel. Cannot send new " +
				"server message.")
			return
		}

		try {
			var msg = ":information: The bot has been added to a new server: ${newServer.name} (${newServer.idLong})"

			if (nowTrusted) {
				msg += "\nSince you have enabled the option to trust new servers by default, this server has been " +
					"added to your trusted list. Global bans issued from this server will be automatically applied in " +
					"your server as well."
				channel.send(msg).await()
			} else {
				msg += "\nIf you wish to automatically apply global bans issued from this server here, add it to your " +
					"trusted server list by pressing the button below."
				val button = jda.button(ButtonStyle.PRIMARY, "Trust server", expiration = Duration.INFINITE) {
					event -> handleTrustServerButton(event, newServer.idLong)
				}

				channel.send(msg).addActionRow(button).await()
			}
		} catch (e: Exception) {
			with (ErrorHandler(e)) {
				printToErrorChannel(jda, server)
			}
		}
	}

	private fun handleTrustServerButton(event: ButtonInteractionEvent, newServerId: Long) {
		val server = event.guild!!

		event.deferReply().queue()
		try {
			val command = getCommandByName(jda, TrustedHandler.TRUSTED_COMMAND, server)
				?: throw CommandErrorException("Cannot find command /${TrustedHandler.TRUSTED_COMMAND}")

			if (!canUseCommand(event.member!!, server, command, TRUSTED_CMD_PERMISSION, event.guildChannel)) {
				event.hook.send("You don't have permission to modify the trusted server list.").queue()
				return
			}

			TrustedServers.setTrust(server.idLong, newServerId, true)
			event.hook.send("Successfully added the new server to the trusted server list.").queue()
			event.editButton(jda.button(ButtonStyle.PRIMARY, "Server trusted",
				emoji = Emoji.fromUnicode("✅"), disabled = true, listener = {})).queue()
		} catch (e: Exception) {
			with (ErrorHandler(e)) {
				replyDeferred(event)
				printToErrorChannel(jda, server)
			}
		}
	}
}