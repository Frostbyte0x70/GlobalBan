package definitions

import definitions.globals.Env
import dev.minn.jda.ktx.events.onCommand
import dev.minn.jda.ktx.interactions.commands.slash
import getLogger
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction

/**
 * Class used to bulk register commands and their listeners.
 */
class CommandCreator(private val jda: JDA) {
	private val logger = getLogger(this::class)

	/**
	 * Used to register global commands. Null if command test mode is enabled (global commands are registered for the
	 * main server instead)
	 */
	private val globalUpdateCommandsAction = if (Env.commandTestMode) null else jda.updateCommands()

	/**
	 * Used to register commands for the main server. Null if the bot hasn't joined the main server yet.
	 */
	private val mainServerUpdateCommandsAction: CommandListUpdateAction?

	init {
		val mainServer = jda.getGuildById(Env.mainServerId)
		mainServer ?: logger.warn("Main server not found (maybe because the bot has not been added to it yet). " +
			"Commands will not be registered for the main server.")

		mainServerUpdateCommandsAction = mainServer?.updateCommands()
	}

	/**
	 * Adds a slash command to be bulk registered later.
	 * @param commandObj Corresponding [Command] object
	 * @param jdaBuilder Code used to initialize the [SlashCommandData] of the JDA command
	 */
	fun slash(name: String, description: String, commandObj: Command, jdaBuilder: SlashCommandData.() -> Unit) {
		val updateAction = if (commandObj.mainServerOnly || globalUpdateCommandsAction == null) {
			mainServerUpdateCommandsAction
		} else {
			globalUpdateCommandsAction
		}

		if (updateAction != null) {
			updateAction.slash(name, description, jdaBuilder)
			jda.onCommand(name) { event -> commandObj.run(event) }
		}
	}

	/**
	 * Bulk registers all the previously added commands. All previously registered commands will
	 * be overwritten by the new batch.
	 */
	fun register() {
		globalUpdateCommandsAction?.queue()
		mainServerUpdateCommandsAction?.queue()
	}
}