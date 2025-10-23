package handlers

import CommandErrorException
import definitions.Command
import definitions.CommandCreator
import definitions.ErrorHandler
import definitions.GlobalActionRunner
import definitions.TRUSTED_CMD_PERMISSION
import definitions.globals.Settings
import definitions.globals.TrustedServers
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.commands.option
import dev.minn.jda.ktx.interactions.commands.restrict
import dev.minn.jda.ktx.interactions.components.button
import dev.minn.jda.ktx.interactions.components.getOption
import dev.minn.jda.ktx.messages.Mentions
import dev.minn.jda.ktx.messages.reply_
import dev.minn.jda.ktx.messages.send
import getLogger
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import nothing
import utils.canUseCommand
import utils.getCommandByName
import utils.servers
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

/**
 * Handles the global ban command
 */
class BanHandler(private val jda: JDA, commandCreator: CommandCreator) {
	companion object {
		private const val GLOBAL_BAN_COMMAND = "gban"
	}

	private val logger = getLogger(this::class)

	init {
		with(commandCreator) {
			slash(
				GLOBAL_BAN_COMMAND,
				"Send a global ban request for a user",
				Command(::runGlobalBanCommand)
			) {
				restrict(guild = true, perm = Permission.BAN_MEMBERS)
				option<User>("user", "User to ban globally", required = true)
				option<String>("reason", "Reason for global ban", required = true)
			}
		}
	}

	private fun runGlobalBanCommand(event: GenericCommandInteractionEvent) {
		val issuer = event.user
		val sourceServer = event.guild!!
		val targetUser = event.getOption<User>("user")!!
		val reason = event.getOption<String>("reason")!!

		val serverIds = servers(jda).map { it.idLong }.toList()
		val sourceTrustedBy = TrustedServers.getWhoTrusts(sourceServer.idLong)

		event.deferReply().queue()
		try {
			val results = GlobalActionRunner<BanRequestResult>(serverIds).run {
				serverId -> sendBanRequest(issuer, sourceServer, targetUser, reason, sourceTrustedBy, serverId)
			}

			val successCount = results.filterValues { it == BanRequestResult.SUCCESS }.size
			val nonTrustedCount = results.filterValues { it == BanRequestResult.NON_TRUSTED_ALERT }.size
			val fallbackAlertCount = results.filterValues { it == BanRequestResult.ERROR_FALLBACK_ALERT }.size
			val failureCount = results.filterValues { it == BanRequestResult.ERROR_FAILURE }.size
			var msg = ""

			msg += ":white_check_mark: Successfully banned **${targetUser.name}** across **$successCount** " +
				"server${if (successCount == 1) "" else "s"}."
			if (nonTrustedCount > 0) {
				msg += "\n- :information: $nonTrustedCount server${if (nonTrustedCount == 1) "" else "s"} did not " +
					"automatically apply the ban because your server isn't in their trusted list. An alert was sent " +
					"for them instead."
			}
			if (fallbackAlertCount > 0) {
				msg += "\n- :x: $fallbackAlertCount server${if (fallbackAlertCount == 1) "" else "s"} failed to " +
					"apply the ban due to an error. An alert was sent for them instead."
			}
			if (failureCount > 0) {
				msg += "\n- :no_entry: $failureCount server${if (failureCount == 1) "" else "s"} failed to apply " +
					"the ban and did not recieve a fallback alert either due to multiple errors."
			}

			event.hook.send(msg).queue()
		} catch (e: Exception) {
			with(ErrorHandler(e)) {
				printToErrorChannel(jda, event.guild)
				replyDeferred(event)
			}
		}
	}

