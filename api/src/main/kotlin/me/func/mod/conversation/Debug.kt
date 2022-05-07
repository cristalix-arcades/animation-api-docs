package me.func.mod.conversation

import me.func.mod.*
import me.func.mod.StandardMods
import me.func.mod.util.consoleCommand
import me.func.mod.util.fileLastName
import me.func.mod.util.listFiles
import me.func.mod.util.log
import ru.cristalix.core.formatting.Formatting

const val DEBUG_COMMAND = "anime:debug"

object Debug {

    private var lastUseDebugCommand = 0L

    init {
        // Канал для отправки клиенту информации для отладки
        Anime.createReader(DEBUG_COMMAND) { player, _ ->
            val message = generate()
            ModTransfer(message.size) // Сколько строк отладки выводить\
                .apply { message.forEach { string(it) } }
                .send(DEBUG_COMMAND, player)
        }

        // Команда для получения информации из консоли
        consoleCommand(DEBUG_COMMAND) { generate().forEach { log(it) } }
    }

    private fun generate() = arrayListOf<String>().apply {
        val now = System.currentTimeMillis()
        if (now - lastUseDebugCommand < 5000) {
            add(Formatting.error("Command execute failure! Too much uses..."))
            return@apply
        }
        lastUseDebugCommand = now

        val listMod = listFiles(MOD_LOCAL_DIR_NAME)?.apply { listFiles(MOD_LOCAL_TEST_DIR_NAME)?.let { addAll(it) } }
        add("Animation-API успешно работает!")
        add("API Version: $VERSION")
        add("Standards Mods: ${StandardMods.mods.joinToString(", ") { it.name }}")
        add("Custom Mods: ${ModLoader.mods.keys.filter { it != STANDARD_MOD_URL.fileLastName() }.joinToString(", ") { it }}")
        add("Storage Mods: ${(listMod?.sumOf { it.length() } ?: 0) / 1024}KB")
        add("Allocated Mods: ${(listMod?.filter { ModLoader.mods.containsKey(it.name.fileLastName()) }?.sumOf { it.length() } ?: 0) / 1024}KB")
    }

}