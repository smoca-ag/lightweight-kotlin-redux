package ch.smoca.redux

import kotlin.reflect.KClass

abstract class Saga<T : State> {
    lateinit var dispatch: (action: Action) -> Unit
    abstract suspend fun onAction(action: Action, oldState: T, newState: T)
    // return a sealed class if you need multiple actions
    open fun onlyAcceptAction() : KClass<out Action>?{
        return null
    }
}


