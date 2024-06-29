package ch.smoca.redux.sagas

import ch.smoca.redux.Action
import ch.smoca.redux.Middleware
import ch.smoca.redux.Saga
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

class CancellableSagaMiddleware<T : State>(
    private val sagas: List<Saga<T>>,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) : Middleware<T> {
    enum class Policy {
        TAKE_LATEST,
        TAKE_EVERY,
        TAKE_LEADING,
        CANCEL
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
            val context = contexts[saga] ?: SagaContext(saga, coroutineDispatcher.limitedParallelism(1))
            contexts[saga] = context
            val policy = (action as? CancellableAction)?.policy ?: Policy.TAKE_EVERY
            when (policy) {
                Policy.TAKE_LATEST -> takeLatest(context, action, oldState, newState)
                Policy.TAKE_EVERY -> takeEvery(context, action, oldState, newState)
                Policy.TAKE_LEADING -> takeLeading(context, action, oldState, newState)
                Policy.CANCEL -> context.jobs[action::class]?.cancel()
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