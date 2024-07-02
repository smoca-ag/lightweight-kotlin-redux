package ch.smoca.redux.sagas

import ch.smoca.redux.Action
import ch.smoca.redux.Middleware
import ch.smoca.redux.State
import ch.smoca.redux.Store
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * A middleware that queues actions and processes them in a saga.
 * This saga will only accept QueueingAction, other actions will forward to the next middleware.
 */
class QueueingSagaMiddleware<T : State>(private val sagas: List<Saga<T>>) : Middleware<T> {
    var coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO

    enum class Policy {
        ADD, // add the action to the queue
        CLEAR, // clear the queue
        CLEAR_AND_ADD, // clear the queue and add the action
    }

    interface QueueingAction : Action {
        val policy: Policy
            get() = Policy.ADD
    }

    data class QueuedAction<T : State>(val action: Action, val oldState: T, val newState: T) :
        QueueingAction

    data class SagaQueue<T : State>(
        var queue: Channel<QueuedAction<T>> = Channel(Channel.UNLIMITED),
        var consumer: Job? = null
    )

    private val contexts: MutableMap<Saga<T>, SagaQueue<T>> = mutableMapOf()

    override fun process(action: Action, store: Store<T>, next: (action: Action) -> Unit) {

        val oldState = store.getState()
        next(action)
        val newState = store.getState()

        (action as? QueueingAction)?.let {
            sagas.forEach { saga ->
                val sagaQueue = contexts[saga] ?: SagaQueue()
                contexts[saga] = sagaQueue
                if (processToQueue(sagaQueue, action.policy)) {
                    CoroutineScope(coroutineDispatcher).launch {
                        sagaQueue.queue.send(QueuedAction(action, oldState, newState))
                    }

                    //start consumer
                    if (sagaQueue.consumer == null) {
                        sagaQueue.consumer = CoroutineScope(coroutineDispatcher).launch {
                            for (queuedAction in sagaQueue.queue) {
                                saga.onAction(
                                    queuedAction.action,
                                    queuedAction.oldState,
                                    queuedAction.newState
                                )
                            }
                        }

                    }
                }

            }
        }
    }

    /*
    @return true if the new elements can be added
     */
    private fun processToQueue(
        sagaQueue: SagaQueue<T>,
        policy: Policy
    ): Boolean {
        return when (policy) {
            Policy.ADD -> {
                true
            }

            Policy.CLEAR -> {
                //clear everything
                clearSagaQueue(sagaQueue)
                false
            }

            Policy.CLEAR_AND_ADD -> {
                clearSagaQueue(sagaQueue)
                true
            }
        }
    }

    private fun clearSagaQueue(sagaQueue: SagaQueue<T>) {
        sagaQueue.queue.cancel()
        sagaQueue.queue = Channel(Channel.UNLIMITED)
        sagaQueue.consumer?.cancel()
        sagaQueue.consumer = null
    }
}