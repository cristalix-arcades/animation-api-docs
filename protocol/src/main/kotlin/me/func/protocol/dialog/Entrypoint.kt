package me.func.protocol.dialog

data class Entrypoint(val id: String, val title: String, val screen: Screen) {

    var subtitle: String? = null

    fun subtitle(subtitle: String): Entrypoint {
        this.subtitle = subtitle
        return this
    }
}