import definitions.CommandCreator
import dev.minn.jda.ktx.jdabuilder.light
import features.TestCommands
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.internal.utils.JDALogger

class Main {
    val logger = getLogger(Main::class)

    @Suppress("unused")
    fun main() {
        JDALogger.setFallbackLoggerEnabled(false)
        val jda = light(Env.token)
        // TODO: DB connection

        val commandCreator = CommandCreator(jda)

        // Create feature implementation classes
        TestCommands(jda, commandCreator)

        // Register all commands
        commandCreator.register()

        logger.info("Bot started. Invite link: ${jda.getInviteUrl(Permission.BAN_MEMBERS)}")
    }
}

fun main() {
    Main().main()
}