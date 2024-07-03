package ch.smoca.redux.sagas

import ch.smoca.redux.Action
import ch.smoca.redux.State
import kotlin.reflect.KClass

abstract class Saga<T : State> {
    lateinit var dispatch: (action: Action) -> Unit
    abstract suspend fun onAction(action: Action, oldState: T, newState: T)
    // return a sealed class if you need multiple actions
    open val acceptAction : KClass<out Action>? = null
}


