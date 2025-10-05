package definitions

import dev.minn.jda.ktx.events.onCommand
import dev.minn.jda.ktx.interactions.commands.slash
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

/**
 * Class used to bulk register commands and their listeners.
 */
class CommandCreator(val jda: JDA) {
    val updateCommandsAction = jda.updateCommands()

    /**
     * Adds a slash command to be bulk registered later.
     * @param commandObj Corresponding [Command] object
     * @param jdaBuilder Code used to initialize the [SlashCommandData] of the JDA command
     */
    fun slash(name: String, description: String, commandObj: Command, jdaBuilder: SlashCommandData.() -> Unit) {
        updateCommandsAction.slash(name, description, jdaBuilder)
        jda.onCommand(name) {event -> commandObj.run(event)}
    }

    /**
     * Bulk registers all the previously added commands as global commands. All previously registered commands will
     * be overwritten by the new batch.
     */
    fun register() {
        updateCommandsAction.queue()
    }
}