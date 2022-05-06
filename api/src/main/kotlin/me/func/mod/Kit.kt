package me.func.mod

import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent
import me.func.mod.Anime.graffitiClient
import me.func.mod.Anime.provided
import me.func.mod.Npc.npcs
import me.func.mod.battlepass.BattlePass
import me.func.mod.conversation.ModLoader
import me.func.mod.conversation.ModTransfer
import me.func.mod.graffiti.CoreGraffitiClient
import me.func.mod.graffiti.GraffitiManager
import me.func.mod.util.fileLastName
import me.func.protocol.Mod
import net.minecraft.server.v1_12_R1.MinecraftServer
import net.minecraft.server.v1_12_R1.SoundEffects.id
import org.bukkit.Bukkit
import org.bukkit.Bukkit.getPluginManager
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import ru.cristalix.core.formatting.Formatting
import java.util.EnumSet

val STANDARD_MOD_URL = MOD_STORAGE_URL + "standard-mod-bundle.jar"
val GRAFFITI_MOD_URL = MOD_STORAGE_URL + "graffiti-bundle.jar"

@PublishedApi
internal object StandardMods : Listener {
    val mods: EnumSet<Mod> = EnumSet.noneOf(Mod::class.java)

    init {
        ModLoader.loadFromWeb(STANDARD_MOD_URL)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun PlayerJoinEvent.handle() {
        if (mods.isNotEmpty()) {
            ModLoader.send(STANDARD_MOD_URL.fileLastName(), player)

            ModTransfer()
                .integer(mods.size)
                .apply {
                    mods.forEach { integer(it.ordinal) }
                }.send("anime:loadmod", player)
        }
    }
}

enum class Kit(val fromUrl: String? = null, private val setup: () -> Unit = {}) : Listener {
    STANDARD({ StandardMods.mods.add(Mod.STANDARD) }),
    LOOTBOX({ StandardMods.mods.add(Mod.LOOTBOX) }) {
        @EventHandler(priority = EventPriority.LOW)
        fun PlayerCommandPreprocessEvent.handle() {
            if (!cancel && message == "lootboxsound")
                player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1f, 2f)
        }
    },
    DIALOG({ StandardMods.mods.add(Mod.DIALOG) }),
    EXPERIMENTAL({ StandardMods.mods.add(Mod.EXPERIMENTAL) }) {
        @EventHandler(priority = EventPriority.LOW)
        fun PlayerJoinEvent.handle() {
            MinecraftServer.SERVER.postToMainThread {
                Banners.show(player, *Banners.banners.map { it.value }.toTypedArray())
            }
        }
    },
    MULTI_CHAT({ StandardMods.mods.add(Mod.CHAT) }),
    NPC({ StandardMods.mods.add(Mod.NPC) }) {
        @EventHandler(priority = EventPriority.LOW)
        fun PlayerJoinEvent.handle() {
            MinecraftServer.SERVER.postToMainThread {
                npcs.forEach { (_, value) -> value.spawn(player) }
            }
        }

        @EventHandler
        fun PlayerUseUnknownEntityEvent.handle() {
            npcs[entityId]?.click?.accept(this)
        }

        @EventHandler
        fun PlayerChangedWorldEvent.handle() {
            // Если игрок сменил мир, отправить ему NPC в его мире
            MinecraftServer.SERVER.postToMainThread {
                npcs.forEach { (_, npc) -> npc.hide(player) }
                npcs.filter { it.value.worldUuid == null || it.value.worldUuid == player.world.uid }
                    .forEach { (_, npc) -> npc.spawn(player) }
            }
        }
    },
    BATTLEPASS({ StandardMods.mods.add(Mod.BATTLEPASS) }) {
        @EventHandler(priority = EventPriority.LOW)
        fun PlayerJoinEvent.handle() {
            MinecraftServer.SERVER.postToMainThread {
                BattlePass.battlePasses.forEach { (_, value) -> BattlePass.send(player, value) }
            }
        }
    },
    HEALTH_BAR({ StandardMods.mods.add(Mod.HEALTHBAR) }),
    GRAFFITI(GRAFFITI_MOD_URL) {
        @EventHandler(priority = EventPriority.LOW)
        fun AsyncPlayerPreLoginEvent.handle() {
            // Если он на самом деле не заходит, то не грузить
            if (result == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
                // Первая попытка загрузки данных
                GraffitiManager.tryPutData(uniqueId)
            }
        }

        @EventHandler
        fun PlayerQuitEvent.handle1() {
            // Очистка от игрока
            GraffitiManager.clear(player)
        }

        @EventHandler(priority = EventPriority.LOW)
        fun PlayerJoinEvent.handle() {
            // Загрузка нового клиента
            graffitiClient = graffitiClient ?: CoreGraffitiClient()

            Bukkit.getScheduler().runTaskLater(provided, {
                // Отправить картинку с граффити
                Anime.loadTexture(player, "https://storage.c7x.ru/func/animation-api/graffiti.png")

                // Отправить игроку мод
                ModLoader.send(GRAFFITI_MOD_URL.fileLastName(), player)

                // Отправка всех действующих граффити игроку в его мире
                GraffitiManager.sendGraffitiBulk(player)

                // Загрузка персональных граффити
                GraffitiManager.tryPutData(player.uniqueId).thenAccept { data ->

                    // Если данные игрока успешно загрузились - отправить их
                    data?.let {
                        val transfer = ModTransfer(player.uniqueId.toString(), data.packs.size)

                        data.packs.forEach { pack ->
                            transfer.string(pack.uuid.toString()).integer(pack.graffiti.size)

                            pack.graffiti.forEach { graffiti ->
                                transfer.string(graffiti.uuid.toString())
                                    .integer(graffiti.address.x)
                                    .integer(graffiti.address.y)
                                    .integer(graffiti.address.size)
                                    .integer(graffiti.address.maxUses)
                                    .string(graffiti.author)
                                    .integer(graffiti.uses)
                            }

                            transfer.string(pack.title)
                                .string(pack.creator)
                                .integer(pack.price)
                                .integer(pack.rare)
                                .boolean(pack.available)
                        }

                        transfer.integer(data.activePack).integer(data.stickers.size)

                        data.stickers.forEach {
                            transfer.string(it.uuid.toString())
                                .string(it.name)
                                .integer(it.rare.ordinal)
                                .long(it.openTime)
                        }

                        data.activeSticker?.toString()?.let { transfer.string(it) }

                        transfer.send("graffiti:init", player)
                        return@thenAccept
                    }

                    // Если же данные не загрузились
                    player.sendMessage(Formatting.error("Сервер не получил данных от сервиса граффити."))
                }
            }, 5)
        }

        @EventHandler
        fun PlayerChangedWorldEvent.handle() {
            // Если игрок был телепортирован, отправить ему граффити в его мире
            MinecraftServer.SERVER.postToMainThread { GraffitiManager.sendGraffitiBulk(player) }
        }
    },
    DEBUG,
    STORE({ StandardMods.mods.add(Mod.STORE) }),
    ;

    constructor(setup: () -> Unit) : this(null, setup)

    fun init() {
        setup()
        getPluginManager().registerEvents(this, provided)
    }
}
