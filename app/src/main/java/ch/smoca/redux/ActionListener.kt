package ch.smoca.redux

interface ActionListener<A: Any> {
    fun onAction(action: A)
}
