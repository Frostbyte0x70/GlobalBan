package features

import definitions.Command
import definitions.CommandCreator
import dev.minn.jda.ktx.interactions.commands.restrict
import dev.minn.jda.ktx.interactions.components.button
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle

/**
 * Contains a few test commands
 * TODO: Remove eventually
 */
class TestCommands(val jda: JDA, commandCreator: CommandCreator) {
    companion object {
        private const val ENV_TEST_COMMAND = "envtest"
        private const val BUTTON_TEST_COMMAND = "buttontest"
        private const val RESTRICTED_TEST_COMMAND = "restrictedtest"
    }

    init {
        with (commandCreator) {
            slash (
                ENV_TEST_COMMAND,
                "Prints a couple of environment variables",
                Command(::runEnvTestCommand)
            ) {restrict(guild=true)}
            slash (
                BUTTON_TEST_COMMAND,
                "Shows a button that can be pressed",
                Command(::runButtonTestCommand)
            ) {restrict(guild=true)}
            slash (
                RESTRICTED_TEST_COMMAND,
                "Test command that can only be run by admins in the main server",
                Command(::runRestrictedCommand, mainServerOnly = true)
            ) {restrict(guild=true, perm = Permission.ADMINISTRATOR)}
        }
    }

    private fun runEnvTestCommand(event: GenericCommandInteractionEvent) {
        event.reply_("Main server ID: ${Env.mainServerId}, log level: ${Env.logLevel}").queue()
    }

    private fun runButtonTestCommand(event: GenericCommandInteractionEvent) {
        val button = jda.button(ButtonStyle.PRIMARY, "Test button") {event -> handleTestButton(event)}

        event.reply_("A wild button appeared!").addActionRow(button).queue()

        // Components v2 syntax - TODO: Use compoenents v2 once jda-ktx supports it
        // event.reply_("A wild button appeared!").addComponents(ActionRow.of(button))
    }

    private fun handleTestButton(event: ButtonInteractionEvent) {
        event.reply_("The button was pressed!").queue()
    }

    private fun runRestrictedCommand(event: GenericCommandInteractionEvent) {
        event.reply_("If you're seeing this, we're in the main server, and you're an admin!").queue()
    }
}