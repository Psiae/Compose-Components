package dev.flammky.compose_components.android.reorderable

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import dev.flammky.compose_components.core.*
import dev.flammky.compose_components.core.horizontalOffset
import dev.flammky.compose_components.core.verticalOffset

interface ReorderableLazyListScope {

    fun item(
        // as of now key is a must, there will be non-key variant in the future
        key: Any,
        content: @Composable ReorderableLazyItemScope.() -> Unit
    ) = items(1, { key }) { content() }

    fun items(
        count: Int,
        // as of now key is a must, there will be non-key variant in the future
        key: (Int) -> Any,
        content: @Composable ReorderableLazyItemScope.(Int) -> Unit
    )
}

/**
 * Interface Scope of an Reorderable Lazy Item
 */
interface ReorderableLazyItemScope {

    /**
     * Info about the item
     */
    val info: ItemInfo

    /**
     * install reorder input Modifier,
     *
     * any drag gesture starting on the composable with this modifier will be interpreted as start
     * of reordering event
     */
    fun Modifier.reorderInput(): Modifier

    /**
     * install reorder input Modifier with timeout,
     *
     * any down gesture on the composable that last for at least the specified [timeMs] will be
     * interpreted as start of reordering event
     *
     * defaults to the device `Long Press` duration config, normally it's 400ms
     */
    fun Modifier.reorderLongInput(timeMs: Long? = null): Modifier

    /**
     * install visual modifiers such as `zIndex` and `graphicsLayer` for the Item to float over other item,
     *
     * this is optional and is Not applied by default
     */
    @SnapshotRead
    fun Modifier.reorderingItemVisualModifiers(): Modifier

    /**
     * Info about the item
     */
    interface ItemInfo {
        val dragging: Boolean
            @SnapshotRead get

        val key: Any
        val indexInParent: Int
        val indexInBatch: Int
    }
}

internal class RealReorderableLazyListScope() : ReorderableLazyListScope {

    private val _indexToKeyMapping = mutableMapOf<Int, Any>()
    private val _indexToItemMapping = mutableMapOf<Int, ReorderableLazyListItem>()
    private val _keyToIndexMapping = mutableMapOf<Any, Int>()
    private val _intervals = mutableListOf<ReorderableLazyItemInterval>()
    private var itemsLastIndex = 0

    val intervals: List<ReorderableLazyItemInterval> = _intervals

    override fun item(
        key: Any,
        content: @Composable ReorderableLazyItemScope.() -> Unit
    ) {
        items(1, { key }) { content() }
    }

    override fun items(
        count: Int,
        key: (Int) -> Any,
        content: @Composable ReorderableLazyItemScope.(Int) -> Unit
    ) {
        val intervalStartIndex = itemsLastIndex
        itemsLastIndex += count
        val interval = ReorderableLazyItemInterval(
            items = mutableListOf<ReorderableLazyListItem>()
                .apply {
                    repeat(count) { i ->
                        val iKey = key(i)
                        val item = ReorderableLazyListItem(
                            i,
                            intervalStartIndex + i,
                            iKey,
                            null,
                            content
                        )
                        _indexToKeyMapping[intervalStartIndex + i] = iKey
                        _indexToItemMapping[intervalStartIndex + i] = item
                        _keyToIndexMapping[iKey] = intervalStartIndex + i
                        add(item)
                    }
                }
        )
        _intervals.add(interval)
        repeat(count) { i ->
            val iKey = key(i)
            _indexToKeyMapping[intervalStartIndex + i] = iKey
            _keyToIndexMapping[iKey] = intervalStartIndex + i
        }
    }

    fun indexOfKey(key: Any): Int = _keyToIndexMapping.getOrElse(key) { -1 }
    fun itemOfIndex(index: Int): ReorderableLazyListItem? = _indexToItemMapping.getOrElse(index) { null }
}

