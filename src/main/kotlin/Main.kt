import definitions.CommandCreator
import definitions.db.Database
import definitions.db.WhitelistDB
import definitions.globals.Env
import definitions.globals.Whitelist
import dev.minn.jda.ktx.jdabuilder.light
import handlers.BotStartHandler
import handlers.ServerJoinHandler
import handlers.TestCommandsHandler
import handlers.WhitelistHandler
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.internal.utils.JDALogger

class Main {
    val logger = getLogger(this::class)

    @Suppress("unused")
    fun main() {
        JDALogger.setFallbackLoggerEnabled(false)
        val jda = light(Env.token)

        // Create DB classes
        val db = Database(Env.dbHost, Env.dbPort, Env.dbUser, Env.dbPassword, Env.dbDatabase)
        val wdb = WhitelistDB(db)

        // Create global objects
        val whitelist = Whitelist.create(wdb)

        val commandCreator = CommandCreator(jda)

        // Create handlers
        TestCommandsHandler(jda, commandCreator)
        WhitelistHandler(jda, commandCreator, whitelist)
        ServerJoinHandler(jda)
        BotStartHandler(jda)

        // Register all commands
        commandCreator.register()

        logger.info("Bot started. Invite link: ${jda.getInviteUrl(Permission.BAN_MEMBERS)}")
    }
}

fun main() {
    Main().main()
}