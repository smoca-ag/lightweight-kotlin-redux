package ch.smoca.redux.stateobservers

import ch.smoca.redux.Action
import ch.smoca.redux.Reducer
import ch.smoca.redux.State
import ch.smoca.redux.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StateObserverTest {

    private lateinit var store: Store<TestState>
    private lateinit var observerMiddleware: StateObserverMiddleware<TestState>
    private var testObserver: TestStateObserver = TestStateObserver()

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        val dispatcher = StandardTestDispatcher(TestCoroutineScheduler())
        observerMiddleware = StateObserverMiddleware(listOf(testObserver), dispatcher)
        store = Store(TestState(), listOf(TestReducer()), listOf(observerMiddleware), dispatcher)
        Dispatchers.setMain(dispatcher)
    }

    @Test
    fun testNoChangeChange() = runTest {
        store.dispatch(TestAction(0))
        testScheduler.advanceUntilIdle()
        assertFalse(
            testObserver.stateDidChange,
            "State observer should not have been called, because there is already testProperty = 0 in the state"
        )
    }

    @Test
    fun testCallObserverOnChange() = runTest {
        store.dispatch(TestAction(1))
        testScheduler.advanceUntilIdle()
        assertTrue(
            testObserver.stateDidChange,
            "State observer should have been called, because state has changed"
        )
    }

    @Test
    fun testUnwatchedChange() = runTest {
        //test if observer is called if we change the unwatched property
        store.dispatch(ChangeUnwatchedState(1))
        testScheduler.advanceUntilIdle()
        assertTrue(
            !testObserver.stateDidChange,
            "State observer should not have been called, because only an unwatched property has changed"
        )
        //change the watched property
        store.dispatch(TestAction(1))
        testScheduler.advanceUntilIdle()
        assertTrue(
            testObserver.stateDidChange,
            "State observer should have been called, because state has changed"
        )
    }



    data class TestState(
        val testProperty: Int = 0,
        val unwatchedProperty: Int = 0,
    ) : State

    data class TestAction(val id: Int = 0) : Action
    data class ChangeUnwatchedState(val data: Int = 0) : Action
    class TestReducer : Reducer<TestState> {
        override fun reduce(action: Action, state: TestState): TestState {
            when (action) {
                is TestAction -> {
                    return state.copy(
                        testProperty = action.id
                    )
                }
                is ChangeUnwatchedState -> {
                    return state.copy(
                        unwatchedProperty = action.data
                    )
                }
            }
            return state
        }
    }

    class TestStateObserver : StateObserver<TestState>() {
        var stateDidChange: Boolean = false
        override fun onStateChanged(state: TestState) {
            stateDidChange = true
        }

        override fun selectSubState(state: TestState): Any {
            return state.testProperty
        }
    }


}