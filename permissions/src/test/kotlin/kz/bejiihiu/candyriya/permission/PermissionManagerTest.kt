package kz.bejiihiu.candyriya.permission

import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

public class PermissionManagerTest {

    @Test
    public fun `default group has help`() {
        val pm = PermissionManager()
        val uuid = UUID.randomUUID()
        val subject = PlayerSubject(uuid, "test", pm)
        assertThat(pm.permissionValue(subject, "Candyriya.command.help")).isEqualTo(Tristate.TRUE)
        assertThat(pm.permissionValue(subject, "Candyriya.command.stop")).isEqualTo(Tristate.FALSE)
    }

    @Test
    public fun `op has everything`() {
        val pm = PermissionManager()
        val uuid = UUID.randomUUID()
        pm.setOp(uuid, true)
        val subject = PlayerSubject(uuid, "op", pm)
        assertThat(pm.permissionValue(subject, "Candyriya.command.stop")).isEqualTo(Tristate.TRUE)
        assertThat(pm.permissionValue(subject, "anything.else")).isEqualTo(Tristate.TRUE)
    }

    @Test
    public fun `group inheritance merges parents`() {
        val pm = PermissionManager()
        // default has help, op parents default and has Candyriya.*
        val uuid = UUID.randomUUID()
        pm.setUserGroups(uuid, setOf("op"))
        val subject = PlayerSubject(uuid, "op2", pm)
        assertThat(pm.permissionValue(subject, "Candyriya.command.help")).isEqualTo(Tristate.TRUE)
        assertThat(pm.permissionValue(subject, "Candyriya.command.reload")).isEqualTo(Tristate.TRUE)
    }

    @Test
    public fun `console always true`() {
        val pm = PermissionManager()
        assertThat(pm.permissionValue(ConsoleSubject, "anything")).isEqualTo(Tristate.TRUE)
    }

    @Test
    public fun `external provider overrides internal`() {
        val pm = PermissionManager()
        pm.setProvider(PermissionProvider { PermissionFunction.ALWAYS_TRUE })
        val uuid = UUID.randomUUID()
        val subject = PlayerSubject(uuid, "x", pm)
        assertThat(pm.permissionValue(subject, "nope")).isEqualTo(Tristate.TRUE)
        pm.setProvider(null)
        assertThat(pm.permissionValue(subject, "nope")).isEqualTo(Tristate.FALSE)
    }

    @Test
    public fun `custom groups work`() {
        val pm = PermissionManager()
        pm.setGroups(
            mapOf(
                "default" to PermissionGroup("default", setOf("Candyriya.command.help"), emptySet(), true),
                "mod" to PermissionGroup("mod", setOf("Candyriya.command.kick"), setOf("default"), false)
            )
        )
        val uuid = UUID.randomUUID()
        pm.setUserGroups(uuid, setOf("mod"))
        val s = PlayerSubject(uuid, "mod", pm)
        assertThat(pm.permissionValue(s, "Candyriya.command.help")).isEqualTo(Tristate.TRUE)
        assertThat(pm.permissionValue(s, "Candyriya.command.kick")).isEqualTo(Tristate.TRUE)
        assertThat(pm.permissionValue(s, "Candyriya.command.stop")).isEqualTo(Tristate.FALSE)
    }
}
