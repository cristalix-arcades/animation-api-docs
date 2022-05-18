package me.func.mod.selection

import me.func.mod.conversation.ModTransfer
import me.func.mod.selection.MenuManager.open
import me.func.mod.selection.MenuManager.pushSelection
import org.bukkit.entity.Player
import java.util.*

class Selection(
    override var uuid: UUID = UUID.randomUUID(),
    var title: String = "Меню",
    var money: String = "Загрузка...",
    var vault: String = "coin",
    var hint: String = "Купить",
    var rows: Int = 3,
    var columns: Int = 4,
    var storage: List<Button>? = null
) : Openable {
    constructor(title: String, money: String, hint: String, rows: Int, columns: Int, vararg storage: Button) :
            this(UUID.randomUUID(), title, money, "coin", hint, rows, columns, storage.toList())

    fun buttons(vararg setup: Button) = apply { storage = setup.toList() }

    override fun open(player: Player) = pushSelection(player, this).open(player, "storage:open",
        ModTransfer()
            .string(uuid.toString())
            .string(title)
            .string(vault)
            .string(money)
            .string(hint)
            .integer(rows)
            .integer(columns)
            .integer(storage?.size ?: 0)
            .apply { storage?.forEach { it.write(this) } }
    )
}