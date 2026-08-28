package kz.bejiihiu.candiriya.permission

/**
 * Three-valued permission result.
 * Mirrors Velocity's Tristate but stays standalone so LuckPerms bridge can map directly.
 */
public enum class Tristate {
    TRUE,
    FALSE,
    UNDEFINED;

    public fun asBoolean(): Boolean = this == TRUE

    public fun asBooleanOr(defaultValue: Boolean): Boolean = when (this) {
        TRUE -> true
        FALSE -> false
        UNDEFINED -> defaultValue
    }

    public companion object {
        public fun fromBoolean(value: Boolean): Tristate = if (value) TRUE else FALSE
    }
}
