package kz.bejiihiu.candiriya.permission

/**
 * Calculates permission value for a subject.
 * Functional interface so providers can use lambdas or method refs.
 */
public fun interface PermissionFunction {
    public fun getPermissionValue(permission: String): Tristate

    public companion object {
        public val ALWAYS_TRUE: PermissionFunction = PermissionFunction { Tristate.TRUE }
        public val ALWAYS_FALSE: PermissionFunction = PermissionFunction { Tristate.FALSE }
        public val ALWAYS_UNDEFINED: PermissionFunction = PermissionFunction { Tristate.UNDEFINED }
    }
}
