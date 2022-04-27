package me.func.mod.experimental.disguise

import me.func.mod.conversation.ModTransfer
import me.func.mod.log
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import javax.naming.OperationNotSupportedException

internal class ModEntityDisguiser : EntityDisguiser {
    override fun disguise(entity: Entity, type: EntityType, vararg players: Player) {
        if (entity !is Player) {
            log("Sorry! Only player type can be disquised!")
            return
        }

        players.forEach { ModTransfer(entity, false, type.typeId).send(CHANNEL, it) }
    }

    override fun reset(entity: Entity, vararg players: Player) {
        if (entity !is Player) {
            log("Sorry! Only player type can be disquised!")
            return
        }

        players.forEach { ModTransfer(entity, true).send(CHANNEL, it) }
    }
}