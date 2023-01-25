package dev.flammky.compose_components.core

/**
 * Denote that the annotated target is a `Snapshot Read`.
 * meaning it will read a SnapShot target and may trigger any ReadObserver that execute the block
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
)
@Retention(AnnotationRetention.SOURCE)
annotation class SnapshotRead()