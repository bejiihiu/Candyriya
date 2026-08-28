package kz.bejiihiu.candiriya.permission

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

public class WildcardResolverTest {

    @Test
    public fun `exact match wins`() {
        val r = WildcardResolver(setOf("candiriya.command.help"))
        assertThat(r.getPermissionValue("candiriya.command.help")).isEqualTo(Tristate.TRUE)
        assertThat(r.getPermissionValue("candiriya.command.other")).isEqualTo(Tristate.UNDEFINED)
    }

    @Test
    public fun `wildcard matches prefix`() {
        val r = WildcardResolver(setOf("candiriya.*"))
        assertThat(r.getPermissionValue("candiriya.command.help")).isEqualTo(Tristate.TRUE)
        assertThat(r.getPermissionValue("candiriya.other")).isEqualTo(Tristate.TRUE)
        assertThat(r.getPermissionValue("other")).isEqualTo(Tristate.UNDEFINED)
    }

    @Test
    public fun `star matches everything`() {
        val r = WildcardResolver(setOf("*"))
        assertThat(r.getPermissionValue("anything")).isEqualTo(Tristate.TRUE)
    }

    @Test
    public fun `negation beats wildcard`() {
        val r = WildcardResolver(setOf("candiriya.*", "-candiriya.command.stop"))
        assertThat(r.getPermissionValue("candiriya.command.help")).isEqualTo(Tristate.TRUE)
        assertThat(r.getPermissionValue("candiriya.command.stop")).isEqualTo(Tristate.FALSE)
    }

    @Test
    public fun `exact deny via dash prefix`() {
        val r = WildcardResolver(setOf("-candiriya.admin"))
        assertThat(r.getPermissionValue("candiriya.admin")).isEqualTo(Tristate.FALSE)
    }

    @Test
    public fun `longest wildcard wins`() {
        val r = WildcardResolver(setOf("candiriya.*", "candiriya.command.*"))
        // candiriya.command.* is longer, so it wins, but both are TRUE, still TRUE
        assertThat(r.getPermissionValue("candiriya.command.help")).isEqualTo(Tristate.TRUE)
        // test negation with longer prefix
        val r2 = WildcardResolver(setOf("candiriya.*", "-candiriya.command.*"))
        assertThat(r2.getPermissionValue("candiriya.command.help")).isEqualTo(Tristate.FALSE)
        assertThat(r2.getPermissionValue("candiriya.other")).isEqualTo(Tristate.TRUE)
    }
}
