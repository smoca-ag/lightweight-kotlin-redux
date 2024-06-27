package ch.smoca.redux


interface StateListener {
    fun <T> onStateChanged(state: T)
}
