package io.github.victinix888.selfgoverningcatalyst.block

import io.github.victinix888.selfgoverningcatalyst.SELF_GOVERNING_CATALYST_BLOCK_ENTITY
import io.github.victinix888.selfgoverningcatalyst.blockentity.SelfGoverningCatalystBlockEntity
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.ItemScatterer
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

class SelfGoverningCatalystBlock(blockSettings: FabricBlockSettings) : FacingBlock(blockSettings), BlockEntityProvider {

    companion object {
        val DEFAULT_DIRECTION = Direction.NORTH

        // property for whether the block is receiving a redstone signal, has to be manually changed
        val TRIGGERED: BooleanProperty = Properties.TRIGGERED
    }

    init {
        defaultState = stateManager.defaultState.with(FACING, DEFAULT_DIRECTION).with(TRIGGERED, false)
    }

    private lateinit var direction: Direction

    override fun createBlockEntity(pos: BlockPos?, state: BlockState?): BlockEntity {
        return SelfGoverningCatalystBlockEntity(pos, state)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>?) {
        builder?.add(FACING, TRIGGERED)
    }

    override fun getPlacementState(ctx: ItemPlacementContext?): BlockState? {
        direction = ctx?.playerLookDirection?.opposite ?: DEFAULT_DIRECTION
        return defaultState.with(FACING, direction)
    }

    override fun createScreenHandlerFactory(state: BlockState?, world: World?, pos: BlockPos?): ExtendedScreenHandlerFactory? {
        return world?.getBlockEntity(pos) as? ExtendedScreenHandlerFactory
    }

    @Suppress("DEPRECATION")
    override fun onUse(state: BlockState?, world: World?, pos: BlockPos?, player: PlayerEntity?, hand: Hand?, hit: BlockHitResult?): ActionResult {
        return if (world != null && player != null) {
            if (!world.isClient) {
                if (world.getBlockEntity(pos) is SelfGoverningCatalystBlockEntity) {
                    val factory = createScreenHandlerFactory(state, world, pos)
                    player.openHandledScreen(factory)
                }
            }
            ActionResult.SUCCESS
        } else {
            ActionResult.CONSUME
        }
    }

    override fun neighborUpdate(state: BlockState?, world: World?, pos: BlockPos?, block: Block?, fromPos: BlockPos?, notify: Boolean) {
        if (world != null && state != null) {
            val isReceivingRedstoneSignal = world.isReceivingRedstonePower(pos) || world.isReceivingRedstonePower(pos?.up())
            val wasTriggered = state.get(TRIGGERED)
            if (isReceivingRedstoneSignal && !wasTriggered) {
                world.setBlockState(pos, state.with(TRIGGERED, true), 4)
            } else if (!isReceivingRedstoneSignal && wasTriggered) {
                world.setBlockState(pos, state.with(TRIGGERED, false), 4)
            }
        }
    }
    
    @Suppress("DEPRECATION")
    override fun onStateReplaced(state: BlockState?, world: World?, pos: BlockPos?, newState: BlockState?, moved: Boolean) {
        if (state?.block != newState?.block) {
            val blockEntity = world?.getBlockEntity(pos)
            if (blockEntity is SelfGoverningCatalystBlockEntity) {
                ItemScatterer.spawn(world, pos, blockEntity)
            }

            super.onStateReplaced(state, world, pos, newState, moved)

        }
    }
    
    override fun <T : BlockEntity?> getTicker(
        world: World?,
        state: BlockState?,
        type: BlockEntityType<T>?
    ): BlockEntityTicker<T>? {
        if (type == SELF_GOVERNING_CATALYST_BLOCK_ENTITY) {
            if (world?.isClient == false) {
                return BlockEntityTicker<T> { tickWorld, _, tickingBlockWorld, tickingBlockEntity ->
                    tickingBlockEntity as SelfGoverningCatalystBlockEntity
                    SelfGoverningCatalystBlockEntity.serverTick(tickWorld, tickingBlockWorld, tickingBlockEntity)
                }
            }
        }
        
        return null
    }
}