package io.github.victinix888.selfgoverningcatalyst.screen

import com.mojang.blaze3d.systems.RenderSystem
import io.github.victinix888.selfgoverningcatalyst.MODID
import io.github.victinix888.selfgoverningcatalyst.blockentity.ClickMode
import io.github.victinix888.selfgoverningcatalyst.blockentity.RedstoneMode
import io.github.victinix888.selfgoverningcatalyst.entity.AimDirection
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier

class SelfGoverningCatalystScreen(
        screenHandler: SelfGoverningCatalystScreenHandler,
        private val playerInventory: PlayerInventory,
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
    private val redstoneModeButtonText
        get() = when (redstoneMode) {
            RedstoneMode.IGNORE -> TranslatableText("gui.$MODID.ignore")
            RedstoneMode.LOW -> TranslatableText("gui.$MODID.low")
            RedstoneMode.HIGH -> TranslatableText("gui.$MODID.high")
        }

    private var mode = screenHandler.clickMode
    private var aimDirection = screenHandler.aimDirection
    private var redstoneMode = screenHandler.redstoneMode

    init {
        backgroundHeight = 174
        playerInventoryTitleY = backgroundHeight - 94
    }

    override fun init() {
        super.init()

        addDrawableChild(ButtonWidget(x + 8, y + 15, 95, 20, clickModeButtonText) { button ->
            val blockEntity = playerInventory.player.world.getBlockEntity(screenHandler.blockPos)
            if (blockEntity != null) {
                toggleMode()

                val passedData = PacketByteBuf(Unpooled.buffer())
                passedData.writeBlockPos(blockEntity.pos)
                passedData.writeInt(mode.ordinal)

                ClientPlayNetworking.send(Identifier(MODID, "mode_button_click_packet"), passedData)

                button.message = clickModeButtonText
            }
        })

        addDrawableChild(ButtonWidget(x + 8, y + 35, 95, 20, aimDirectionButtonText) { button ->
            val blockEntity = playerInventory.player.world.getBlockEntity(screenHandler.blockPos)
            if (blockEntity != null) {
                toggleAim()

                val passedData = PacketByteBuf(Unpooled.buffer())
                passedData.writeBlockPos(blockEntity.pos)
                passedData.writeInt(aimDirection.ordinal)

                ClientPlayNetworking.send(Identifier(MODID, "aim_button_click_packet"), passedData)

                button.message = aimDirectionButtonText
            }
        })

        addDrawableChild(ButtonWidget(x + 8, y + 55, 95, 20, redstoneModeButtonText) { button ->
            val blockEntity = playerInventory.player.world.getBlockEntity(screenHandler.blockPos)
            if (blockEntity != null) {
                toggleRedstone()

                val passedData = PacketByteBuf(Unpooled.buffer())
                passedData.writeBlockPos(blockEntity.pos)
                passedData.writeInt(redstoneMode.ordinal)

                ClientPlayNetworking.send(Identifier(MODID, "redstone_button_click_packet"), passedData)

                button.message = redstoneModeButtonText
            }
        })
    }

    override fun render(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(matrices)
        super.render(matrices, mouseX, mouseY, delta)
        drawMouseoverTooltip(matrices, mouseX, mouseY)
    }

    override fun drawBackground(matrices: MatrixStack?, delta: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.setShader { GameRenderer.getPositionTexShader() }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        RenderSystem.setShaderTexture(0, texture)
        val i = (width - backgroundWidth) / 2
        val j = (height - backgroundHeight) / 2
        drawTexture(matrices, i, j, 0, 0, backgroundWidth, backgroundHeight)
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

    private fun toggleRedstone() {
        redstoneMode = when (redstoneMode) {
            RedstoneMode.IGNORE -> RedstoneMode.LOW
            RedstoneMode.LOW -> RedstoneMode.HIGH
            RedstoneMode.HIGH -> RedstoneMode.IGNORE
        }
    }
}