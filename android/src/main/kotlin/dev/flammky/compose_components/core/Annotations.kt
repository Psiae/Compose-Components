package dev.flammky.compose_components.core

import androidx.compose.runtime.snapshots.Snapshot

/**
 * Denote that the annotated target is a `[Snapshot] Read`.
 * meaning it will read a SnapShot target and may trigger any ReadObserver that execute the block.
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
)
@Retention(AnnotationRetention.SOURCE)
annotation class SnapshotRead()

/**
 * Denote that the annotated target is a `[Snapshot] Reader`.
 * meaning any read of a snapshot target within the block may cause it to react in some way.
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER,
)
@Retention(AnnotationRetention.SOURCE)
annotation class SnapshotReader()

