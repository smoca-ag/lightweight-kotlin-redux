package ch.smoca.redux.sagas

import ch.smoca.redux.Action
import ch.smoca.redux.State
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * A middleware that manages sagas whose actions can be cancelled based on specified [policies][Policy].
 *
 * This middleware processes actions dispatched to sagas and applies cancellation policies,
 * such as taking only the latest, every, or leading action. Actions that are not instances of
 * [CancellableAction] are processed as [TAKE_EVERY][Policy.TAKE_EVERY].
 *
 * @param T the type of state processed by the sagas.
 * @property coroutineDispatcher the dispatcher on which saga actions are executed (default is [Dispatchers.IO]).
 *
 * @constructor Creates a new instance of [CancellableSagaMiddleware] with the provided sagas.
 */
class CancellableSagaMiddleware<T : State>(
    sagas: List<Saga<T>>,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SagaMiddleware<T>(sagas) {

    /**
     * Defines the policy for handling multiple actions that are dispatched in rapid succession.
     *
     * Each policy determines how concurrent or overlapping actions are managed by the system.
     */
    enum class Policy {
        /**
         * Only the latest action is processed; any previous pending actions are cancelled.
         */
        TAKE_LATEST,

        /**
         * Every dispatched action is processed.
         */
        TAKE_EVERY,

        /**
         * Only the first action is processed; subsequent actions are ignored until the first action completes.
         */
        TAKE_LEADING,

        /**
         * Cancels the last pending action and does not process the current action.
         */
        CANCEL_LAST,
    }

    /**
     * Interface for actions that support cancellation policies.
     *
     * By implementing this interface, an action can specify which cancellation policy should be applied
     * when it is dispatched to the middleware. If not overridden, the default policy is [TAKE_EVERY][Policy.TAKE_EVERY].
     */
    interface CancellableAction : Action {
        val policy: Policy
            get() = Policy.TAKE_EVERY
    }

    private data class SagaContext<T : State>(
        val saga: Saga<T>,
        val dispatcher: CoroutineDispatcher
    ) {
        val jobs: MutableMap<KClass<out Action>, Job> = mutableMapOf()
    }
    private val contexts: MutableMap<Saga<T>, SagaContext<T>> = mutableMapOf()

    override fun onActionForSaga(saga: Saga<T>, action: Action, oldState: T, newState: T) {
        val context =
            contexts[saga] ?: SagaContext(saga, coroutineDispatcher.limitedParallelism(1))
        contexts[saga] = context
        val policy = (action as? CancellableAction)?.policy ?: Policy.TAKE_EVERY
        when (policy) {
            Policy.TAKE_EVERY -> takeEvery(context, action, oldState, newState)
            Policy.TAKE_LATEST -> takeLatest(context, action, oldState, newState)
            Policy.TAKE_LEADING -> takeLeading(context, action, oldState, newState)
            Policy.CANCEL_LAST -> context.jobs[action::class]?.cancel()
        }
    }

    private fun takeLeading(context: SagaContext<T>, action: Action, oldState: T, newState: T) {
        if (context.jobs[action::class] == null || context.jobs[action::class]?.isCompleted == true) {
            context.jobs[action::class] = CoroutineScope(context.dispatcher).launch {
                context.saga.onAction(action, oldState, newState)
            }
        }
    }

    private fun takeEvery(context: SagaContext<T>, action: Action, oldState: T, newState: T) {
        context.jobs[action::class] = CoroutineScope(context.dispatcher).launch {
            context.saga.onAction(action, oldState, newState)
        }
    }

    private fun takeLatest(context: SagaContext<T>, action: Action, oldState: T, newState: T) {
        context.jobs[action::class]?.cancel()
        context.jobs[action::class] = CoroutineScope(context.dispatcher).launch {
            context.saga.onAction(action, oldState, newState)
        }
    }


}