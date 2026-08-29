package kz.bejiihiu.candyriya.command

import kz.bejiihiu.candyriya.permission.PermissionManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

private class TestCommand(
    override val permission: String? = null,
    private val onExec: (CommandSource, Array<String>) -> Unit = { _, _ -> },
    private val onSuggest: (CommandSource, Array<String>) -> List<String> = { _, _ -> emptyList() }
) : Command {
    var executed = false
    var lastArgs: Array<String>? = null
    override fun execute(source: CommandSource, args: Array<String>) {
        executed = true
        lastArgs = args
        onExec(source, args)
    }

    override fun suggest(source: CommandSource, args: Array<String>): List<String> = onSuggest(source, args)
}

public class CommandManagerTest {

    private fun console(): ConsoleSource = ConsoleSource { }

    private fun player(permissions: Set<String> = emptySet()): PlayerSource {
        val pm = PermissionManager()
        val id = java.util.UUID.randomUUID()
        for (p in permissions) pm.addUserPermission(id, p)
        if (permissions.isEmpty()) {
            // default group already has help, so no extra
        }
        return PlayerSource(id, "player", pm)
    }

    @Test
    public fun `dispatch finds command`() {
        val cm = CommandManager()
        val cmd = TestCommand()
        cm.register("test", cmd)
        val src = console()
        assertThat(cm.dispatch(src, "test")).isTrue
        assertThat(cmd.executed).isTrue
    }

    @Test
    public fun `dispatch unknown returns false`() {
        val cm = CommandManager()
        val src = console()
        assertThat(cm.dispatch(src, "unknown")).isFalse
    }

    @Test
    public fun `dispatch checks permission`() {
        val cm = CommandManager()
        val cmd = TestCommand(permission = "candyriya.secret")
        cm.register("secret", cmd)
        val noPerm = player(emptySet())
        cm.dispatch(noPerm, "secret")
        assertThat(cmd.executed).isFalse
        val hasPerm = player(setOf("candyriya.secret"))
        cm.dispatch(hasPerm, "secret")
        assertThat(cmd.executed).isTrue
    }

    @Test
    public fun `console bypasses permission`() {
        val cm = CommandManager()
        val cmd = TestCommand(permission = "candyriya.secret")
        cm.register("secret", cmd)
        val src = console()
        assertThat(src.isConsole).isTrue
        assertThat(src.hasPermission("candyriya.secret")).isTrue
        cm.dispatch(src, "secret")
        assertThat(cmd.executed).isTrue
    }

    @Test
    public fun `suggest filters by permission`() {
        val cm = CommandManager()
        cm.register("open", TestCommand())
        cm.register("secret", TestCommand(permission = "candyriya.secret"))
        val srcNoPerm = player(emptySet())
        val sug = cm.suggest(srcNoPerm, "")
        assertThat(sug).contains("open")
        assertThat(sug).doesNotContain("secret")
        val srcPerm = player(setOf("candyriya.secret"))
        assertThat(cm.suggest(srcPerm, "")).contains("secret")
    }

    @Test
    public fun `alias works`() {
        val cm = CommandManager()
        val cmd = TestCommand()
        cm.register("candyriya", cmd, "candiriya")
        val src = console()
        cm.dispatch(src, "candiriya")
        assertThat(cmd.executed).isTrue
    }

    @Test
    public fun `console is distinguishable`() {
        val console = console()
        val player = player()
        assertThat(console.isConsole).isTrue
        assertThat(player.isConsole).isFalse
        assertThat(console.name).isEqualTo("Console")
        assertThat(player.name).isEqualTo("player")
    }
}
