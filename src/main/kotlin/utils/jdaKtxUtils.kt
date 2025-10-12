import dev.minn.jda.ktx.messages.MentionConfig
import dev.minn.jda.ktx.messages.Mentions

fun Mentions.Companion.nothing(): Mentions {
	return Mentions(
		MentionConfig.users(emptyList()),
		MentionConfig.roles(emptyList()),
		everyone = false,
		here = false
	)
}