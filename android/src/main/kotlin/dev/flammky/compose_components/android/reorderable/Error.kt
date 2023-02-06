package dev.flammky.compose_components.android.reorderable

internal fun internalReorderableError(msg: String): Nothing = error(
    """
        InternalReorderableError, please send a bug report.
        msg=$msg
    """
)

internal fun publicReorderableError(msg: String): Nothing = error(
    """
        PublicReorderableError, please refer to usage documentation.
        msg=$msg
    """
)

internal fun internalReorderableStateCheck(
    state: Boolean,
    lazyMsg: () -> Any
) {
    if (!state) internalReorderableError(lazyMsg().toString())
}

internal fun publicReorderableStateCheck(
    state: Boolean,
    lazyMsg: () -> Any
) {
    if (!state) publicReorderableError(lazyMsg().toString())
}