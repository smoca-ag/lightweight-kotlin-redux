package ch.smoca.redux

/**
 * A listener interface for receiving state change notifications.
 *
 * Implementers of this interface can respond to state updates by overriding the [onStateChanged] method.
 * This method is called with the new state whenever a change occurs.
 */
interface StateListener {
    /**
     * Called when the state has changed.
     *
     * @param T the type of the state.
     * @param state the new state.
     */
    fun <T> onStateChanged(state: T)
}
