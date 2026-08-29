package kz.bejiihiu.candyriya.permission

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

public class WildcardResolverTest {

    @Test
    public fun `exact match wins`() {
        val r = WildcardResolver(setOf("Candyriya.command.help"))
        assertThat(r.getPermissionValue("Candyriya.command.help")).isEqualTo(Tristate.TRUE)
        assertThat(r.getPermissionValue("Candyriya.command.other")).isEqualTo(Tristate.UNDEFINED)
    }

    @Test
    public fun `wildcard matches prefix`() {
        val r = WildcardResolver(setOf("Candyriya.*"))
        assertThat(r.getPermissionValue("Candyriya.command.help")).isEqualTo(Tristate.TRUE)
        assertThat(r.getPermissionValue("Candyriya.other")).isEqualTo(Tristate.TRUE)
        assertThat(r.getPermissionValue("other")).isEqualTo(Tristate.UNDEFINED)
    }

    @Test
    public fun `star matches everything`() {
        val r = WildcardResolver(setOf("*"))
        assertThat(r.getPermissionValue("anything")).isEqualTo(Tristate.TRUE)
    }

    @Test
    public fun `negation beats wildcard`() {
        val r = WildcardResolver(setOf("Candyriya.*", "-Candyriya.command.stop"))
        assertThat(r.getPermissionValue("Candyriya.command.help")).isEqualTo(Tristate.TRUE)
        assertThat(r.getPermissionValue("Candyriya.command.stop")).isEqualTo(Tristate.FALSE)
    }

    @Test
    public fun `exact deny via dash prefix`() {
        val r = WildcardResolver(setOf("-Candyriya.admin"))
        assertThat(r.getPermissionValue("Candyriya.admin")).isEqualTo(Tristate.FALSE)
    }

    @Test
    public fun `longest wildcard wins`() {
        val r = WildcardResolver(setOf("Candyriya.*", "Candyriya.command.*"))
        // Candyriya.command.* is longer, so it wins, but both are TRUE, still TRUE
        assertThat(r.getPermissionValue("Candyriya.command.help")).isEqualTo(Tristate.TRUE)
        // test negation with longer prefix
        val r2 = WildcardResolver(setOf("Candyriya.*", "-Candyriya.command.*"))
        assertThat(r2.getPermissionValue("Candyriya.command.help")).isEqualTo(Tristate.FALSE)
        assertThat(r2.getPermissionValue("Candyriya.other")).isEqualTo(Tristate.TRUE)
    }
}
