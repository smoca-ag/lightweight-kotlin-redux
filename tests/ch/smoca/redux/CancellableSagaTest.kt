package ch.smoca.redux


import ch.smoca.redux.sagas.CancellableSagaMiddleware
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.reflect.KClass
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CancellableSagaTest {

    private lateinit var testSaga: TestSaga
    private lateinit var middleware: CancellableSagaMiddleware<TestState>
    private val store = Store(TestState())

    @BeforeTest
    fun setUp() {
        testSaga = TestSaga()
        middleware = CancellableSagaMiddleware(listOf(testSaga))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testTakeEvery() = runTest {
        middleware.coroutineDispatcher = StandardTestDispatcher(testScheduler)
        launch {
            (1..3).forEach { i ->
                middleware.process(
                    TestSaga.CancelledActions.CancellableTestAction(i),
                    store
                ) {}
            }
            //the test saga waits for 1000ms.
            testScheduler.advanceTimeBy(3000)
            //3 actions should go through.
            assertEquals(3, testSaga.processedActions.size)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testTakeLatest() = runTest {
        middleware.coroutineDispatcher = StandardTestDispatcher(testScheduler)
        launch {
            (1..3).forEachIndexed() { index, _ ->
                middleware.process(
                    TestSaga.CancelledActions.CancellableTestAction(
                        id = index + 1,
                        policy = CancellableSagaMiddleware.Policy.TAKE_LATEST
                    ),
                    store
                ) {}
            }
        }
        //the test saga waits for 1000ms. But only the last action should go through.
        testScheduler.advanceTimeBy(1001)
        //only the last action should go through.
        assertEquals(1, testSaga.processedActions.size, "Only 1 action should go trough")
        //the last action must have id 3.
        assertEquals(
            3,
            (testSaga.processedActions.first() as TestSaga.CancelledActions.CancellableTestAction).id,
            "last action must have id 3"
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testTakeLeading() = runTest {
        middleware.coroutineDispatcher = StandardTestDispatcher(testScheduler)
        launch {
            (1..3).forEachIndexed() { index, _ ->
                middleware.process(
                    TestSaga.CancelledActions.CancellableTestAction(
                        id = index + 1,
                        policy = CancellableSagaMiddleware.Policy.TAKE_LEADING
                    ),
                    store
                ) {}
            }
        }
        //the test saga waits for 1000ms. But only the first action should go through.
        testScheduler.advanceTimeBy(2000)
        //only the first action should go through.
        assertEquals(1, testSaga.processedActions.size, "Only 1 action should go through")
        //the action must have id 1.
        assertEquals(
            1,
            (testSaga.processedActions.first() as TestSaga.CancelledActions.CancellableTestAction).id,
            "the action must have id 1"
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testCancel() = runTest {
        middleware.coroutineDispatcher = StandardTestDispatcher(testScheduler)
        launch {
            (1..3).forEachIndexed() { index, _ ->
                middleware.process(
                    TestSaga.CancelledActions.CancellableTestAction(
                        id = index + 1,
                        policy = CancellableSagaMiddleware.Policy.CANCEL_LAST
                    ),
                    store
                ) {}
            }
        }
        //the test saga waits for 1000ms. But all action should be canceled
        testScheduler.advanceTimeBy(1001)
        //no action should go through
        assertEquals(0, testSaga.processedActions.size, "No action should go trough")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testFilterActions() = runTest {
        middleware.coroutineDispatcher = StandardTestDispatcher(testScheduler)

        launch {
            middleware.process(
                TestSaga.OtherActions.TestAction(
                    id = 0
                ),
                store
            ) {}

            middleware.process(
                TestSaga.CancelledActions.CancellableTestAction(
                    id = 0,
                    policy = CancellableSagaMiddleware.Policy.TAKE_EVERY
                ),
                store
            ) {}

            middleware.process(
                TestSaga.CancelledActions.SecondLevel.SecondLevelAction,
                store
            ) {}
        }
        testScheduler.advanceTimeBy(1001)
        //only cancellable actions should be processed
        assertEquals(
            1,
            testSaga.processedActions.filterIsInstance<TestSaga.CancelledActions.CancellableTestAction>().size,
            "CancellableTestAction must be processed"
        )
        assertEquals(
            1,
            testSaga.processedActions.filterIsInstance<TestSaga.CancelledActions.SecondLevel.SecondLevelAction>().size,
            "SecondLevel Action must be processed"
        )
        assertEquals(2, testSaga.processedActions.size, "Only two actions should be processed")
    }
}

class TestSaga : Saga<TestState>() {
    val processedActions = mutableListOf<Action>()

    sealed class OtherActions : Action {
        data class TestAction(val id: Int = 0) : OtherActions()
    }

    sealed class CancelledActions : CancellableSagaMiddleware.CancellableAction {
        data class CancellableTestAction(
            val id: Int = 0,
            override val policy: CancellableSagaMiddleware.Policy = CancellableSagaMiddleware.Policy.TAKE_EVERY
        ) : CancelledActions()

        sealed class SecondLevel : CancelledActions() {
            data object SecondLevelAction : SecondLevel()
        }
    }

    override suspend fun onAction(action: Action, oldState: TestState, newState: TestState) {
        delay(1_000)
        processedActions.add(action)
    }

    override fun onlyAcceptAction(): KClass<out Action> {
        return CancelledActions::class
    }
}

data class TestState(val testProperty: Int = 0) : State

