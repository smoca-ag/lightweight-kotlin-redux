package ch.smoca.redux.saga

import ch.smoca.redux.Action
import ch.smoca.redux.Reducer
import ch.smoca.redux.State
import ch.smoca.redux.Store
import ch.smoca.redux.sagas.QueueingSagaMiddleware
import ch.smoca.redux.stateobservers.StateObserver
import ch.smoca.redux.stateobservers.StateObserverMiddleware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class StateObserverTest {

    private lateinit var store: Store<TestState>
    private lateinit var observerMiddleware: StateObserverMiddleware<TestState>
    private var testObserver : TestStateObserver<TestState> = TestStateObserver()

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        val dispatcher = StandardTestDispatcher(TestCoroutineScheduler())
        observerMiddleware = StateObserverMiddleware<TestState>(listOf(testObserver), dispatcher)
        store = Store(TestState(), listOf(TestReducer()), listOf(observerMiddleware),  dispatcher)
        Dispatchers.setMain(dispatcher)
    }

    @Test
    fun testStateObserver() = runTest{
        store.dispatch(TestAction(1))
        testScheduler.advanceUntilIdle()
        assertTrue(testObserver.stateDidChange, "State observer should have been called")
    }

    data class TestState(val testProperty: Int = 0) : State

    data class TestAction(val id: Int = 0) : Action
    class TestReducer : Reducer<TestState> {
        override fun reduce(action: Action, state: TestState): TestState {
            when(action){
                is TestAction -> {
                    return state.copy(testProperty = action.id)
                }
            }
            return state
        }

    }

    class TestStateObserver<TestState>: StateObserver() {
        var stateDidChange: Boolean = false
        override fun onStateChanged(state: State) {
            stateDidChange = true
        }
    }
}