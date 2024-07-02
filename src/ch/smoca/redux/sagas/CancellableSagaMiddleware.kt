package ch.smoca.redux.sagas

import ch.smoca.redux.Action
import ch.smoca.redux.Middleware
import ch.smoca.redux.State
import ch.smoca.redux.Store
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * Actions to this middleware can be cancelled (@Policy).
 * Actions that are not CancelledActions will be processed as TAKE_EVERY.
 * If this middleware should be limited to certain actions, provide a map of Sagas to the accepted actions.
 */
class CancellableSagaMiddleware<T : State>(
    private val sagas: List<Saga<T>>,
) : Middleware<T> {
    //override this for tests
    var coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
    var acceptedActions: Map<Saga<T>, KClass<out Action>>? = null

    enum class Policy {
        TAKE_LATEST, // only the latest action is processed, previous actions are cancelled
        TAKE_EVERY, // every action is processed
        TAKE_LEADING, // only the first action is processed, subsequent actions are ignored until the first action is completed
        CANCEL_LAST, // cancel the last pending action, but do not process this action
    }

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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun process(action: Action, store: Store<T>, next: (action: Action) -> Unit) {
        val oldState = store.getState()
        next(action)
        val newState = store.getState()
        sagas.forEach { saga ->
            // Only accept actions that are explicitly allowed or all actions if no action is specified
            val accepted = acceptedActions
            if (accepted?.get(saga) == null || accepted[saga]?.isInstance(action) == true
            ) {
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