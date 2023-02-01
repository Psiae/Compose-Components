package dev.flammky.compose_components.core

import kotlin.reflect.KProperty

/**
 * Lazy delegate, but with construct function instead of constructor
 */

class LazyConstructor<T> @JvmOverloads constructor(lock: Any = Any()) {

    /**
     * Placeholder Object, generic is nullable
     */
    private object UNSET

    /**
     * The Lock
     */
    private val _lock: Any = lock

    /**
     * The value holder. [UNSET] if not set
     */
    private var _value: Any? = UNSET

    @Suppress("UNCHECKED_CAST")
    private val castValue: T
        get() = try {
            _value as T
        } catch (cce: ClassCastException) {
            error("localValue=$_value was UNSET")
        }

    /**
     * The value.
     *
     * @throws IllegalStateException if [_value] is [UNSET]
     */
    val value: T
        get() {
            if (!isConstructed()) {
                // The value is not yet initialized, check if its still being initialized.
                // If not then IllegalStateException will be thrown
                sync()
            }
            return castValue
        }

    /**
     *  Whether [_value] is already initialized
     *  @see isConstructedAtomic
     */
    fun isConstructed() = _value !== UNSET

    /**
     * Whether [_value] is already initialized, atomically
     * @see isConstructed
     */
    fun isConstructedAtomic() = sync { isConstructed() }

    /**
     * Construct the delegated value.
     *
     * if [_value] is not [UNSET] then it will be returned, ignoring [lazyValue]
     */
    fun construct(lazyValue: () -> T): T {
        if (isConstructed()) {
            return castValue
        }
        return sync {
            if (!isConstructed()) {
                _value = lazyValue()
            }
            castValue
        }
    }

    /**
     * Construct the delegated value.
     *
     * if [_value] is not [UNSET] then [lazyThrow] will be invoked, which return [Nothing]
     */
    fun constructOrThrow(
        lazyValue: () -> T,
        lazyThrow: () -> Nothing
    ): T {
        if (isConstructed()) {
            lazyThrow()
        }
        return sync {
            if (isConstructed()) {
                lazyThrow()
            }
            _value = lazyValue()
            castValue
        }
    }

    private fun sync(): Unit = sync { }
    private fun <T> sync(block: () -> T): T = synchronized(_lock) { block() }

    companion object {
        fun <T> LazyConstructor<T>.valueOrNull(): T? {
            return try { value } catch (ise: IllegalStateException) { null }
        }

        val <T> LazyConstructor<T>.lazy: Lazy<T>
            get() = object : Lazy<T> {
                override val value: T get() = this@lazy.value
                override fun isInitialized(): Boolean = this@lazy.isConstructed()
            }

        operator fun <T> LazyConstructor<T>.getValue(receiver: Any?, property: KProperty<*>): T {
            return value
        }

        operator fun <T> LazyConstructor<T>.setValue(receiver: Any?, property: KProperty<*>, value: T) {
            construct { value }
        }
    }
}
