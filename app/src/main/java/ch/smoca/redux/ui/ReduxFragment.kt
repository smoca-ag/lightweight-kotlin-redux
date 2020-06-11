package ch.smoca.redux.ui
import androidx.fragment.app.Fragment
import ch.smoca.redux.Action
import ch.smoca.redux.State
import ch.smoca.redux.Store
import ch.smoca.redux.ioc.IOCProvider

abstract class ReduxFragment<S: State> : Fragment() {

    private lateinit var  store: Store<S>
    private var currentState: S? = null
    val state: S?
    get() = currentState

    override fun onStart() {
        super.onStart()
        store = IOCProvider.iocOfContext(this).resolve()
        store.stateObservable.observe(this.viewLifecycleOwner::getLifecycle) {
            currentState = it
            onStateChanged(it)
        }
    }

    fun dispatch(action: Action) {
        store.dispatch(action)
    }
    abstract fun onStateChanged(state: S)

}