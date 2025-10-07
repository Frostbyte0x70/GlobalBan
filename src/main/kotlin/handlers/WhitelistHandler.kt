package handlers

import definitions.Command
import definitions.CommandCreator
import definitions.globals.Whitelist
import dev.minn.jda.ktx.interactions.commands.option
import dev.minn.jda.ktx.interactions.commands.restrict
import dev.minn.jda.ktx.interactions.commands.subcommand
import dev.minn.jda.ktx.interactions.components.getOption
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent

/**
 * Handles whitelist-related commands
 * TODO: Test
 */
class WhitelistHandler(val jda: JDA, commandCreator: CommandCreator, val whitelist: Whitelist) {
    companion object {
        private const val WHITELIST_COMMAND = "whitelist"
        private const val SHOW_SUBCOMMAND = "show"
        private const val ADD_SUBCOMMAND = "add"
        private const val REMOVE_SUBCOMMAND = "remove"
    }

    init {
        with (commandCreator) {
            slash (
                WHITELIST_COMMAND,
                "(Owner only) Manage the list of servers the bot can be added to",
                Command(::runWhitelistCommand, mainServerOnly = true)
            ) {
                restrict(guild=true, perm = Permission.ADMINISTRATOR)
                subcommand(SHOW_SUBCOMMAND, "Show whitelisted servers")
                subcommand(ADD_SUBCOMMAND, "Add a server to the whitelist") {
                    option<Long>("server_id", "ID of the server to add", required = true)
                }
                subcommand(REMOVE_SUBCOMMAND, "Remove a server from the whitelist") {
                    option<Long>("server_id", "ID of the server to remove", required = true)
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
        var text = "Whitelisted servers:\n"
        for (serverId in Whitelist.get().getAllServers()) {
            text += "- ${(jda.getGuildById(serverId)?.name?.plus(" ")) ?: ""}($serverId)"
        }
        event.reply_(text.removeSuffix("\n")).queue()
    }

    private fun runAddSubcommand(event: GenericCommandInteractionEvent) {
        event.deferReply(true)
        try {
            Whitelist.get().add(event.getOption<Long>("server_id")!!)
            event.reply_("Successfully added the server to the whitelist").queue()
        } catch (_: Exception) {
            // TODO: ErrorHandler class
            event.reply_("Error").queue()
        }
    }

    private fun runRemoveSubcommand(event: GenericCommandInteractionEvent) {
        event.deferReply(true)
        val serverId = event.getOption<Long>("server_id")!!

        try {
            Whitelist.get().remove(serverId)

            // If the removed server is no longer considered whitelisted, leave it.
            // (It could still be whitelisted if it's the main server)
            if (!Whitelist.get().isWhitelisted(serverId)) {
                jda.getGuildById(serverId)?.leave()?.complete()
            }

            event.reply_("Successfully removed the server from the whitelist").queue()
        } catch (_: Exception) {
            // TODO: ErrorHandler class
            event.reply_("Error").queue()
        }
    }
}