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
	private val updateCommandsAction: CommandListUpdateAction?

	init {
		if (Env.commandTestMode) {
			val mainServer = jda.getGuildById(Env.mainServerId)

			mainServer ?: logger.warn("Command test mode is enabled, but the main server cannot be found (maybe " +
				"because the bot has not been added to it yet). Commands will not be registered.")
			updateCommandsAction = mainServer?.updateCommands()
		} else {
			updateCommandsAction = jda.updateCommands()
		}
	}

	/**
	 * Adds a slash command to be bulk registered later.
	 * @param commandObj Corresponding [Command] object
	 * @param jdaBuilder Code used to initialize the [SlashCommandData] of the JDA command
	 */
	fun slash(name: String, description: String, commandObj: Command, jdaBuilder: SlashCommandData.() -> Unit) {
		if (updateCommandsAction != null) {
			updateCommandsAction.slash(name, description, jdaBuilder)
			jda.onCommand(name) { event -> commandObj.run(event) }
		}
	}

	/**
	 * Bulk registers all the previously added commands. All previously registered commands will
	 * be overwritten by the new batch.
	 */
	fun register() {
		updateCommandsAction?.queue()

		if (!Env.commandTestMode) {
			// Clear local commands for the main server
			val mainServer = jda.getGuildById(Env.mainServerId)

			mainServer ?: logger.warn("Cannot find main server. Local commands for the server will not be cleared.")
			mainServer?.updateCommands()?.queue()
		}
	}
}