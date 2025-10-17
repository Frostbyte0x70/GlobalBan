import definitions.CommandCreator
import definitions.ErrorHandler
import definitions.db.Database
import definitions.db.SettingsDB
import definitions.db.TrustedDB
import definitions.db.WhitelistDB
import definitions.globals.Env
import definitions.globals.Settings
import definitions.globals.TrustedServers
import definitions.globals.Whitelist
import dev.minn.jda.ktx.jdabuilder.light
import handlers.AlertHandler
import handlers.BanHandler
import handlers.BotStartHandler
import handlers.ServerJoinHandler
import handlers.SettingsHandler
import handlers.TestCommandsHandler
import handlers.TrustedHandler
import handlers.WhitelistHandler
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.requests.RestAction

class Main {
	private val logger = getLogger(this::class)

	@Suppress("unused")
	fun main() {
		val jda = light(Env.token)

		// Create DB classes
		val db = Database(Env.dbHost, Env.dbPort, Env.dbUser, Env.dbPassword, Env.dbDatabase)
		val wdb = WhitelistDB(db)
		val sdb = SettingsDB(db)
		val tdb = TrustedDB(db)

		// Create global objects
		Whitelist.create(wdb)
		Settings.create(sdb)
		TrustedServers.create(tdb)

		// Wait for JDA to be done before performing any actions that require accessing its cache
		jda.awaitReady()
		RestAction.setDefaultFailure { e -> ErrorHandler(e).printToErrorChannel(jda) }
		val commandCreator = CommandCreator(jda)

		// Create handlers
		BotStartHandler(jda)
		ServerJoinHandler(jda)
		TestCommandsHandler(jda, commandCreator)
		WhitelistHandler(jda, commandCreator)
		SettingsHandler(jda, commandCreator)
		TrustedHandler(jda, commandCreator)
		AlertHandler(jda, commandCreator)
		BanHandler(jda, commandCreator)

		// Register all commands
		commandCreator.register()

		logger.info("Bot started. Invite link: ${jda.getInviteUrl(Permission.BAN_MEMBERS)}")
	}
}

fun main() {
	Main().main()
}