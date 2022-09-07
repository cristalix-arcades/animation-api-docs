package me.func.mod.menu.choicer

import me.func.mod.menu.Button
import me.func.mod.menu.MenuManager
import me.func.mod.menu.MenuManager.bind
import me.func.mod.menu.Storage
import org.bukkit.entity.Player
import java.util.*

class Choicer(
    override var uuid: UUID = UUID.randomUUID(),
    override var title: String = "Игра",
    var description: String = "Выбери нужный под-режим!",
    override var storage: MutableList<Button> = mutableListOf()
) : Storage {
    var allowClosing: Boolean = true

    constructor(title: String, description: String, vararg storage: Button) :
            this(UUID.randomUUID(), title, description, storage.toMutableList())

    companion object {

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private val choicer: Choicer = Choicer()

        fun uuid(uuid: UUID) = apply { choicer.uuid = uuid }
        fun title(title: String) = apply { choicer.title = title }
        fun description(description: String) = apply { choicer.description = description }
        fun storage(storage: MutableList<Button>) = apply { choicer.storage = storage }
        fun storage(vararg storage: Button) = apply { choicer.storage = storage.toMutableList() }
        fun build() = choicer
    }

    override fun open(player: Player): Storage = MenuManager.push(player, this).apply {
        bind(player)
            .string(title)
            .string(description)
            .boolean(allowClosing)
            .integer(storage.size)
            .apply { storage.forEach { it.write(this) } }
            .send("storage:choice", player)
    }
}