internal class RealReorderableLazyItemScope(
    private val parentOrientation: Orientation,
    private val positionInParent: ItemPosition,
    private val positionInBatch: ItemPosition,
    private val currentDraggingItemPositionInParent: @SnapshotRead () -> ItemPosition?,
    private val currentDraggingItemDelta: @SnapshotRead () -> Offset,
    private val onReorderInput: (pointerId: PointerId, offset: Offset?) -> Unit,
    val content: @Composable ReorderableLazyItemScope.(index: Int) -> Unit
) : ReorderableLazyItemScope {

    // TODO: Make things as lazy as possible
    override val info = object : ReorderableLazyItemScope.ItemInfo {

        override val dragging: Boolean by derivedStateOf(policy = structuralEqualityPolicy()) {
            currentDraggingItemPositionInParent()?.takeIf { it.key == key } != null
        }

        override val key: Any = positionInParent.key
        override val indexInParent: Int = positionInParent.index
        override val indexInBatch: Int = positionInBatch.index
    }

    override fun Modifier.reorderInput(): Modifier {
        return this.then(
            // install suspending pointer input filter to the composable
            Modifier.pointerInput(this@RealReorderableLazyItemScope) {
                // for each possible gesture, install handle
                forEachGesture {
                    // put pointer filter
                    awaitPointerEventScope {
                        // await first down / press
                        awaitFirstDown().let { firstDown ->
                            // await for pointer slop (a distance in pixel before a gesture is considered a movement)
                            var slop = Offset.Zero
                            val awaitSlop = when (parentOrientation) {
                                Orientation.Vertical -> {
                                    awaitVerticalPointerSlopOrCancellation(
                                        pointerId = firstDown.id,
                                        pointerType = firstDown.type
                                    ) { change, slopReached ->
                                        change.consume()
                                        slop = verticalOffset(slopReached)
                                    }
                                }
                                Orientation.Horizontal -> {
                                    awaitHorizontalPointerSlopOrCancellation(
                                        pointerId = firstDown.id,
                                        pointerType = firstDown.type
                                    ) { change, slopReached ->
                                        change.consume()
                                        slop = horizontalOffset(slopReached)
                                    }
                                }
                                else -> notImplementedError()
                            }
                            if (awaitSlop != null) {
                                // if the slop is reached from the down event then it's a Drag Start
                                onReorderInput(firstDown.id, slop)
                            }
                        }
                    }
                }
            }
        )
    }

    override fun Modifier.reorderLongInput(timeMs: Long?): Modifier {
        return this.then(
            // install pointer-input filter to the composable
            Modifier.pointerInput(this@RealReorderableLazyItemScope) {
                // for each new gesture, install handle
                forEachGesture {
                    // put pointerEvent filter
                    awaitPointerEventScope {
                        // await first down / gesture
                        awaitFirstDown()
                            .let { firstDown ->
                                // await the down event for the specified time
                                if (awaitLongPressOrCancellation(firstDown.id, timeMs) != null) {
                                    // was pressed long enough without interruption then it's a Drag Start
                                    onReorderInput(firstDown.id, null)
                                }
                            }
                    }
                }
            }
        )
    }

    @SnapshotRead
    override fun Modifier.reorderingItemVisualModifiers(): Modifier {
        return combineIf(info.dragging) {
            Modifier
                .zIndex(1f)
                .graphicsLayer {
                    when (parentOrientation) {
                        Orientation.Horizontal -> {
                            translationX = currentDraggingItemDelta().x
                        }
                        Orientation.Vertical -> {
                            translationY = currentDraggingItemDelta().y
                        }
                    }
                }
        }
    }
}

internal class ReorderableLazyItemInterval(
    val items: List<ReorderableLazyListItem>
)

internal class ReorderableLazyListItem(
    val indexInInterval: Int,
    val indexInParent: Int,
    val key: Any,
    val type: Any?,
    val content: @Composable ReorderableLazyItemScope.(index: Int) -> Unit
)