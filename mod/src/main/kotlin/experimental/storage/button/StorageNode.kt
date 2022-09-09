package experimental.storage.button

import Main.Companion.menuStack
import dev.xdark.clientapi.opengl.GlStateManager
import dev.xdark.feder.NetUtil
import experimental.storage.AbstractMenu
import experimental.storage.menu.MenuManager
import io.netty.buffer.Unpooled
import me.func.protocol.menu.Button
import ru.cristalix.uiengine.ClickEvent
import ru.cristalix.uiengine.UIEngine
import ru.cristalix.uiengine.element.AbstractElement
import ru.cristalix.uiengine.element.CarvedRectangle
import ru.cristalix.uiengine.element.TextElement
import ru.cristalix.uiengine.utility.*

abstract class StorageNode<T : AbstractElement>(
    override var price: Long = -1,
    override var title: String,
    override var description: String,
    override var hint: String? = null,
    var hoverText: String,
    open var icon: T,
    override var special: Boolean = false
) : Button() {

    var bundle: CarvedRectangle? = null
    var titleElement: TextElement? = null
    var descriptionElement: TextElement? = null
    var hintElement: TextElement? = null
    var hintContainer: CarvedRectangle? = null

    fun createHint(sized: V3, default: String) = hintContainer ?: carved {
        carveSize = 2.0
        size = sized
        color = if (special) Color(255, 157, 66, 1.0) else Color(74, 140, 236, 1.0)
        color.alpha = 0.0
        beforeRender { GlStateManager.disableDepth() }
        afterRender { GlStateManager.enableDepth() }

        hintElement = +text {
            origin = CENTER
            align = CENTER
            color = WHITE
            shadow = true
            color.alpha = 0.0
            content = hint ?: default
            scale = V3(1.5, 1.5, 1.5)
        }
    }.apply { hintContainer = this }

    fun click(menu: AbstractMenu, event: ClickEvent) {
        println(1)
        if (MenuManager.isMenuClickBlocked()) return
        val key = menu.storage.indexOf(this@StorageNode)
        if (command.isNullOrEmpty()) {
            UIEngine.clientApi.clientConnection().sendPayload("storage:click", Unpooled.buffer().apply {
                NetUtil.writeUtf8(this, menu.uuid.toString())
                writeInt(key)
                writeInt(event.button.ordinal)
            })
            return
        }
        menu.close()
        // Если через пол секунды, откроется другое меню - то не чистим стэк
        UIEngine.schedule(0.5) {
            if (menuStack.peek() != menu)
                return@schedule
            menuStack.clear()
        }
        UIEngine.clientApi.chat().sendChatMessage("$command $key")
    }

    fun optimizeSpace(length: Double = (bundle?.size?.x ?: 200.0) - (bundle?.size?.y ?: 100.0)) {
        if (bundle == null || descriptionElement == null) return
        val words = description.split(" ")

        descriptionElement!!.content = lineStart
        words.forEach { word ->
            val line = descriptionElement!!.content.split("\n").last()
            val new = line + word
            val color = line.split("§").last().first()
            if (line != lineStart && new.getRealWidth() > length) {
                descriptionElement!!.content += "\n§$color"
            }
            descriptionElement!!.content += "$word "
        }
    }

    private fun String.getRealWidth(): Int {
        val font = UIEngine.clientApi.fontRenderer()
        val colorChars = count { it == '§' } * 2
        val realString = drop(colorChars).replace("\n", "")

        return font.getStringWidth(realString)
    }

    abstract fun scaling(scale: Double): T

    private companion object {
        private const val lineStart = "§f"
    }
}
