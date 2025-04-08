package ch.smoca.redux.stateobservers

import ch.smoca.redux.Action
import ch.smoca.redux.State

/**
 * Observes state changes and triggers asynchronous operations when the observed state is updated.
 *
 * A `StateObserver` encapsulates logic that should react to changes in the application's state. It
 * can hold internal state and run asynchronous methods. The observer is notified via [onStateChanged]
 * when the state changes. The method is invoked on a coroutine with limited parallelism (1), meaning that
 * a new invocation may start as soon as the previous one completes.
 *
 * The observer has access to the [dispatch] function, which allows it to dispatch new actions. This is useful
 * for scenarios where reacting to a state change should trigger further state updates or operations handled by
 * reducers.
 *
 * Classes that inherit `StateObserver` can overwrite [selectSubState].
 * Instead of the whole state, the selected sub state will be compared. If a difference is
 * detected, [onStateChanged] will be triggered.
 *
 * **Usage Example:**
 *
 * ```
 * class ExampleStateObserver : StateObserver<TestState>() {
 *     override fun onStateChanged(state: TestState) {
 *         // React to the state change
 *         if (state.testProperty == 1) {
 *             // Dispatch a new action if needed
 *             dispatch(WorkResult())
 *         }
 *     }
 *
 *     override fun selectSubState(state: TestState): Any = state.example
 * }
 * ```
 *
 * @param T the type of state being observed. The state is expected to be immutable.
 */
abstract class StateObserver<T : State> {
    /**
     * Function for dispatching actions to the store.
     *
     * This property is typically assigned by the [StateObserverMiddleware] to provide the observer
     * with the ability to dispatch new actions when a state change occurs.
     */
    lateinit var dispatch: (action: Action) -> Unit

    /**
     * Called when the state changes.
     *
     * This method is invoked in a coroutine with a concurrency limit of 1, ensuring that state changes
     * are processed sequentially. It is the responsibility of the observer to determine if the change is
     * relevant and to avoid unnecessary operations.
     * This method is invoked only when the specific part of the state,
     * selected via [selectSubState], has changed.
     *
     * @param state The new state after the change.
     */
    abstract fun onStateChanged(state: T)

    /**
     * Selects the relevant part of the state that this observer is interested in.
     *
     * The returned sub state will be compared between the old and new state. If a difference is
     * detected, [onStateChanged] will be triggered.
     *
     * @param state The current state from which to select the sub state.
     * @return The selected sub state. Defaults to [state]
     */
    open fun selectSubState(state: T): Any? = state
}