	private suspend fun sendBanRequest(issuer: User, sourceServer: Guild, targetUser: User,
		reason: String, sourceTrustedBy: List<Long>, targetServerId: Long): BanRequestResult {
		val server = jda.getGuildById(targetServerId)
		if (server == null) {
			logger.error("Cannot find server from ID $targetServerId")
			return BanRequestResult.ERROR_FAILURE
		}

		val trusted = sourceTrustedBy.contains(targetServerId)
		var banSuccess = false

		if (trusted) {
			try {
				server.ban(targetUser, 0, TimeUnit.SECONDS)
					.reason("Global ban by ${issuer.name} from ${sourceServer.name}: $reason").await()
				banSuccess = true
			} catch (e: Exception) {
				with(ErrorHandler(e)) {
					printToErrorChannel(jda, server)
				}
			}
		}

		val channelId = Settings.get().getForServer(targetServerId)?.notificationsChannelId ?:
			return BanRequestResult.fromSteps(trusted, banSuccess)
		val channel = server.getTextChannelById(channelId)
		if (channel == null) {
			logger.error("Channel $channelId in server $targetServerId is not a text channel. Cannot send " +
				"global ban message.")
			return BanRequestResult.fromSteps(trusted, banSuccess)
		}

		if (trusted) {
			if (banSuccess) {
				// Send message notifying the ban
				try {
					val msg = ":hammer: Global ban issued by <@${issuer.idLong}> (${issuer.name}) " +
						"from *${sourceServer.name}* for <@${targetUser.idLong}> (${targetUser.name}): $reason" +
						"\n - :white_check_mark: This ban has been automatically applied in your server."
					channel.send(msg, mentions = Mentions.nothing()).await()
				} catch (e: Exception) {
					with (ErrorHandler(e)) {
						printToErrorChannel(jda, server)
					}
				}
				return BanRequestResult.fromSteps(trusted = true, banSuccess = true)
			} else {
				// Try to send a fallback alert
				try {
					val msg = ":hammer: Global ban issued by <@${issuer.idLong}> (${issuer.name}) " +
						"from *${sourceServer.name}* for <@${targetUser.idLong}> (${targetUser.name}): $reason" +
						"\n - :warning: This ban has **not** been automatically applied in your server because an " +
						"error occurred when trying to apply the ban. Check that the bot has the ban members " +
						"permission. You can try to apply the ban again by clicking the below button."
					val applyButton = jda.button(ButtonStyle.PRIMARY, "Apply ban", expiration = Duration.INFINITE) {
						event -> handleApplyBanButton(event, issuer, sourceServer, targetUser, reason)
					}
					channel.send(msg, mentions = Mentions.nothing()).addActionRow(applyButton).await()
					return BanRequestResult.fromSteps(trusted = true, banSuccess = false, alertSuccess = true)
				} catch (e: Exception) {
					with (ErrorHandler(e)) {
						printToErrorChannel(jda, server)
					}
					return BanRequestResult.fromSteps(trusted = true, banSuccess = false, alertSuccess = false)
				}
			}
		} else {
			// Try to send an alternative alert
			try {
				val msg = ":hammer: Global ban issued by <@${issuer.idLong}> (${issuer.name}) " +
					"from *${sourceServer.name}* for <@${targetUser.idLong}> (${targetUser.name}): $reason" +
					"\n - :warning: This ban has **not** been automatically applied in your server because the " +
					"origin server is not on your trusted server list. You can use the buttons below to apply the " +
					"ban, or to apply it and trust the origin server so future bans issued from there are " +
					"automatically applied in your server as well."
				val applyButton = jda.button(ButtonStyle.PRIMARY, "Apply ban", expiration = Duration.INFINITE) {
					event -> handleApplyBanButton(event, issuer, sourceServer, targetUser, reason)
				}
				val applyAndTrustButton = jda.button(ButtonStyle.PRIMARY, "Apply ban and trust server",
					expiration = Duration.INFINITE) {
					event -> handleApplyBanAndTrustButton(event, issuer, sourceServer, targetUser, reason)
				}
				channel.send(msg, mentions = Mentions.nothing()).addActionRow(applyButton, applyAndTrustButton).await()
				return BanRequestResult.fromSteps(trusted = false, banSuccess = false, alertSuccess = true)
			} catch (e: Exception) {
				with (ErrorHandler(e)) {
					printToErrorChannel(jda, server)
				}
				return BanRequestResult.fromSteps(trusted = false, banSuccess = false, alertSuccess = false)
			}
		}
	}


