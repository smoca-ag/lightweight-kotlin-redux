package ch.smoca.redux

/**
 * Represents the immutable state of the application.
 *
 * All state classes should be implemented as data classes with `val` only properties.
 * This ensures that the state remains immutable throughout the lifecycle of the application.
 *
 * **Usage Example:**
 *
 * ```
 * data class TestState(
 *     val count: Int = 0
 * ): State
 * ```
 */
interface State
