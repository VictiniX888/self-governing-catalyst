package io.github.victinix888.selfgoverningcatalyst

import io.github.victinix888.selfgoverningcatalyst.block.SelfGoverningCatalystBlock
import io.github.victinix888.selfgoverningcatalyst.blockentity.ClickMode
import io.github.victinix888.selfgoverningcatalyst.blockentity.RedstoneMode
import io.github.victinix888.selfgoverningcatalyst.blockentity.SelfGoverningCatalystBlockEntity
import io.github.victinix888.selfgoverningcatalyst.entity.AimDirection
import io.github.victinix888.selfgoverningcatalyst.screen.SelfGoverningCatalystScreen
import io.github.victinix888.selfgoverningcatalyst.screen.SelfGoverningCatalystScreenHandler
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry
import net.minecraft.block.Material
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import java.util.function.Supplier

const val MODID = "selfgoverningcatalyst"

val SELF_GOVERNING_CATALYST = SelfGoverningCatalystBlock(FabricBlockSettings.of(Material.METAL))
val SELF_GOVERNING_CATALYST_BLOCK_ENTITY: BlockEntityType<SelfGoverningCatalystBlockEntity> = BlockEntityType.Builder
        .create({ pos, state -> SelfGoverningCatalystBlockEntity(pos, state) }, SELF_GOVERNING_CATALYST)
        .build(null)

lateinit var SELF_GOVERNING_CATALYST_SCREEN_HANDLER: ScreenHandlerType<SelfGoverningCatalystScreenHandler>

@Suppress("unused")
fun init() {
    Registry.register(Registry.BLOCK, Identifier(MODID, "self_governing_catalyst"), SELF_GOVERNING_CATALYST)
    Registry.register(Registry.ITEM, Identifier(MODID, "self_governing_catalyst"),
            BlockItem(SELF_GOVERNING_CATALYST, Item.Settings().group(ItemGroup.MISC)))
    Registry.register(Registry.BLOCK_ENTITY_TYPE, Identifier(MODID, "self_governing_catalyst"),
            SELF_GOVERNING_CATALYST_BLOCK_ENTITY)

    SELF_GOVERNING_CATALYST_SCREEN_HANDLER = ScreenHandlerRegistry
            .registerExtended(Identifier(MODID, "self_governing_catalyst")) { syncId, inventory, buf ->
                SelfGoverningCatalystScreenHandler(syncId, inventory, buf.readBlockPos(), ClickMode.values()[buf.readInt()], AimDirection.values()[buf.readInt()], RedstoneMode.values()[buf.readInt()])
            }

    ServerSidePacketRegistry.INSTANCE.register(Identifier(MODID, "mode_button_click_packet")) { packetContext, packetByteBuf ->
        val blockPos = packetByteBuf.readBlockPos()
        val clickMode = ClickMode.values()[packetByteBuf.readInt()]

        packetContext.taskQueue.execute {
            if (packetContext.player.world.canSetBlock(blockPos)) {
                val blockEntity = packetContext.player.world.getBlockEntity(blockPos) as? SelfGoverningCatalystBlockEntity
                if (blockEntity != null) {
                    blockEntity.mode = clickMode
                }
            }
        }
    }

    ServerSidePacketRegistry.INSTANCE.register(Identifier(MODID, "aim_button_click_packet")) { packetContext, packetByteBuf ->
        val blockPos = packetByteBuf.readBlockPos()
        val aimDirection = AimDirection.values()[packetByteBuf.readInt()]

        packetContext.taskQueue.execute {
            if (packetContext.player.world.canSetBlock(blockPos)) {
                val blockEntity = packetContext.player.world.getBlockEntity(blockPos) as? SelfGoverningCatalystBlockEntity
                if (blockEntity != null) {
                    blockEntity.aimDirection = aimDirection
                }
            }
        }
    }

    ServerSidePacketRegistry.INSTANCE.register(Identifier(MODID, "redstone_button_click_packet")) { packetContext, packetByteBuf ->
        val blockPos = packetByteBuf.readBlockPos()
        val redstoneMode = RedstoneMode.values()[packetByteBuf.readInt()]

        packetContext.taskQueue.execute {
            if (packetContext.player.world.canSetBlock(blockPos)) {
                val blockEntity = packetContext.player.world.getBlockEntity(blockPos) as? SelfGoverningCatalystBlockEntity
                if (blockEntity != null) {
                    blockEntity.redstoneMode = redstoneMode
                }
            }
        }
    }
}

@Suppress("unused")
fun initClient() {
    ScreenRegistry.register(SELF_GOVERNING_CATALYST_SCREEN_HANDLER, ::SelfGoverningCatalystScreen)
}

