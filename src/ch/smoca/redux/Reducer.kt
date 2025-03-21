package ch.smoca.redux

/**
 * A reducer that listens to actions and produces a new state.
 *
 * Reducers are pure functions that accept an action and the current state and return a new state.
 * They should not have any side effects or hold any internal state.
 *
 * **Usage Example**
 * ```
 * class CountReducer: Reducer<AppState> {
 *     // A sealed class lets the compiler check if the 'when' expression is exhaustive
 *     sealed class CountAction : Action {
 *         data class Add(val amount: Int) : CountAction()
 *     }
 *
 *     override fun reduce(action: Action, state: AppState): AppState {
 *         // only process actions that concern us, otherwise return state
 *         (action as? CountAction) ?: return state
 *         when (action) {
 *             is CountAction.Add -> {
 *                 // the copy-function on each data class can be used to create a new state
 *                 return state.copy(count = state.count + action.amount)
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * @param T The type of state to reduce.
 */
interface Reducer<T : State> {
    /**
     * Reduces the given action and state into a new state.
     *
     * This method is invoked for every action dispatched. If the reducer does not handle
     * the action, it should simply return the provided state.
     *
     * @param action The dispatched action.
     * @param state The current state.
     * @return The new state resulting from the action.
     */
    fun reduce(action: Action, state: T): T
}
