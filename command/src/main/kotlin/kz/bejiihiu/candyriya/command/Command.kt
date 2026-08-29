package kz.bejiihiu.candyriya.command

/**
 * Simple command contract.
 * Keeps API tiny — future can add Brigadier/Simple/Raw adapters without breaking.
 */
public interface Command {
    /** Permission node required to execute, or null if open. Checked before execute. */
    public val permission: String?

    /** Short description for help. */
    public val description: String get() = ""

    /** Usage string for help, e.g. "<player> <group>". */
    public val usage: String get() = ""

    public fun execute(source: CommandSource, args: Array<String>)

    /**
     * Suggestions for tab-complete.
     * Return empty if no suggestions.
     * `args` includes already typed args (without alias).
     */
    public fun suggest(source: CommandSource, args: Array<String>): List<String> = emptyList()
}