	private fun handleApplyBanButton(event: ButtonInteractionEvent, issuer: User, sourceServer: Guild, targetUser: User,
		reason: String) {
		val server = event.guild!!
		val member = event.member!!

		if (!member.hasPermission(Permission.BAN_MEMBERS)) {
			event.reply_("<@${member.idLong}> You don't have permission to ban members in this server.",
				mentions = Mentions.nothing()).queue()
			return
		}

		event.deferReply().queue()
		try {
			server.ban(targetUser, 0, TimeUnit.SECONDS)
				.reason("Global ban by ${issuer.name} from ${sourceServer.name}: $reason").complete()
			event.hook.send("Ban successfully applied manually by <@${member.idLong}>.", mentions = Mentions.nothing())
				.queue()
			event.editButton(jda.button(ButtonStyle.PRIMARY, "Ban applied",
				emoji = Emoji.fromUnicode("✅"), disabled = true, listener = {})).queue()
		} catch (e: Exception) {
			with(ErrorHandler(e)) {
				printToErrorChannel(jda, server)
				replyDeferred(event)
			}
		}
	}


	private fun handleApplyBanAndTrustButton(event: ButtonInteractionEvent, issuer: User, sourceServer: Guild,
		targetUser: User, reason: String) {
		val server = event.guild!!
		val member = event.member!!

		if (!member.hasPermission(Permission.BAN_MEMBERS)) {
			event.reply_("<@${member.idLong}> You don't have permission to ban members in this server.",
				mentions = Mentions.nothing()).queue()
			return
		}

		event.deferReply().queue()
		try {
			val command = getCommandByName(jda, TrustedHandler.TRUSTED_COMMAND, server)
				?: throw CommandErrorException("Cannot find command /${TrustedHandler.TRUSTED_COMMAND}")

			if (!canUseCommand(member, server, command, TRUSTED_CMD_PERMISSION, event.guildChannel)) {
				event.hook.send("<@${member.idLong}> You don't have permission to modify the trusted server list.",
					mentions = Mentions.nothing()).queue()
				return
			}

			server.ban(targetUser, 0, TimeUnit.SECONDS)
				.reason("Global ban by ${issuer.name} from ${sourceServer.name}: $reason").complete()
			TrustedServers.setTrust(server.idLong, sourceServer.idLong, true)

			event.hook.send("Ban successfully applied manually by <@${member.idLong}>. The origin server has also " +
				"been added to the trusted server list.", mentions = Mentions.nothing()).queue()
			event.editButton(jda.button(ButtonStyle.PRIMARY, "Applied and trusted",
				emoji = Emoji.fromUnicode("✅"), disabled = true, listener = {})).queue()
		} catch (e: Exception) {
			with(ErrorHandler(e)) {
				printToErrorChannel(jda, server)
				replyDeferred(event)
			}
		}
	}

	/**
	 * Represents the possible outcomes of a ban request in a server
	 */
	enum class BanRequestResult {
		/** User successfully banned */
		SUCCESS,
		/** Target does not trust source, an alert message was sent instead */
		NON_TRUSTED_ALERT,
		/** User could not be banned due to an error, an alert message was sent instead */
		ERROR_FALLBACK_ALERT,
		/**
		 * User could not be banned due to an error, the fallback alert could not be posted due to an error. Nothing was done.
		 */
		ERROR_FAILURE;

		companion object {
			/**
			 * Returns the BanRequestResult that should be used as the result of a ban request depending on which
			 * steps succeeded.
			 * @param trusted True if the destination server for the request trusts the source server
			 * @param banSuccess True if banning the user on the destination server succeeded
			 * @param alertSuccess True if the destination server did not trust the source, but an alternative alert was
			 * successfully issued instead, or if the ban failed but a fallback alert was successfully sent instead.
			 */
			fun fromSteps(trusted: Boolean, banSuccess: Boolean = false, alertSuccess: Boolean = false): BanRequestResult {
				return if (trusted) {
					if (banSuccess) {
						SUCCESS
					} else {
						if (alertSuccess) {
							ERROR_FALLBACK_ALERT
						} else {
							ERROR_FAILURE
						}
					}
				} else {
					if (alertSuccess) {
						NON_TRUSTED_ALERT
					} else {
						ERROR_FAILURE
					}
				}
			}
		}
	}
}