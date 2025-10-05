package definitions

import dev.minn.jda.ktx.messages.reply_
import features.GlobalListeners
import getLogger
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent

/**
 * Wraps the execution of commands. Contains some tasks and checks common to all commands, such as checking if
 * the command can be used in the current server.
 * If the checks fail, an error message will be displayed to the user who ran the command.
 *
 * @param innerCall Function to run if the checks pass. Receives a [GenericCommandInteractionEvent], returns nothing.
 * @param mainServerOnly True if the command can only be used in the main server set in the bot config
 */
class Command(private val innerCall: (GenericCommandInteractionEvent) -> Unit, val mainServerOnly: Boolean = false) {
    val logger = getLogger(GlobalListeners::class)

    fun run(event: GenericCommandInteractionEvent) {
        logger.info("Command run by ${event.user.name} on ${event.guild?.name ?: "DMs"}: ${event.commandString}")

        val serverId = event.guild?.idLong

        if (serverId != null) {
            // TODO: Check if server is whitelisted, reject command if not

            if (mainServerOnly && serverId != Env.mainServerId) {
                event.reply_("Error: This command can only be used in the main server.").queue()
                return
            }
        }

        innerCall(event)
    }
}