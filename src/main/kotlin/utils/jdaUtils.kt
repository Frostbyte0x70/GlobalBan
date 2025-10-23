package utils

import definitions.globals.Env
import definitions.globals.Whitelist
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.privileges.IntegrationPrivilege

/**
 * Returns whether the given server [member] has the given [permission] in the server. If a [channel] is specified,
 * returns if the member has the permission in that channel.
 */
fun hasPermission(member: Member, permission: Permission, channel: GuildChannel?): Boolean {
	return if (channel == null) {
		member.hasPermission(permission)
	} else {
		member.hasPermission(channel, permission)
	}
}

/**
 * Attempts to retrieve a global or server command by its [name].
 * If a [server] is specified, a local command with the given name will be searched in the server. If [server] is
 * omitted or there's not a command with that name in the server, a global command will be searched instead.
 * This method performs a synchronous request to the Discord API.
 * @return The first command with the given name, or null if no command was found with the given name.
 */
fun getCommandByName(jda: JDA, name: String, server: Guild?): Command? {
	val serverCommand = server?.retrieveCommands()?.complete()?.firstOrNull { it.name == name }
	if (serverCommand != null) {
		return serverCommand
	}

	// Try searching for a global command
	return jda.retrieveCommands().complete().firstOrNull { it.name == name }
}

/**
 * Determines if the given [member] can use the given [command] in the given [server].
 * If a [channel] is specified, the check will be performed for that channel only. If not, it will be performed
 * for the whole server.
 * @param defaultPermission Default permission needed to use the command (set when registering the command).
 */
fun canUseCommand(member: Member, server: Guild, command: Command, defaultPermission: Permission?,
	channel: GuildChannel?): Boolean {
	if (!hasPermission(member, Permission.USE_APPLICATION_COMMANDS, channel)) {
		return false
	}

	val commandPermissions = server.retrieveCommandPrivileges().complete().getCommandPrivileges(command)
	if (commandPermissions != null) {
		var channelCheck: Boolean? = null
		val channelOverrides = commandPermissions.filter { it.type ==  IntegrationPrivilege.Type.CHANNEL}
		val roleOverrides = commandPermissions.filter { it.type ==  IntegrationPrivilege.Type.ROLE}
		val memberOverrides = commandPermissions.filter { it.type ==  IntegrationPrivilege.Type.USER}

		// 1: Check channel denies
		if (channel != null && !channelOverrides.isEmpty()) {
			val allChannelsOverride = channelOverrides.firstOrNull { it.targetsAllChannels() }
			val thisChannelOverride = channelOverrides.firstOrNull { it.id.toLong() == channel.idLong }

			if (thisChannelOverride != null) {
				channelCheck = thisChannelOverride.isEnabled
			}
			if (channelCheck == null && allChannelsOverride != null) {
				channelCheck = allChannelsOverride.isEnabled
			}
		}

		if (channelCheck != null && !channelCheck) {
			// Not allowed in this channel, user and role overrides don't matter
			return false
		}

		// 2: Check user overrides
		if (!memberOverrides.isEmpty()) {
			val thisMemberOverride = memberOverrides.firstOrNull { it.id.toLong() == member.idLong }
			if (thisMemberOverride != null) {
				// Explicit permission for this user, role overrides don't matter
				return thisMemberOverride.isEnabled
			}
		}

		// 3: Check role overrides
		val memberRoleIds = member.unsortedRoles.map { it.idLong }
		if (!roleOverrides.isEmpty()) {
			val everyoneOverride = roleOverrides.firstOrNull { it.targetsEveryone() }
			val memberRolesAllow = roleOverrides.filter { memberRoleIds.contains(it.id.toLong()) && it.isEnabled}
			val memberRolesDeny = roleOverrides.filter { memberRoleIds.contains(it.id.toLong()) && it.isDisabled}

			if (!memberRolesAllow.isEmpty()) {
				// Allowed for at least one of the user's roles
				return true
			}
			if (!memberRolesDeny.isEmpty()) {
				// Denied for at least one of the user's roles
				return false
			}
			if (everyoneOverride != null) {
				// Enabled or disabled for everyone
				return everyoneOverride.isEnabled
			}
		}

		if (!memberOverrides.isEmpty() || !roleOverrides.isEmpty()) {
			// There are role or member overrides, but none apply to the user. The user cannot use the command.
			return false
		}
	}

	// 4: Check default permissions (no role or user overrides, no channel denies)
	return defaultPermission?.let { hasPermission(member, defaultPermission, channel) } ?: true
}

/**
 * Returns all (explicitly or implicitly) whitelisted servers the bot is currently in
 */
fun servers(jda: JDA): List<Guild> {
	val whitelistIds = Whitelist.get().getAllServers().plus(Env.mainServerId)
	return jda.guilds.filter { it.idLong in whitelistIds }
}