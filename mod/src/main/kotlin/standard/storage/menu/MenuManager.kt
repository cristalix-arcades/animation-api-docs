package standard.storage.menu

import Main.Companion.menuStack
import asColor
import dev.xdark.clientapi.item.ItemTools
import dev.xdark.clientapi.resource.ResourceLocation
import dev.xdark.feder.NetUtil
import io.netty.buffer.ByteBuf
import org.lwjgl.input.Mouse
import readColoredUtf8
import readRgb
import ru.cristalix.clientapi.KotlinModHolder.mod
import ru.cristalix.uiengine.UIEngine
import ru.cristalix.uiengine.utility.Color
import standard.storage.AbstractMenu
import standard.storage.Information
import standard.storage.button.StorageItemStack
import standard.storage.button.StorageItemTexture
import standard.storage.menu.selection.SelectionManager
import java.util.*

class MenuManager {

    companion object {
        var openTimeAndDelayMillis = 0L // Время последнего открытия меню
        val beforeClickDelay = 200 // задержка перед кликом в миллисекундах
        val itemPadding = 4.0

        fun isMenuClickBlocked() = openTimeAndDelayMillis + beforeClickDelay > System.currentTimeMillis()

        fun readIcons(buffer: ByteBuf) = MutableList(buffer.readInt()) {
            val data = if (buffer.readBoolean()) StorageItemStack(ItemTools.read(buffer)) // item
            else StorageItemTexture(NetUtil.readUtf8(buffer)) // texture

            data.apply {
                price = buffer.readLong() // price
                priceText = buffer.readColoredUtf8()
                title = buffer.readColoredUtf8() // item title
                description = buffer.readColoredUtf8() // item description
                hint = buffer.readColoredUtf8() // item hint
                hoverText = buffer.readColoredUtf8() // item hover desc
                command = buffer.readColoredUtf8() // command
                vault = buffer.readColoredUtf8() // vault
                backgroundColor = buffer.readRgb() // color
                enabled = buffer.readBoolean() // enabled button
                sale = buffer.readInt()
            }
        }

        fun push(gui: AbstractMenu) {
            if (menuStack.size > 20)
                menuStack.clear()
            menuStack.push(gui)
            openTimeAndDelayMillis = System.currentTimeMillis()
            gui.open()
        }

        init {
            println("Menu manager loaded successfully!")

            // Инициализируем менеджера для Selection
            SelectionManager.run()
            SelectionManager.Companion

            // Открыть меню подтверждения
            mod.registerChannel("func:accept") {
                menuStack.clear()
                Confirmation(UUID.fromString(NetUtil.readUtf8(this)), NetUtil.readUtf8(this)).open()
            }

            // Обновить кнопку
            mod.registerChannel("button:update") {
                val last = if (menuStack.size < 1) return@registerChannel else menuStack.peek()
                val index = readInt()
                if (index < 0 || index >= last.storage.size) return@registerChannel
                val node = last.storage[index]
                val inited = node.bundle != null

                when (readByte().toInt()) {
                    0 -> {
                        if (!inited) return@registerChannel

                        val parts = NetUtil.readUtf8(this).split(":", limit = 2)
                        (node as StorageItemTexture).icon.textureLocation =
                            ResourceLocation.of(parts.first(), parts.last())
                    }

                    1 -> {
                        if (!inited) return@registerChannel
                        (node as StorageItemStack).icon.stack = ItemTools.read(this)
                    }

                    2 -> {
                        node.title = readColoredUtf8()

                        if (!inited) return@registerChannel
                        node.titleElement?.content = node.title
                    }

                    3 -> {
                        node.description = readColoredUtf8()

                        if (!inited) return@registerChannel
                        node.descriptionElement?.content = node.description

                        if (last is PlayChoice) return@registerChannel
                        node.optimizeSpace()
                    }

                    4 -> {
                        node.hint = readColoredUtf8()
                        if (!inited) return@registerChannel
                        node.hintElement?.content = node.hint!!
                    }

                    5 -> {
                        if (!inited) return@registerChannel
                        node.hoverText = readColoredUtf8()
                    }

                    6 -> {
                        node.backgroundColor = readRgb()
                        if (!inited) return@registerChannel
                        node.hintContainer?.color = node.backgroundColor.asColor()
                        node.bundle?.color = node.backgroundColor.asColor(0.28)
                    }

                    else -> return@registerChannel
                }
            }

            // Открыть меню выбора режима
            mod.registerChannel("storage:choice") {
                push(
                    PlayChoice(
                        uuid = UUID.fromString(NetUtil.readUtf8(this)),
                        info = NetUtil.readUtf8(this),
                        title = readColoredUtf8(),
                        description = readColoredUtf8(),
                        allowClosing = readBoolean(),
                        storage = readIcons(this)
                    )
                )
            }

            // Обновление места нахождения текста при наведении
            var lastText: String?

            UIEngine.postOverlayContext.afterRender {
                if (menuStack.isEmpty()) {
                    return@afterRender
                }

                lastText = null
                val stack = menuStack.peek() ?: return@afterRender
                for (node in stack.storage) {
                    if (node.bundle?.hovered == true) {
                        lastText = node.hoverText
                        break
                    }
                }

                // Информационный блок
                if (stack is Information && stack.info.isNotEmpty() && stack.getInformationBlock().hovered) {
                    lastText = stack.info
                }

                if (lastText.isNullOrEmpty()) {
                    return@afterRender
                }

                val resolution = UIEngine.clientApi.resolution()
                val scaleFactor = resolution.scaleFactor

                val x = Mouse.getX() / scaleFactor
                val y = resolution.scaledHeight - Mouse.getY() / scaleFactor

                val screen = UIEngine.clientApi.minecraft().currentScreen()
                screen?.drawHoveringText(lastText!!.split("\n"), x, y)
            }
        }
    }
}
