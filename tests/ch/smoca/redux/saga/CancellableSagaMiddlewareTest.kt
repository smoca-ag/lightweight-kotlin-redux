package ch.smoca.redux.saga


import ch.smoca.redux.Action
import ch.smoca.redux.Store
import ch.smoca.redux.sagas.CancellableSagaMiddleware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.reflect.KClass
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CancellableSagaTest {

    private lateinit var testSaga: TestSaga
    private lateinit var cancellableSagaMiddleware: CancellableSagaMiddleware<TestState>
    private lateinit var store: Store<TestState>

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() = run {
        testSaga = TestSaga()
        val dispatcher = StandardTestDispatcher(TestCoroutineScheduler())
        cancellableSagaMiddleware = CancellableSagaMiddleware(listOf(testSaga), dispatcher)
        store = Store(TestState(), listOf())
        Dispatchers.setMain(dispatcher)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testTakeEvery() = runTest {
        launch {
            (1..3).forEach { i ->
                cancellableSagaMiddleware.process(
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
        launch {
            (1..3).forEachIndexed() { index, _ ->
                cancellableSagaMiddleware.process(
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
        launch {
            (1..3).forEachIndexed() { index, _ ->
                cancellableSagaMiddleware.process(
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
        launch {
            (1..3).forEachIndexed() { index, _ ->
                cancellableSagaMiddleware.process(
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
        assertEquals(0, testSaga.processedActions.size, "No action should go through")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testFilterActions() = runTest {
        //change TestSaga to only accept  CancelledActions
        testSaga = object : TestSaga() {
            override val acceptAction: KClass<out Action> =
                CancelledActions::class

        }
        cancellableSagaMiddleware = CancellableSagaMiddleware(listOf(testSaga), StandardTestDispatcher(testScheduler))

        launch {
            cancellableSagaMiddleware.process(
                TestSaga.OtherActions.TestAction(
                    id = 0
                ),
                store
            ) {}

            cancellableSagaMiddleware.process(
                TestSaga.CancelledActions.CancellableTestAction(
                    id = 0,
                    policy = CancellableSagaMiddleware.Policy.TAKE_EVERY
                ),
                store
            ) {}

            cancellableSagaMiddleware.process(
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



