package definitions

import definitions.globals.Env
import dev.minn.jda.ktx.messages.reply_
import dev.minn.jda.ktx.messages.send
import getLogger
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.callbacks.IDeferrableCallback
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import throwableToStr
import truncateLines

/**
 * Used to deal with exceptions thrown while the bot runs. Will print the error to the console, plus any additional
 * operations specified by the user, such as replying with an error message, or printing the error in a Discord
 * channel.
 */
class ErrorHandler(error: Throwable) {
	companion object {
		private const val DEFAULT_ERROR_MESSAGE = "An error occurred while running this command."
	}

	private val logger = getLogger(this::class)

	private val errorStr = throwableToStr(error)

	init {
		logger.error(errorStr)
	}

	/**
	 * Replies to the [interaction] with the given error message. The message will be ephemeral by default.
	 * If an error message is not given, the [DEFAULT_ERROR_MESSAGE] will be sent.
	 * Can only be used for direct replies. If deferReply has already been run, use [replyDeferred] instead.
	 */
	fun reply(interaction: IReplyCallback, message: String? = null, ephemeral: Boolean = true) {
		interaction.reply_(message ?: DEFAULT_ERROR_MESSAGE, ephemeral = ephemeral).queue()
	}

	/**
	 * Replies with the given error message by editing the deferred reply message.
	 * If an error message is not given, the [DEFAULT_ERROR_MESSAGE] will be sent.
	 * Can only be called after deferReply has been used.
	 */
	fun replyDeferred(interaction: IDeferrableCallback, message: String? = null) {
		interaction.hook.send(message ?: DEFAULT_ERROR_MESSAGE).queue()
	}

	/**
	 * Prints the error to the error channel within the main server set in the environment config.
	 * If no error channel is set, does nothing.
	 * If an [originServer] is specified, the name and ID of the server will be included in the error message.
	 */
	fun printToErrorChannel(jda: JDA, originServer: Guild? = null) {
		val channelId = Env.mainErrorChannel ?: return
		val channel = jda.getGuildById(Env.mainServerId)?.getTextChannelById(channelId)

		if (channel == null) {
			logger.warn("Cannot find a text channel for server ${Env.mainServerId} and channel ID $channelId")
			return
		}

		var msg = ""
		if (originServer != null) {
			msg += "${originServer.name} (${originServer.id})\n"
		}

		msg += "```${truncateLines(errorStr, MAX_DISCORD_MESSAGE_LENGTH - 6 - msg.length)}```"
		channel.sendMessage(msg).queue(null) {
			// If this fails, simply print the error to the console (the default is to print it to the console and
			// to the main error channel, which could cause an infinite loop, since we're in the function that does
			// just that).
			e -> ErrorHandler(e)
		}
	}

}