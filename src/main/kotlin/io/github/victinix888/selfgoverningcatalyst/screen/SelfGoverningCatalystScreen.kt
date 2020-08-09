package io.github.victinix888.selfgoverningcatalyst.screen

import com.mojang.blaze3d.systems.RenderSystem
import io.github.victinix888.selfgoverningcatalyst.MODID
import io.github.victinix888.selfgoverningcatalyst.blockentity.ClickMode
import io.github.victinix888.selfgoverningcatalyst.entity.AimDirection
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier

class SelfGoverningCatalystScreen(
        screenHandler: SelfGoverningCatalystScreenHandler,
        playerInventory: PlayerInventory,
        title: Text
) : HandledScreen<SelfGoverningCatalystScreenHandler>(screenHandler, playerInventory, title) {

    private val texture = Identifier(MODID, "textures/gui/container/self_governing_catalyst.png")
    private val clickModeButtonText
        get() = if (mode == ClickMode.RIGHT_CLICK) TranslatableText("gui.$MODID.right_click") else TranslatableText("gui.$MODID.left_click")
    private val aimDirectionButtonText
        get() = when (aimDirection) {
            AimDirection.STRAIGHT -> TranslatableText("gui.$MODID.straight")
            AimDirection.UP -> TranslatableText("gui.$MODID.up")
            AimDirection.DOWN -> TranslatableText("gui.$MODID.down")
        }

    private var mode = screenHandler.clickMode
    private var aimDirection = screenHandler.aimDirection

    override fun init() {
        super.init()

        addButton(ButtonWidget(x + 8, y + 30, 60, 20, clickModeButtonText, ButtonWidget.PressAction { button ->
            val blockEntity = playerInventory.player.world.getBlockEntity(screenHandler.blockPos)
            if (blockEntity != null) {
                toggleMode()

                val passedData = PacketByteBuf(Unpooled.buffer())
                passedData.writeBlockPos(blockEntity.pos)
                passedData.writeInt(mode.ordinal)

                ClientSidePacketRegistry.INSTANCE.sendToServer(Identifier(MODID, "mode_button_click_packet"), passedData)

                button.message = clickModeButtonText
            }
        }))

        addButton(ButtonWidget(x + 8, y + 50, 60, 20, aimDirectionButtonText, ButtonWidget.PressAction { button ->
            val blockEntity = playerInventory.player.world.getBlockEntity(screenHandler.blockPos)
            if (blockEntity != null) {
                toggleAim()

                val passedData = PacketByteBuf(Unpooled.buffer())
                passedData.writeBlockPos(blockEntity.pos)
                passedData.writeInt(aimDirection.ordinal)

                ClientSidePacketRegistry.INSTANCE.sendToServer(Identifier(MODID, "aim_button_click_packet"), passedData)

                button.message = aimDirectionButtonText
            }
        }))
    }

    override fun render(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(matrices)
        super.render(matrices, mouseX, mouseY, delta)
        drawMouseoverTooltip(matrices, mouseX, mouseY)
    }

    override fun drawBackground(matrices: MatrixStack?, delta: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F)
        client?.textureManager?.bindTexture(texture)
        val k = (width - backgroundWidth) / 2
        val l = (height - backgroundHeight) / 2
        drawTexture(matrices, k, l, 0, 0, backgroundWidth, backgroundHeight)
    }

    private fun toggleMode() {
        mode = if (mode == ClickMode.RIGHT_CLICK) ClickMode.LEFT_CLICK else ClickMode.RIGHT_CLICK
    }

    private fun toggleAim() {
        aimDirection = when (aimDirection) {
            AimDirection.STRAIGHT -> AimDirection.UP
            AimDirection.UP -> AimDirection.DOWN
            AimDirection.DOWN -> AimDirection.STRAIGHT
        }
    }
}